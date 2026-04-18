package k4k.travelcorequesting.client.screens;

import k4k.travelcorequesting.TravelcoreQuesting;
import k4k.travelcorequesting.client.interfaces.ClientQuestBookManagerContainer;
import k4k.travelcorequesting.domain.enums.CompletionStatus;
import k4k.travelcorequesting.infra.networking.QuestBookTaskPinC2SPacket;
import k4k.travelcorequesting.questing.models.QuestBookQuestListItem;
import k4k.travelcorequesting.questing.models.QuestBookQuest;
import k4k.travelcorequesting.questing.models.QuestBookTask;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

/**
 * Экран квестовой книги.
 *
 * <p>Левая половина — прокручиваемый список квестов (активные + завершённые).
 * Правая половина — детали выбранного квеста: заголовок, описание и задачи текущего этапа.
 */
public class QuestBookQuestsScreen extends Screen {

    private static final Identifier BACKGROUND_TEXTURE =
            new Identifier(TravelcoreQuesting.MOD_ID, "textures/gui/quest_book.png");
    private static final Identifier ICONS_TEXTURE = Identifier.of(TravelcoreQuesting.MOD_ID, "textures/icons/default.png");

    // Размеры книги
    private static final int BOOK_W = 256;
    private static final int BOOK_H = 180;

    // Левая панель (в координатах книги)
    private static final int LEFT_X = 16;
    private static final int LEFT_Y = 24;
    private static final int LEFT_W = 104;
    private static final int LEFT_H = 136;

    // Правая панель (в координатах книги)
    private static final int RIGHT_X = 136;
    private static final int RIGHT_Y = 24;
    private static final int RIGHT_W = 104;
    private static final int RIGHT_H = 136;

    // Положение заголовка книги (в координатах книги)
    private static final int TITLE_Y = 12;

    // Элементы списка квестов
    private static final int ITEM_ICON = 8;
    private static final int ITEM_ICON_GAP = 2;
    private static final int ITEM_GAP = 4;

    // Внутренний отступ секции заголовка (заголовок+подложка 6px → 2px → разделитель 2px → 4px)
    private static final int SECTION_HEADER_FULL_HEIGHT = 14;

    // Позиции правой панели (относительно RIGHT_Y)
    private static final int RIGHT_DIV2_REL = 22;   // второй разделитель (книга y=46)
    private static final int RIGHT_TITLE_REL = 9;   // заголовок квеста (книга y=33)
    private static final int RIGHT_DESC_REL = 28;   // описание (книга y=52)
    private static final int RIGHT_TASKS_GAP = 6;   // отступ между концом описания и списком задач

    // Прогресс-бар
    private static final int BAR_WIDTH = 32;

    // Цвета из текстуры (загружаются в init)
    private int color1 = 0xFF333333;
    private int color2 = 0xFF4A4A4A;
    private int color3 = 0xFF888888;

    // Положение книги на экране (вычисляется в render)
    private int bookX;
    private int bookY;

    // Состояние выбора
    private @Nullable Identifier selectedQuestId = null;
    private @Nullable QuestBookQuest detailData = null;
    private @Nullable CompletableFuture<QuestBookQuest> detailFuture = null;

    // Скролл
    private int leftScroll = 0;
    private int rightScroll = 0;
    private int leftContentHeight = 0;
    private int rightContentHeight = 0;

    // Hover: вычисляется каждый кадр в render
    private @Nullable Identifier hoveredQuestId = null;
    private int hoveredTaskIndex = -1;
    private int mouseX;
    private int mouseY;

    public QuestBookQuestsScreen() {
        super(Text.translatable("gui.quest_book"));
    }

    @Override
    protected void init() {
        super.init();
        loadColors();
    }

    /**
     * Читает три цвета из текстуры книги.
     * Цвет #1 — пиксель (0, 184), #2 — (8, 184), #3 — (16, 184).
     */
    private void loadColors() {
        if (client == null) return;
        var resource = client.getResourceManager().getResource(BACKGROUND_TEXTURE).orElse(null);
        if (resource == null) return;
        try (var stream = resource.getInputStream();
             var image = NativeImage.read(stream)) {
            color2 = abgrToArgb(image.getColor(0, 184));
            color1 = abgrToArgb(image.getColor(8, 184));
            color3 = abgrToArgb(image.getColor(16, 184));
        } catch (Exception ignored) {
            // Оставляем дефолтные цвета
        }
    }

    /**
     * Конвертирует ABGR (формат NativeImage) в ARGB (формат drawText).
     *
     * @param abgr цвет в формате NativeImage
     * @return цвет в формате ARGB
     */
    private static int abgrToArgb(int abgr) {
        int r = abgr & 0xFF;
        int g = (abgr >> 8) & 0xFF;
        int b = (abgr >> 16) & 0xFF;
        int a = (abgr >> 24) & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (client == null) return;

        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.bookX = (width - BOOK_W) / 2;
        this.bookY = (height - BOOK_H) / 2;

        // Подгружаем детали если future завершился
        if (detailFuture != null && detailFuture.isDone()) {
            detailData = detailFuture.join();
            detailFuture = null;
        }

        var questManager = ClientQuestBookManagerContainer.getQuestManager(client);

        // Сброс выбора если квест исчез из списка (reload датапаков, drop)
        if (selectedQuestId != null && !questManager.hasQuest(selectedQuestId)) {
            selectedQuestId = null;
            detailData = null;
            detailFuture = null;
        }

        // Автообновление деталей если кэш был инвалидирован пока книга открыта
        if (selectedQuestId != null && detailFuture == null && detailData != null
                && !questManager.isDetailCacheFresh(selectedQuestId)) {
            detailData = null;
            detailFuture = questManager.fetchQuestDetails(selectedQuestId);
        }

        renderBackground(context);
        context.drawTexture(BACKGROUND_TEXTURE, bookX, bookY, 0, 0, BOOK_W, BOOK_H);
        super.render(context, mouseX, mouseY, delta);

        hoveredQuestId = null;
        hoveredTaskIndex = -1;

        drawTitle(context);
        drawLeftPanel(context);
        drawRightPanel(context);
    }

    /** Рисует заголовок "QUESTS" по центру с декорациями по бокам. */
    private void drawTitle(DrawContext context) {
        var title = Text.literal("").append(Text.translatable("gui.quest_book.header")).formatted(Formatting.BOLD);
        int tw = textRenderer.getWidth(title);
        int tx = bookX + BOOK_W / 2 - tw / 2;
        int ty = bookY + TITLE_Y;
        int decorY = ty - (6 - textRenderer.fontHeight) / 2;

        // Левая декорация (0, 192, 16×6)
        context.drawTexture(BACKGROUND_TEXTURE, tx - 2 - 16, decorY, 0, 192, 16, 6);
        // Правая декорация (16, 192, 16×6)
        context.drawTexture(BACKGROUND_TEXTURE, tx + tw + 2, decorY, 16, 192, 16, 6);

        context.drawText(textRenderer, title, tx, ty, color1, false);
    }

    // -------------------------------------------------------------------------
    // Левая панель
    // -------------------------------------------------------------------------

    /** Рисует левую панель: список активных и завершённых квестов. */
    private void drawLeftPanel(DrawContext context) {
        int px = bookX + LEFT_X;
        int py = bookY + LEFT_Y;

        context.enableScissor(px, py, px + LEFT_W, py + LEFT_H);

        var quests = ClientQuestBookManagerContainer.getQuestManager(client).getPlayerQuests();

        if (quests.isEmpty()) {
            var text = Text.translatable("gui.quest_book.no_quests");
            int tw = textRenderer.getWidth(text);
            context.drawText(textRenderer, text, px + (LEFT_W - tw) / 2, py + LEFT_H / 2 - 4, color3, false);
            context.disableScissor();
            leftContentHeight = 0;
            return;
        }

        var active = quests.stream().filter(q -> q.completionStatus() == null).toList();
        var complete = quests.stream().filter(q -> q.completionStatus() != null).toList();

        int y = py - leftScroll;
        int startY = y;

        // Секция "ACTIVE"
        var activeText = Text.literal("").append(Text.translatable("gui.quest_book.active")).formatted(Formatting.BOLD);
        drawSectionHeader(context, px, y, activeText, color2);
        y += SECTION_HEADER_FULL_HEIGHT;
        for (var q : active) {
            y = drawQuestItem(context, px, py, y, q);
        }

        // Разделитель секций (центрированный, 4px после последнего квеста)
        int dividerY = y + 4;
        context.drawTexture(BACKGROUND_TEXTURE, px + (LEFT_W - 32) / 2, dividerY, 32, 192, 32, 2);

        // Секция "COMPLETE"
        y = dividerY + 2 + 4;
        var completeText = Text.literal("").append(Text.translatable("gui.quest_book.complete")).formatted(Formatting.BOLD);
        drawSectionHeader(context, px, y, completeText, color1);
        y += SECTION_HEADER_FULL_HEIGHT;
        for (var q : complete) {
            y = drawQuestItem(context, px, py, y, q);
        }

        leftContentHeight = y - startY;
        context.disableScissor();
    }

    /**
     * Рисует заголовок секции с фоновой подложкой и разделителем.
     * Высота занимаемой области — {@value SECTION_HEADER_FULL_HEIGHT} пикселей.
     *
     * @param px    левый край панели (экранные координаты)
     * @param y     верхний край заголовка (экранные координаты)
     * @param label текст заголовка
     * @param color цвет текста
     */
    private void drawSectionHeader(DrawContext context, int px, int y, Text label, int color) {
        int tw = textRenderer.getWidth(label);
        int bgU = tw + 4;
        int bgW = LEFT_W - bgU;

        // Фоновая подложка справа от заголовка
        if (bgW > 0) {
            context.drawTexture(BACKGROUND_TEXTURE, px + bgU, y, bgU, 200, bgW, 6);
        }

        context.drawText(textRenderer, label, px, y, color, false);

        // Разделитель под заголовком (2px ниже, через 2px)
        context.drawTexture(BACKGROUND_TEXTURE, px, y + 8, 0, 208, LEFT_W, 2);
    }

    /**
     * Рисует один элемент списка квестов (иконка + заголовок).
     * Учитывает hover и выделение; анимирует заголовок, если он не помещается.
     *
     * @param px    левый край панели
     * @param py    верхняя граница видимой части панели (для проверки hover)
     * @param y     текущая Y-позиция элемента (экранные координаты со скроллом)
     * @param quest данные квеста
     * @return Y-позиция следующего элемента
     */
    private int drawQuestItem(DrawContext context, int px, int py, int y, QuestBookQuestListItem quest) {
        int itemH = Math.max(ITEM_ICON, textRenderer.fontHeight);
        boolean visible = y + itemH > py && y < py + LEFT_H;
        boolean hovered = visible
                && mouseX >= px && mouseX < px + LEFT_W
                && mouseY >= Math.max(y, py) && mouseY < Math.min(y + itemH, py + LEFT_H);
        boolean selected = quest.questId().equals(selectedQuestId);

        if (hovered) hoveredQuestId = quest.questId();

        if (hovered || selected) {
            context.drawBorder(px - 1, y - 1, LEFT_W + 2, itemH + 2, color3);
        } else if (quest.isPinned()) {
            context.drawBorder(px - 1, y - 1, LEFT_W + 2, itemH + 2, color2);
        }

        // Иконка квеста из атласа квеста; u зависит от статуса, v=24 (required row + book offset)
        if (quest.icon() != null) {
            var iconTex = quest.icon().withPath(path -> "textures/icons/" + path + ".png");
            context.drawTexture(iconTex, px, y, iconU(quest.completionStatus()), 24, ITEM_ICON, ITEM_ICON);
        }

        // Заголовок квеста (жирный, чёрный по умолчанию; сохраняет стиль текста)
        int textX = px + ITEM_ICON + ITEM_ICON_GAP;
        int textMaxW = LEFT_W - ITEM_ICON - ITEM_ICON_GAP;
        var boldTitle = Text.literal("").styled(s -> s.withBold(true)).append(quest.title());

        int titleW = textRenderer.getWidth(boldTitle);
        if (titleW > textMaxW) {
            // Анимированный скролл по синусоиде (период 4с)
            long t = System.currentTimeMillis() % 4000;
            float phase = (float) (Math.sin(t / 4000.0 * Math.PI * 2 - Math.PI / 2) + 1) / 2f;
            int offset = (int) (phase * (titleW - textMaxW));
            context.enableScissor(textX, y, textX + textMaxW, y + itemH + 1);
            context.drawText(textRenderer, boldTitle, textX - offset, y, 0x2B2B2B, false);
            context.disableScissor();
        } else {
            context.drawText(textRenderer, boldTitle, textX, y, 0x2B2B2B, false);
        }

        return y + itemH + ITEM_GAP;
    }

    // -------------------------------------------------------------------------
    // Правая панель
    // -------------------------------------------------------------------------

    /** Рисует правую панель: детали выбранного квеста или подсказку. */
    private void drawRightPanel(DrawContext context) {
        int px = bookX + RIGHT_X;
        int py = bookY + RIGHT_Y;

        context.enableScissor(px, py, px + RIGHT_W, py + RIGHT_H);

        if (selectedQuestId == null) {
            var text = Text.translatable("gui.quest_book.no_selection");
            int tw = textRenderer.getWidth(text);
            context.drawText(textRenderer, text, px + (RIGHT_W - tw) / 2, py + RIGHT_H / 2 - 4, color3, false);
            context.disableScissor();
            return;
        }

        int contentBase = py - rightScroll;

        // Разделители заголовка (y=24 и y=46 в книге → rel 0 и rel 22)
        context.drawTexture(BACKGROUND_TEXTURE, px, contentBase, 0, 208, RIGHT_W, 2);
        context.drawTexture(BACKGROUND_TEXTURE, px, contentBase + RIGHT_DIV2_REL, 0, 208, RIGHT_W, 2);

        // Заголовок квеста (жирный, чёрный, по центру панели, y=33 в книге → rel 9)
        if (detailData != null) {
            var boldTitle = Text.literal("").styled(s -> s.withBold(true)).append(detailData.title());
            int titleW = textRenderer.getWidth(boldTitle);
            int titleY = contentBase + RIGHT_TITLE_REL;

            if (titleW > RIGHT_W) {
                long t = System.currentTimeMillis() % 4000;
                float phase = (float) (Math.sin(t / 4000.0 * Math.PI * 2 - Math.PI / 2) + 1) / 2f;
                int offset = (int) (phase * (titleW - RIGHT_W));
                context.enableScissor(px, titleY, px + RIGHT_W, titleY + textRenderer.fontHeight + 1);
                context.drawText(textRenderer, boldTitle, px - offset, titleY, 0x2B2B2B, false);
                context.disableScissor();
            } else {
                context.drawText(textRenderer, boldTitle, px + (RIGHT_W - titleW) / 2, titleY, 0x2B2B2B, false);
            }

            // Описание квеста (многострочное, чёрный); позиция задач считается от его конца
            int taskY = contentBase + RIGHT_DESC_REL;
            if (detailData.description() != null) {
                for (var line : textRenderer.wrapLines(detailData.description(), RIGHT_W)) {
                    context.drawText(textRenderer, line, px, taskY, 0x2B2B2B, false);
                    taskY += textRenderer.fontHeight;
                }
            }
            taskY += RIGHT_TASKS_GAP;

            var questIcon = ClientQuestBookManagerContainer.getQuestManager(client).getPlayerQuests().stream()
                    .filter(q -> q.questId().equals(selectedQuestId))
                    .map(QuestBookQuestListItem::icon)
                    .findFirst().orElse(null);

            var tasks = detailData.tasks();
            var pinnedTaskId = detailData.pinnedTaskId();
            for (int i = 0; i < tasks.size(); i++) {
                taskY = drawTaskItem(context, px, py, taskY, tasks.get(i), i, questIcon, pinnedTaskId);
            }

            rightContentHeight = taskY - (py - rightScroll);
        } else {
            // Детали ещё загружаются
            var ellipsis = String.join("", IntStream.range(0, 3).mapToObj(i -> System.currentTimeMillis() / 300 % 3 == i ? " " : ".").toList());
            var loading = Text.literal(ellipsis);
            context.drawText(textRenderer, loading, px + (RIGHT_W - textRenderer.getWidth(loading)) / 2,
                    contentBase + RIGHT_TITLE_REL, color3, false);
        }

        context.disableScissor();
    }

    /**
     * Рисует один элемент задачи в правой панели.
     * Включает иконку, заголовок (многострочный), описание и прогресс-бар для постепенных условий.
     * Закреплённая задача выделяется рамкой цвета {@code color2}.
     *
     * @param px           левый край правой панели
     * @param py           верхняя граница видимой части панели (для hover)
     * @param y            текущая Y-позиция задачи
     * @param task         данные задачи
     * @param index        индекс в списке (0 = required)
     * @param questIcon    текстура атласа иконок квеста
     * @param pinnedTaskId идентификатор закреплённой задачи; {@code null} — ни одна не закреплена
     * @return Y-позиция следующего элемента
     */
    private int drawTaskItem(DrawContext context, int px, int py, int y, QuestBookTask task, int index, @Nullable Identifier questIcon, @Nullable String pinnedTaskId) {
        boolean isRequired = index == 0;
        // v иконки: HUD использует 0 (optional) или 8 (required); в книге +16
        int iconV = (isRequired ? ITEM_ICON : 0) + 16;
        int iconU = iconU(task.completionStatus());
        var iconTex = questIcon != null ? questIcon.withPath(path -> "textures/icons/" + path + ".png") : ICONS_TEXTURE;

        int textX = px + ITEM_ICON + ITEM_ICON_GAP;
        int textW = RIGHT_W - ITEM_ICON - ITEM_ICON_GAP;

        // Вычисляем высоту элемента
        int titleLines = textRenderer.wrapLines(task.title(), textW).size();
        int titleH = titleLines * textRenderer.fontHeight;
        int descH = task.description() != null
                ? textRenderer.wrapLines(task.description(), textW).size() * textRenderer.fontHeight
                : 0;
        int barH = (task.isGradual() && !task.isComplete()) ? 3 : 0; // 1px бар + 2px отступ
        int itemH = Math.max(ITEM_ICON, titleH) + (descH > 0 ? descH + 1 : 0) + barH;

        boolean visible = y + itemH > py && y < py + RIGHT_H;
        boolean hovered = visible
                && mouseX >= px && mouseX < px + RIGHT_W
                && mouseY >= Math.max(y, py) && mouseY < Math.min(y + itemH, py + RIGHT_H);

        if (hovered) hoveredTaskIndex = index;

        boolean isPinned = task.taskId().equals(pinnedTaskId);
        if (hovered) {
            context.drawBorder(px - 1, y - 1, RIGHT_W + 2, itemH + 2, color3);
        } else if (isPinned) {
            context.drawBorder(px - 1, y - 1, RIGHT_W + 2, itemH + 2, color2);
        }

        // Иконка задачи из атласа квеста; u зависит от статуса, v со смещением +16 относительно HUD
        context.drawTexture(iconTex, px, y, iconU, iconV, ITEM_ICON, ITEM_ICON);

        // Заголовок задачи (серый, многострочный)
        int currentY = y;
        for (var line : textRenderer.wrapLines(task.title(), textW)) {
            context.drawText(textRenderer, line, textX, currentY, 0x2B2B2B, false);
            currentY += textRenderer.fontHeight;
        }

        // Описание задачи
        if (task.description() != null) {
            currentY += 1;
            for (var line : textRenderer.wrapLines(task.description(), textW)) {
                context.drawText(textRenderer, line, textX, currentY, 0x2B2B2B, false);
                currentY += textRenderer.fontHeight;
            }
        }

        // Прогресс-бар для постепенных условий
        if (task.isGradual() && !task.isComplete()) {
            int fillW = (int) (task.completionLevel() * BAR_WIDTH);
            context.drawTexture(ICONS_TEXTURE, textX, currentY + 1, 0, 32, BAR_WIDTH, 1);
            if (fillW > 0) {
                context.drawTexture(ICONS_TEXTURE, textX, currentY + 1, 0, 33, fillW, 1);
            }
        }

        return y + itemH + ITEM_GAP;
    }

    // -------------------------------------------------------------------------
    // Ввод
    // -------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        // Клик по квесту в левой панели
        if (hoveredQuestId != null) {
            selectQuest(hoveredQuestId);
            playClickSound();
            return true;
        }

        // Клик по задаче в правой панели
        if (hoveredTaskIndex >= 0 && detailData != null && selectedQuestId != null) {
            var tasks = detailData.tasks();
            if (hoveredTaskIndex < tasks.size()) {
                var task = tasks.get(hoveredTaskIndex);
                ClientPlayNetworking.send(new QuestBookTaskPinC2SPacket(selectedQuestId, task.taskId()));
                playClickSound();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int panelX = bookX + LEFT_X;
        int panelY = bookY + LEFT_Y;

        if (mouseX >= panelX && mouseX < panelX + LEFT_W
                && mouseY >= panelY && mouseY < panelY + LEFT_H) {
            int maxScroll = Math.max(0, leftContentHeight - LEFT_H);
            leftScroll = (int) Math.max(0, Math.min(leftScroll - amount * 10, maxScroll));
            return true;
        }

        int rightPanelX = bookX + RIGHT_X;
        int rightPanelY = bookY + RIGHT_Y;
        if (mouseX >= rightPanelX && mouseX < rightPanelX + RIGHT_W
                && mouseY >= rightPanelY && mouseY < rightPanelY + RIGHT_H) {
            int maxScroll = Math.max(0, rightContentHeight - RIGHT_H);
            rightScroll = (int) Math.max(0, Math.min(rightScroll - amount * 10, maxScroll));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    /**
     * Выбирает квест и запрашивает его детали с сервера.
     *
     * @param questId идентификатор квеста
     */
    private void selectQuest(Identifier questId) {
        selectedQuestId = questId;
        detailData = null;
        rightScroll = 0;
        detailFuture = ClientQuestBookManagerContainer.getQuestManager(client).fetchQuestDetails(questId);
    }

    /**
     * Возвращает U-координату иконки в текстуре по статусу завершения.
     * Совпадает с иконками HUD (0=active, 16=success, 24=failure, 32=skipped).
     *
     * @param status статус завершения; {@code null} означает активную задачу/квест
     * @return U-координата в пикселях
     */
    private static int iconU(@Nullable CompletionStatus status) {
        if (status == null) return 0;
        return switch (status) {
            case SUCCESS -> 16;
            case FAILURE -> 24;
            case SKIPPED -> 32;
        };
    }

    /** Воспроизводит звук нажатия кнопки. */
    private void playClickSound() {
        if (client == null) return;
        client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}

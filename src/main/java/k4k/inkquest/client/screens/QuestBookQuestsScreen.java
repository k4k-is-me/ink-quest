package k4k.inkquest.client.screens;

import k4k.inkquest.TravelcoreQuesting;
import k4k.inkquest.client.interfaces.ClientQuestBookManagerContainer;
import k4k.inkquest.domain.enums.CompletionStatus;
import k4k.inkquest.domain.enums.TaskButton;
import k4k.inkquest.infra.networking.QuestBookTaskActionC2SPacket;
import k4k.inkquest.infra.networking.QuestBookTaskPinC2SPacket;
import k4k.inkquest.questing.models.QuestBookQuestListItem;
import k4k.inkquest.questing.models.QuestBookQuest;
import k4k.inkquest.questing.models.QuestBookTask;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import org.lwjgl.glfw.GLFW;

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
    private static final int BAR_BG_V = 40;
    private static final int BAR_FILL_V = 41;

    // Кнопки ручного завершения задачи
    private static final int BUTTON_SIZE = 8;
    private static final int BUTTON_GAP = 1;
    private static final int BUTTONS_TOP_GAP = 2;
    private static final int ICON_BUTTON_SUCCESS_U = 0;
    private static final int ICON_BUTTON_FAILURE_U = 8;
    private static final int ICON_BUTTON_SKIP_U = 16;
    private static final int ICON_BUTTON_V = 216;
    private static final int ICON_BUTTON_HOVER_V_OFFSET = 8;

    // Задачи в правой панели
    private static final int TASKS_SIDE_PADDING = 2;
    private static final int OPTIONAL_TASK_EXTRA_PADDING = 6;
    private static final int TASKS_GAP = 2;

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

    // Позиции кнопок задач: taskIndex → список слотов. Пересчитываются каждый кадр в drawRightPanel.
    private final Map<Integer, List<ButtonSlot>> taskButtonSlots = new HashMap<>();

    /** Позиция одной кнопки задачи на экране. */
    private record ButtonSlot(TaskButton button, int x, int y) {}

    // Клавиатурный фокус
    private enum ActivePanel { LEFT, RIGHT }
    private ActivePanel activePanel = ActivePanel.LEFT;
    private @Nullable Identifier focusedQuestId = null;
    private int focusedTaskIndex = -1;

    /** Идентификатор квеста для выбора при первом открытии экрана. */
    private @Nullable Identifier preselectQuestId;

    public QuestBookQuestsScreen() {
        this(null);
    }

    /**
     * @param preselectQuestId квест, который будет выбран при первом открытии;
     *                         {@code null} — без предварительного выбора
     */
    public QuestBookQuestsScreen(@Nullable Identifier preselectQuestId) {
        super(Text.translatable("gui.quest_book"));
        this.preselectQuestId = preselectQuestId;
    }

    @Override
    protected void init() {
        super.init();
        loadColors();
        if (preselectQuestId != null) {
            selectQuest(preselectQuestId);
            preselectQuestId = null;
        }
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
        if (focusedQuestId != null && !questManager.hasQuest(focusedQuestId)) {
            focusedQuestId = null;
        }

        // Автообновление деталей если кэш был инвалидирован пока книга открыта.
        // detailData не сбрасываем — показываем старые данные до получения новых, чтобы не мелькал экран.
        if (selectedQuestId != null && detailFuture == null && detailData != null
                && !questManager.isDetailCacheFresh(selectedQuestId)) {
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
        drawButtonTooltip(context, mouseX, mouseY);
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

        var pinned = quests.stream().filter(q -> q.completionStatus() == null && q.isPinned()).toList();
        var active = quests.stream().filter(q -> q.completionStatus() == null && !q.isPinned()).toList();
        var complete = quests.stream().filter(q -> q.completionStatus() != null).toList();

        int y = py - leftScroll;
        int startY = y;

        // Разделитель между секциями рисуется перед секцией, если уже показана хотя бы одна,
        // чтобы он никогда не висел «в воздухе» после пустой или последней секции.
        boolean anySectionShown = false;

        // Секция закреплённых (иконка вместо текста)
        if (!pinned.isEmpty()) {
            drawIconSectionHeader(context, px, y);
            y += SECTION_HEADER_FULL_HEIGHT;
            for (var q : pinned) {
                y = drawQuestItem(context, px, py, y, q);
            }
            anySectionShown = true;
        }

        // Секция "ACTIVE"
        if (!active.isEmpty()) {
            if (anySectionShown) {
                context.drawTexture(BACKGROUND_TEXTURE, px + (LEFT_W - 32) / 2, y, 32, 192, 32, 2);
                y += 2 + 4;
            }
            var activeText = Text.literal("").append(Text.translatable("gui.quest_book.active")).formatted(Formatting.BOLD);
            drawSectionHeader(context, px, y, activeText, color2);
            y += SECTION_HEADER_FULL_HEIGHT;
            for (var q : active) {
                y = drawQuestItem(context, px, py, y, q);
            }
            anySectionShown = true;
        }

        // Секция "COMPLETE"
        if (!complete.isEmpty()) {
            if (anySectionShown) {
                context.drawTexture(BACKGROUND_TEXTURE, px + (LEFT_W - 32) / 2, y, 32, 192, 32, 2);
                y += 2 + 4;
            }
            var completeText = Text.literal("").append(Text.translatable("gui.quest_book.complete")).formatted(Formatting.BOLD);
            drawSectionHeader(context, px, y, completeText, color1);
            y += SECTION_HEADER_FULL_HEIGHT;
            for (var q : complete) {
                y = drawQuestItem(context, px, py, y, q);
            }
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
     * Рисует заголовок секции с иконкой из текстуры книги вместо текста.
     * Высота занимаемой области — {@value SECTION_HEADER_FULL_HEIGHT} пикселей.
     *
     * @param px левый край панели
     * @param y  верхний край заголовка
     */
    private void drawIconSectionHeader(DrawContext context, int px, int y) {
        int bgStart = ITEM_ICON + ITEM_ICON_GAP;
        int bgW = LEFT_W - bgStart;
        context.drawTexture(BACKGROUND_TEXTURE, px + bgStart, y, bgStart, 200, bgW, 6);
        context.drawTexture(BACKGROUND_TEXTURE, px, y, 64, 192, ITEM_ICON, ITEM_ICON);
        context.drawTexture(BACKGROUND_TEXTURE, px, y + ITEM_ICON + 2, 0, 208, LEFT_W, 2);
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
        boolean focused = activePanel == ActivePanel.LEFT && quest.questId().equals(focusedQuestId);

        if (focused || hovered || selected) {
            context.drawBorder(px, y - 2, LEFT_W, itemH + 2, color3);
        }

        // Иконка квеста из атласа квеста; u зависит от статуса, v=24 (required row + book offset)
        var iconTex = quest.icon().withPath(path -> "textures/icons/" + path + ".png");
        context.drawTexture(iconTex, px + 2, y, iconU(quest.completionStatus()), 24, ITEM_ICON, ITEM_ICON);

        if (quest.completionStatus() == null && !quest.viewed()) {
            RenderSystem.enableBlend();
            context.drawTexture(BACKGROUND_TEXTURE, px, y - 2, 72, 192, 4, 4);
            RenderSystem.disableBlend();
        }

        // Заголовок квеста (жирный, чёрный по умолчанию; сохраняет стиль текста)
        int textX = px + 2 + ITEM_ICON + ITEM_ICON_GAP;
        int textMaxW = LEFT_W - ITEM_ICON - ITEM_ICON_GAP - 4;
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

        taskButtonSlots.clear();
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
            int taskAreaX = px + TASKS_SIDE_PADDING;
            int taskAreaW = RIGHT_W - TASKS_SIDE_PADDING * 2;

            boolean isPinnedOptional = pinnedTaskId != null
                    && !tasks.isEmpty()
                    && !tasks.get(0).taskId().equals(pinnedTaskId)
                    && tasks.stream().anyMatch(t -> t.taskId().equals(pinnedTaskId));

            if (isPinnedOptional) {
                for (int i = 0; i < tasks.size(); i++) {
                    if (tasks.get(i).taskId().equals(pinnedTaskId)) {
                        taskY = drawTaskItem(context, taskAreaX, taskAreaW, py, taskY, tasks.get(i), i, false, false, questIcon);
                        break;
                    }
                }
                taskY += TASKS_GAP;
                for (int i = 0; i < tasks.size(); i++) {
                    if (tasks.get(i).taskId().equals(pinnedTaskId)) continue;
                    taskY = drawTaskItem(context, taskAreaX, taskAreaW, py, taskY, tasks.get(i), i, i == 0, i != 0, questIcon);
                }
            } else {
                for (int i = 0; i < tasks.size(); i++) {
                    taskY = drawTaskItem(context, taskAreaX, taskAreaW, py, taskY, tasks.get(i), i, i == 0, i != 0, questIcon);
                }
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
     * Необязательные задачи рисуются с дополнительным отступом слева ({@value OPTIONAL_TASK_EXTRA_PADDING}px).
     * Рамка отображается только при hover или keyboard-фокусе.
     *
     * @param taskAreaX    левый край блока задач (уже учитывает TASKS_SIDE_PADDING)
     * @param taskAreaW    ширина блока задач
     * @param py           верхняя граница видимой части панели (для hover)
     * @param y            текущая Y-позиция задачи
     * @param task         данные задачи
     * @param originalIndex индекс в detailData.tasks() (используется для hover/focus)
     * @param isRequired   {@code true} если задача обязательная — влияет только на иконку
     * @param withIndent   {@code true} если нужен доп. отступ слева (опциональные задачи, кроме вынесенной наверх pinned)
     * @param questIcon    текстура атласа иконок квеста; {@code null} — дефолтная
     * @return Y-позиция следующего элемента
     */
    private int drawTaskItem(DrawContext context, int taskAreaX, int taskAreaW, int py, int y,
                             QuestBookTask task, int originalIndex, boolean isRequired, boolean withIndent,
                             @Nullable Identifier questIcon) {
        int indent = withIndent ? OPTIONAL_TASK_EXTRA_PADDING : 0;
        int drawX = taskAreaX + indent;
        int textX = drawX + ITEM_ICON + ITEM_ICON_GAP;
        int textW = taskAreaW - ITEM_ICON - ITEM_ICON_GAP - indent;

        // v иконки: HUD использует 0 (optional) или 8 (required); в книге +16
        int iconV = (isRequired ? ITEM_ICON : 0) + 16;
        int iconU = iconU(task.completionStatus());
        var iconTex = questIcon != null ? questIcon.withPath(path -> "textures/icons/" + path + ".png") : ICONS_TEXTURE;

        // Вычисляем высоту элемента
        int titleLines = textRenderer.wrapLines(task.title(), textW).size();
        int titleH = titleLines * textRenderer.fontHeight;
        int descH = task.description() != null
                ? textRenderer.wrapLines(task.description(), textW).size() * textRenderer.fontHeight
                : 0;
        int barH = (task.hasProgressBar() && !task.isComplete()) ? 3 : 0; // 1px бар + 1px тень + 1px отступ
        int buttonsH = (task.buttons().isEmpty() || task.isComplete()) ? 0 : (BUTTONS_TOP_GAP + BUTTON_SIZE);
        int itemH = Math.max(ITEM_ICON, titleH) + (descH > 0 ? descH + 1 : 0) + barH + buttonsH;

        // Рамка рисуется от drawX - 1, что >= scissors + 1 (не обрезается)
        int borderX = drawX - 2;
        int borderW = ITEM_ICON + ITEM_ICON_GAP + textW + 2;
        boolean visible = y + itemH > py && y < py + RIGHT_H;
        boolean hovered = visible
                && mouseX >= borderX && mouseX < borderX + borderW
                && mouseY >= Math.max(y, py) && mouseY < Math.min(y + itemH, py + RIGHT_H);

        if (hovered) hoveredTaskIndex = originalIndex;
        boolean focused = activePanel == ActivePanel.RIGHT && originalIndex == focusedTaskIndex;

        if (focused || hovered) {
            context.drawBorder(borderX, y - 2, borderW, itemH + 2, color3);
        }

        // Иконка задачи из атласа квеста; u зависит от статуса, v со смещением +16 относительно HUD
        context.drawTexture(iconTex, drawX, y, iconU, iconV, ITEM_ICON, ITEM_ICON);

        // Заголовок задачи (многострочный)
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
        if (task.hasProgressBar() && !task.isComplete()) {
            int fillW = (int) (task.completionLevel() * BAR_WIDTH);
            context.drawTexture(ICONS_TEXTURE, textX, currentY + 1, 0, BAR_BG_V, BAR_WIDTH, 1);
            if (fillW > 0) {
                context.drawTexture(ICONS_TEXTURE, textX, currentY + 1, 0, BAR_FILL_V, fillW, 1);
            }
        }

        // Кнопки ручного завершения задачи
        if (!task.buttons().isEmpty() && !task.isComplete()) {
            int btnY = y + itemH - BUTTON_SIZE;
            int btnX = textX;
            var slots = new ArrayList<ButtonSlot>();
            for (var btn : List.of(TaskButton.SUCCESS, TaskButton.FAILURE, TaskButton.SKIP)) {
                if (!task.buttons().contains(btn)) continue;
                int btnU = switch (btn) {
                    case SUCCESS -> ICON_BUTTON_SUCCESS_U;
                    case FAILURE -> ICON_BUTTON_FAILURE_U;
                    case SKIP    -> ICON_BUTTON_SKIP_U;
                };
                boolean btnHovered = mouseX >= btnX && mouseX < btnX + BUTTON_SIZE
                        && mouseY >= btnY && mouseY < btnY + BUTTON_SIZE;
                int btnV = ICON_BUTTON_V + (btnHovered ? ICON_BUTTON_HOVER_V_OFFSET : 0);
                context.drawTexture(BACKGROUND_TEXTURE, btnX, btnY, btnU, btnV, BUTTON_SIZE, BUTTON_SIZE);
                slots.add(new ButtonSlot(btn, btnX, btnY));
                btnX += BUTTON_SIZE + BUTTON_GAP;
            }
            taskButtonSlots.put(originalIndex, slots);
        }

        return y + itemH + ITEM_GAP;
    }

    /**
     * Рисует tooltip для кнопки задачи, на которую наведён курсор.
     * Вызывается после всех disableScissor, чтобы tooltip не обрезался.
     */
    private void drawButtonTooltip(DrawContext context, int mouseX, int mouseY) {
        for (var slots : taskButtonSlots.values()) {
            for (var slot : slots) {
                if (mouseX >= slot.x() && mouseX < slot.x() + BUTTON_SIZE
                        && mouseY >= slot.y() && mouseY < slot.y() + BUTTON_SIZE) {
                    var key = switch (slot.button()) {
                        case SUCCESS -> "gui.inkquest.task_button.success";
                        case FAILURE -> "gui.inkquest.task_button.failure";
                        case SKIP    -> "gui.inkquest.task_button.skip";
                    };
                    context.drawTooltip(textRenderer, Text.translatable(key), mouseX, mouseY);
                    return;
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Ввод
    // -------------------------------------------------------------------------

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (activePanel == ActivePanel.LEFT) {
            if (keyCode == GLFW.GLFW_KEY_UP) { navigateLeft(-1); return true; }
            if (keyCode == GLFW.GLFW_KEY_DOWN) { navigateLeft(1); return true; }
            if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && focusedQuestId != null) {
                selectQuest(focusedQuestId);
                playClickSound();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_RIGHT && selectedQuestId != null) {
                activePanel = ActivePanel.RIGHT;
                if (focusedTaskIndex < 0) focusedTaskIndex = 0;
                return true;
            }
        } else {
            if (keyCode == GLFW.GLFW_KEY_UP) { navigateRight(-1); return true; }
            if (keyCode == GLFW.GLFW_KEY_DOWN) { navigateRight(1); return true; }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (focusedTaskIndex >= 0 && detailData != null && selectedQuestId != null) {
                    var tasks = detailData.tasks();
                    if (focusedTaskIndex < tasks.size()) {
                        ClientPlayNetworking.send(new QuestBookTaskPinC2SPacket(selectedQuestId, tasks.get(focusedTaskIndex).taskId()));
                        playClickSound();
                    }
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_LEFT) {
                activePanel = ActivePanel.LEFT;
                if (focusedQuestId == null) focusedQuestId = selectedQuestId;
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * Перемещает фокус в левой панели на {@code dir} позиций (+1 вниз, -1 вверх).
     * Если фокус ещё не установлен, выбирает первый или последний элемент.
     *
     * @param dir направление: +1 вниз, -1 вверх
     */
    private void navigateLeft(int dir) {
        if (client == null) return;
        var quests = getOrderedQuests();
        if (quests.isEmpty()) return;
        int current = -1;
        for (int i = 0; i < quests.size(); i++) {
            if (quests.get(i).questId().equals(focusedQuestId)) { current = i; break; }
        }
        int next = current < 0 ? (dir > 0 ? 0 : quests.size() - 1)
                                : Math.max(0, Math.min(quests.size() - 1, current + dir));
        focusedQuestId = quests.get(next).questId();
    }

    /**
     * Перемещает фокус в правой панели на {@code dir} позиций.
     * Если фокус ещё не установлен, выбирает первую или последнюю задачу.
     *
     * @param dir направление: +1 вниз, -1 вверх
     */
    private void navigateRight(int dir) {
        if (detailData == null) return;
        int size = detailData.tasks().size();
        if (size == 0) return;
        focusedTaskIndex = focusedTaskIndex >= 0
                ? Math.max(0, Math.min(size - 1, focusedTaskIndex + dir))
                : (dir > 0 ? 0 : size - 1);
    }

    /**
     * Возвращает квесты в порядке отображения: сначала активные, потом завершённые.
     *
     * @return упорядоченный список квестов игрока
     */
    private List<QuestBookQuestListItem> getOrderedQuests() {
        var quests = ClientQuestBookManagerContainer.getQuestManager(client).getPlayerQuests();
        var result = new ArrayList<QuestBookQuestListItem>();
        quests.stream().filter(q -> q.completionStatus() == null && q.isPinned()).forEach(result::add);
        quests.stream().filter(q -> q.completionStatus() == null && !q.isPinned()).forEach(result::add);
        quests.stream().filter(q -> q.completionStatus() != null).forEach(result::add);
        return result;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        // Клик по кнопке ручного завершения задачи (проверяется до пина задачи)
        if (selectedQuestId != null && detailData != null) {
            for (var entry : taskButtonSlots.entrySet()) {
                for (var slot : entry.getValue()) {
                    if (mouseX >= slot.x() && mouseX < slot.x() + BUTTON_SIZE
                            && mouseY >= slot.y() && mouseY < slot.y() + BUTTON_SIZE) {
                        var tasks = detailData.tasks();
                        int taskIdx = entry.getKey();
                        if (taskIdx < tasks.size()) {
                            ClientPlayNetworking.send(new QuestBookTaskActionC2SPacket(
                                    selectedQuestId, tasks.get(taskIdx).taskId(), slot.button()));
                            playClickSound();
                            var status = switch (slot.button()) {
                                case SUCCESS -> CompletionStatus.SUCCESS;
                                case FAILURE -> CompletionStatus.FAILURE;
                                case SKIP    -> CompletionStatus.SKIPPED;
                            };
                            applyOptimisticTaskCompletion(taskIdx, status);
                            return true;
                        }
                    }
                }
            }
        }

        // Клик по квесту в левой панели
        if (hoveredQuestId != null) {
            activePanel = ActivePanel.LEFT;
            focusedQuestId = hoveredQuestId;
            selectQuest(hoveredQuestId);
            playClickSound();
            return true;
        }

        // Клик по задаче в правой панели
        if (hoveredTaskIndex >= 0 && detailData != null && selectedQuestId != null) {
            var tasks = detailData.tasks();
            if (hoveredTaskIndex < tasks.size()) {
                var task = tasks.get(hoveredTaskIndex);
                activePanel = ActivePanel.RIGHT;
                focusedTaskIndex = hoveredTaskIndex;
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
     * Помечает задачу указанным статусом локально (в detailData и в клиентском кэше),
     * не дожидаясь подтверждения с сервера. Нужно для мгновенной UI-реакции
     * на нажатие кнопки. Серверные S2C-пакеты (через invalidateDetail и автоматический
     * re-fetch в render) перетрут оптимистичную правку, если состояние разойдётся.
     *
     * @param taskIndex индекс задачи в detailData.tasks()
     * @param status    статус, которым нужно пометить задачу
     */
    private void applyOptimisticTaskCompletion(int taskIndex, CompletionStatus status) {
        if (detailData == null || selectedQuestId == null || client == null) return;
        var oldTasks = detailData.tasks();
        if (taskIndex < 0 || taskIndex >= oldTasks.size()) return;
        var oldTask = oldTasks.get(taskIndex);
        if (oldTask.isComplete()) return;

        var newTask = new QuestBookTask(
                oldTask.taskId(), oldTask.title(), oldTask.description(),
                oldTask.hasProgressBar(), oldTask.completionLevel(),
                status, oldTask.buttons()
        );
        var newTasks = new ArrayList<>(oldTasks);
        newTasks.set(taskIndex, newTask);
        var newDetail = new QuestBookQuest(
                detailData.title(), detailData.description(), newTasks, detailData.pinnedTaskId()
        );
        detailData = newDetail;
        ClientQuestBookManagerContainer.getQuestManager(client).cacheQuestDetails(selectedQuestId, newDetail);
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

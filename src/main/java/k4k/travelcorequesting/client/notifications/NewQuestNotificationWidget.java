package k4k.travelcorequesting.client.notifications;

import com.mojang.blaze3d.systems.RenderSystem;
import k4k.travelcorequesting.client.TravelcoreQuestingKeybinds;
import k4k.travelcorequesting.client.animation.Animation;
import k4k.travelcorequesting.client.animation.Animator;
import k4k.travelcorequesting.client.animation.ParameterKey;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import static k4k.travelcorequesting.client.animation.ParameterAnimations.*;

/**
 * HUD-виджет уведомления о новых квестах.
 *
 * <p>Отображает текст «У тебя N новых квестов — нажми [J]» с анимацией
 * fadeIn → hold 3 с → fadeOut. После завершения цепочки {@link #isAnimatorIdle()}
 * возвращает {@code true} и виджет удаляется из {@link k4k.travelcorequesting.client.huds.QuestHudOverlay}.
 *
 * <p>Жизненный цикл целиком управляется аниматором — вся цепочка анимаций
 * ставится в очередь в {@link #playIn()}, а {@link #playOut()} прерывает
 * цепочку и немедленно запускает fade-out.
 */
public class NewQuestNotificationWidget {
    private static final ParameterKey<Float> OPACITY = new ParameterKey<>(Float.class, 0f);

    private static final int IN_DURATION_MS = 300;
    private static final int HOLD_DURATION_MS = 3000;
    private static final int OUT_DURATION_MS = 300;

    private static final Animation IN_ANIMATION = new Animation.Builder()
            .addParameter(OPACITY, fadeIn(), 0, IN_DURATION_MS)
            .build();

    /** Hold-анимация удерживает OPACITY=1 в течение HOLD_DURATION_MS. */
    private static final Animation HOLD_ANIMATION = new Animation.Builder()
            .addParameter(OPACITY, switchTo(1f), 0, HOLD_DURATION_MS)
            .build();

    private static final Animation OUT_ANIMATION = new Animation.Builder()
            .addParameter(OPACITY, fadeOut(), 0, OUT_DURATION_MS)
            .build();

    private final MinecraftClient client = MinecraftClient.getInstance();
    private final Animator animator = new Animator(Util::getMeasuringTimeMs);

    private final int count;

    /**
     * @param count   число новых квестов
     * @param questId идентификатор квеста для открытия по [J]; {@code null} если квестов больше одного
     */
    public NewQuestNotificationWidget(int count) {
        this.count = count;
    }

    /**
     * Запускает полную цепочку анимаций: IN → hold → OUT.
     * Вызывается сразу после передачи виджета в overlay.
     */
    public void playIn() {
        animator.play(IN_ANIMATION);
        animator.queue(HOLD_ANIMATION);
        animator.queue(OUT_ANIMATION);
    }

    /**
     * Прерывает текущую анимацию и немедленно запускает fade-out.
     * Используется при открытии квестовой книги.
     */
    public void playOut() {
        animator.play(OUT_ANIMATION);
    }

    /**
     * Возвращает {@code true}, когда вся цепочка завершена (после OUT).
     * Overlay удаляет виджет при следующей проверке.
     */
    public boolean isAnimatorIdle() {
        return animator.isIdle();
    }

    /**
     * Возвращает высоту виджета при заданной ширине (без вызова рендера).
     */
    public int getHeight() {
        return client.textRenderer.fontHeight * 2;
    }

    /**
     * Рендерит виджет. Возвращает занятую высоту в пикселях.
     *
     * @param context контекст отрисовки
     * @param x       левый край
     * @param y       верхний край
     */
    public void render(DrawContext context, int x, int y) {
        animator.tick();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        var opacity = animator.getParameter(OPACITY);
        RenderSystem.setShaderColor(1, 1, 1, opacity);

        int lineHeight = client.textRenderer.fontHeight;
        context.drawText(client.textRenderer, Text.literal("").append(getMainText()).formatted(Formatting.BOLD), x, y, 0xFFFFFF, true);
        context.drawText(client.textRenderer, getHintText(), x, y + lineHeight, 0xFFFFFF, true);

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }

    /** Строит основной текст уведомления (первая строка). */
    private Text getMainText() {
        if (count == 1) {
            return Text.translatable("gui.travelcorequesting.new_quest_notification.single");
        }
        return Text.translatable("gui.travelcorequesting.new_quest_notification.multiple", count);
    }

    /** Строит подсказку клавиши (вторая строка). */
    private Text getHintText() {
        return Text.translatable(
                "gui.travelcorequesting.new_quest_notification.hint",
                Texts.bracketed(TravelcoreQuestingKeybinds.OPEN_QUESTS.getBoundKeyLocalizedText())
        );
    }
}

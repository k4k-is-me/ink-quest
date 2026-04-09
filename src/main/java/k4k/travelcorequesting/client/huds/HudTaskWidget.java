package k4k.travelcorequesting.client.huds;

import com.mojang.blaze3d.systems.RenderSystem;
import k4k.travelcorequesting.client.utils.DrawContexts;
import k4k.travelcorequesting.common.animation.Animation;
import k4k.travelcorequesting.common.animation.Animator;
import k4k.travelcorequesting.common.animation.ParameterKey;
import static k4k.travelcorequesting.common.animation.ParameterAnimations.*;
import k4k.travelcorequesting.domain.enums.CompletionStatus;
import k4k.travelcorequesting.questing.models.TaskDisplay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

public class HudTaskWidget {
    private static final Identifier TASK_ICONS_TEXTURE = Identifier.of("tq", "textures/icons/default.png");

    private static final int ICON_SIZE = 8;
    private static final int ICON_GAP = 2;
    private static final int PROGRESS_BAR_GAP = 1;

    private static final ParameterKey<Integer> ICON_U = new ParameterKey<>(Integer.class, 0);
    private static final ParameterKey<Float> OPACITY = new ParameterKey<>(Float.class, 0f);
    private static final ParameterKey<Integer> POSITION = new ParameterKey<>(Integer.class, 0);
    private static final ParameterKey<Boolean> STRIKETHROUGH = new ParameterKey<>(Boolean.class, false);

    private static final Animation IN_ANIMATION = new Animation.Builder()
            .addParameter(ICON_U, switchTo(0), 2500, 1)
            .addParameter(OPACITY, fadeIn(), 0, 5000)
            .build();

    private static final Animation OUT_ANIMATION = new Animation.Builder()
            .addParameter(OPACITY, fadeOut(), 0, 5000)
            .build();

    private static final Animation SWITCH_IN_ANIMATION = new Animation.Builder()
            .addParameter(ICON_U, switchTo(0), 2500, 1)
            .addParameter(OPACITY, fadeIn(), 0, 5000)
            .build();

    private static final Animation SWITCH_OUT_ANIMATION = new Animation.Builder()
            .addParameter(OPACITY, fadeOut(), 0, 5000)
            .build();

    private static final Animation SUCCESS_ANIMATION = new Animation.Builder()
            .addParameter(ICON_U, switchTo(16), 1000, 1)
            .addParameter(POSITION, bop(2), 0, 2000)
            .build();

    private static final Animation FAILED_ANIMATION = new Animation.Builder()
            .addParameter(ICON_U, switchTo(24), 1000, 1)
            .addParameter(POSITION, bop(2), 0, 2000)
            .addParameter(STRIKETHROUGH, switchTo(true), 1, 1500)
            .build();

    private static final Animation SKIPPED_ANIMATION = new Animation.Builder()
            .addParameter(ICON_U, switchTo(32), 1000, 1)
            .addParameter(POSITION, bop(2), 0, 2000)
            .build();

    private final MinecraftClient client = MinecraftClient.getInstance();

    private final TaskDisplay display;
    private final boolean isRequired;
    private final Animator animator = new Animator(Util::getMeasuringTimeMs);

    private final @Nullable HudProgressBarWidget successBar;
    private final @Nullable HudProgressBarWidget failureBar;
    private @Nullable CompletionStatus completionStatus = null;

    public HudTaskWidget(TaskDisplay display, boolean isRequired) {
        this.display = display;
        this.isRequired = isRequired;
        this.successBar = display.successTarget() != null ? new HudProgressBarWidget(HudProgressBarWidget.SUCCESS_V) : null;
        this.failureBar = display.failureTarget() != null ? new HudProgressBarWidget(HudProgressBarWidget.FAILURE_V) : null;
    }

    public void playInAnimation() {
        animator.play(IN_ANIMATION);
    }

    public void playOutAnimation() {
        if (completionStatus != null) {
            // Задача завершена — ставим OUT в очередь, чтобы анимация завершения доиграла до конца.
            // play() прервал бы её и иконка не успела бы переключиться.
            animator.queue(OUT_ANIMATION);
        } else {
            // Задача не завершена — прерываем немедленно, чтобы не отставать от fade-out квеста.
            // Квест выставляет свой setShaderColor, но каждый HudTaskWidget перекрывает его своим —
            // без немедленного OUT задача оставалась бы видимой после исчезновения квеста.
            animator.play(OUT_ANIMATION);
        }
    }

    public void playSwitchInAnimation() {
        animator.play(SWITCH_IN_ANIMATION);
    }

    public void playSwitchOutAnimation() {
        if (completionStatus != null) {
            animator.queue(SWITCH_OUT_ANIMATION);
        } else {
            animator.play(SWITCH_OUT_ANIMATION);
        }
    }

    public boolean isAnimatorIdle() {
        return animator.isIdle();
    }

    public void complete(CompletionStatus status) {
        this.completionStatus = status;

        animator.play(switch (status) {
            case SUCCESS -> SUCCESS_ANIMATION;
            case FAILURE -> FAILED_ANIMATION;
            case SKIPPED -> SKIPPED_ANIMATION;
        });
    }

    public void setProgress(int value, boolean isSuccess) {
        if (completionStatus != null) return;

        if (isSuccess && successBar != null) {
            successBar.setProgress(value, display.successTarget());
        } else if (!isSuccess && failureBar != null) {
            failureBar.setProgress(value, display.failureTarget());
        }
    }

    public int getHeight(int maxWidth) {
        int textWidth = maxWidth - ICON_SIZE - ICON_GAP;
        int textHeight = client.textRenderer.getWrappedLinesHeight(display.title(), textWidth);

        int extraHeight = 0;
        if (completionStatus == null) {
            if (successBar != null) extraHeight += successBar.getHeight() + PROGRESS_BAR_GAP;
            if (failureBar != null) extraHeight += failureBar.getHeight() + PROGRESS_BAR_GAP;
        }

        return textHeight + extraHeight;
    }

    public int render(DrawContext context, int x, int y, int w) {
        animator.tick();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        float opacity = animator.getParameter(OPACITY);
        int offsetX = animator.getParameter(POSITION);

        int drawX = x + offsetX;

        // Тень иконки
        RenderSystem.setShaderColor(0.25f, 0.25f, 0.25f, opacity);
        int iconU = animator.getParameter(ICON_U);
        int iconV = isRequired ? ICON_SIZE : 0;
        context.drawTexture(TASK_ICONS_TEXTURE, drawX + 1, y + 1, iconU, iconV, ICON_SIZE, ICON_SIZE);

        // Иконка
        RenderSystem.setShaderColor(1, 1, 1, opacity);
        context.drawTexture(TASK_ICONS_TEXTURE, drawX, y, iconU, iconV, ICON_SIZE, ICON_SIZE);

        // Текст
        var text = animator.getParameter(STRIKETHROUGH)
                ? Text.literal("").append(display.title()).formatted(Formatting.STRIKETHROUGH)
                : display.title();

        int textX = drawX + ICON_SIZE + ICON_GAP;
        int textWidth = w - ICON_SIZE - ICON_GAP;
        int textHeight = DrawContexts.drawTextWrapped(context, client.textRenderer, text, textX, y, textWidth, 0xFFFFFFFF, true);

        // Прогресс-бары
        int totalHeight = textHeight;
        if (completionStatus == null) {
            int barY = y + textHeight + PROGRESS_BAR_GAP;

            if (successBar != null) {
                successBar.render(context, textX, barY, HudProgressBarWidget.BAR_WIDTH);
                barY += successBar.getHeight() + PROGRESS_BAR_GAP;
                totalHeight += successBar.getHeight() + PROGRESS_BAR_GAP;
            }

            if (failureBar != null) {
                failureBar.render(context, textX, barY, HudProgressBarWidget.BAR_WIDTH);
                totalHeight += failureBar.getHeight() + PROGRESS_BAR_GAP;
            }
        }

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();

        return totalHeight;
    }
}
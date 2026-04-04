package k4k.travelcorequesting.client.huds;

import com.mojang.blaze3d.systems.RenderSystem;
import k4k.travelcorequesting.client.utils.DrawContexts;
import k4k.travelcorequesting.common.animation.Animation;
import k4k.travelcorequesting.common.animation.Animator;
import k4k.travelcorequesting.common.animation.parameter_animations.*;
import k4k.travelcorequesting.domain.enums.CompletionStatus;
import k4k.travelcorequesting.questing.models.TaskDisplay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2d;

public class HudTaskWidget {
    private static final Identifier TASK_ICONS_TEXTURE = Identifier.of("tq", "textures/icons/default.png");

    private static final int ICON_SIZE = 8;
    private static final int ICON_GAP = 2;
    private static final int PROGRESS_BAR_HEIGHT = 1;
    private static final int PROGRESS_BAR_GAP = 1;

    private static final Animation IN_ANIMATION = new Animation.Builder()
            .addParameterAnimation("IconU", SwitchValueParameterAnimation.switchTo(0), Animation.ONE_TIME, 1, 250, Integer.class)
            .addParameterAnimation("Position", SlideParameterAnimation.slideIn(-10, 0), Animation.ONE_TIME, 500, Vector2d.class)
            .addParameterAnimation("Opacity", FadeParameterAnimation.fadeIn(), Animation.ONE_TIME, 500, Float.class)
            .build();

    private static final Animation SUCCESS_ANIMATION = new Animation.Builder()
            .addParameterAnimation("IconU", SwitchValueParameterAnimation.switchTo(16), Animation.ONE_TIME, 1, 100, Integer.class)
            .addParameterAnimation("Position", BopParameterAnimation.bopRight(2), Animation.ONE_TIME, 200, 0, Vector2d.class)
            .build();

    private static final Animation FAILED_ANIMATION = new Animation.Builder()
            .addParameterAnimation("IconU", SwitchValueParameterAnimation.switchTo(24), Animation.ONE_TIME, 1, 100, Integer.class)
            .addParameterAnimation("Position", BopParameterAnimation.bopRight(2), Animation.ONE_TIME, 200, 0, Vector2d.class)
            .addParameterAnimation("Strikethrough", SwitchValueParameterAnimation.switchTo(true), Animation.ONE_TIME, 1, 150, Boolean.class)
            .build();

    private static final Animation SKIPPED_ANIMATION = new Animation.Builder()
            .addParameterAnimation("IconU", SwitchValueParameterAnimation.switchTo(32), Animation.ONE_TIME, 1, 100, Integer.class)
            .addParameterAnimation("Position", BopParameterAnimation.bopRight(2), Animation.ONE_TIME, 200, 0, Vector2d.class)
            .build();

    private final MinecraftClient client = MinecraftClient.getInstance();

    private TaskDisplay display;
    private final boolean isRequired;
    private final Animator animator = new Animator();

    private int successProgress = 0;
    private int failureProgress = 0;
    private @Nullable CompletionStatus completionStatus = null;

    public HudTaskWidget(TaskDisplay display, boolean isRequired) {
        this.display = display;
        this.isRequired = isRequired;
    }

    public void playInAnimation() {
        animator.play(IN_ANIMATION, Util.getMeasuringTimeMs());
    }

    public void complete(CompletionStatus status) {
        this.completionStatus = status;

        animator.play(switch (status) {
            case SUCCESS -> SUCCESS_ANIMATION;
            case FAILURE -> FAILED_ANIMATION;
            case SKIPPED -> SKIPPED_ANIMATION;
        }, Util.getMeasuringTimeMs());
    }

    public void setProgress(int value, boolean isSuccess) {
        if (completionStatus != null) return;

        if (isSuccess) {
            this.successProgress = value;
        } else {
            this.failureProgress = value;
        }
    }

    public void setDisplay(TaskDisplay display) {
        this.display = display;
    }

    public int getHeight(int maxWidth) {
        int textWidth = maxWidth - ICON_SIZE - ICON_GAP;
        int textHeight = client.textRenderer.getWrappedLinesHeight(display.title(), textWidth);

        int extraHeight = 0;
        if (completionStatus == null) {
            if (display.successTarget() != null) extraHeight += PROGRESS_BAR_HEIGHT + PROGRESS_BAR_GAP;
            if (display.failureTarget() != null) extraHeight += PROGRESS_BAR_HEIGHT + PROGRESS_BAR_GAP;
        }

        return textHeight + extraHeight;
    }

    public int render(DrawContext context, long t, int x, int y, int w) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        float opacity = animator.getParameter("Opacity", t, Float.class).orElse(1f);
        int offsetX = (int) animator.getParameter("Position", t, Vector2d.class).orElseGet(Vector2d::new).x;

        int drawX = x + offsetX;

        // Тень иконки
        RenderSystem.setShaderColor(0.25f, 0.25f, 0.25f, opacity);
        int iconU = animator.getParameter("IconU", t, Integer.class).orElse(0);
        int iconV = isRequired ? ICON_SIZE : 0;
        context.drawTexture(TASK_ICONS_TEXTURE, drawX + 1, y + 1, iconU, iconV, ICON_SIZE, ICON_SIZE);

        // Иконка
        RenderSystem.setShaderColor(1, 1, 1, opacity);
        context.drawTexture(TASK_ICONS_TEXTURE, drawX, y, iconU, iconV, ICON_SIZE, ICON_SIZE);

        // Текст
        var text = animator.getParameter("Strikethrough", t, Boolean.class).orElse(false)
                ? Text.literal("").append(display.title()).formatted(Formatting.STRIKETHROUGH)
                : display.title();

        int textX = drawX + ICON_SIZE + ICON_GAP;
        int textWidth = w - ICON_SIZE - ICON_GAP;
        int textHeight = DrawContexts.drawTextWrapped(context, client.textRenderer, text, textX, y, textWidth, 0xFFFFFFFF, true);

        // Прогресс-бары
        int totalHeight = textHeight;
        if (completionStatus == null) {
            int barY = y + textHeight + PROGRESS_BAR_GAP;

            if (display.successTarget() != null) {
                renderProgressBar(context, textX, barY, textWidth, successProgress, display.successTarget(), 0xAA00FF00);
                barY += PROGRESS_BAR_HEIGHT + PROGRESS_BAR_GAP;
                totalHeight += PROGRESS_BAR_HEIGHT + PROGRESS_BAR_GAP;
            }

            if (display.failureTarget() != null) {
                renderProgressBar(context, textX, barY, textWidth, failureProgress, display.failureTarget(), 0xAAFF0000);
                totalHeight += PROGRESS_BAR_HEIGHT + PROGRESS_BAR_GAP;
            }
        }

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();

        return totalHeight;
    }

    private void renderProgressBar(DrawContext context, int x, int y, int maxWidth, int current, int target, int color) {
        int clamped = Math.min(current, target);
        int barWidth = (int) (maxWidth * ((float) clamped / target));
        if (barWidth > 0) {
            context.fill(x, y, x + barWidth, y + PROGRESS_BAR_HEIGHT, color);
        }
    }
}
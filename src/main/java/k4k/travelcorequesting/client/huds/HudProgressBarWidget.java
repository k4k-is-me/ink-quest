package k4k.travelcorequesting.client.huds;

import com.mojang.blaze3d.systems.RenderSystem;
import k4k.travelcorequesting.common.animation.Animation;
import k4k.travelcorequesting.common.animation.Animator;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import java.util.function.Function;

public class HudProgressBarWidget {
    private static final Identifier PROGRESS_BAR_TEXTURE = Identifier.of("tq", "textures/icons/default.png");

    private static final int BAR_HEIGHT = 1;
    public static final int BAR_WIDTH = 32;
    public static final int BG_V = 32;
    public static final int SUCCESS_V = 33;
    public static final int FAILURE_V = 34;

    private static final Function<Float, Animation> FILL_ANIMATION = delta -> new Animation.Builder()
            .addParameterAnimation("FillDelta", progress -> delta * (float) (1.0 - progress), Animation.ONE_TIME, 300, Float.class)
            .build();

    private final int textureV;
    private final Animator animator = new Animator();
    private float currentFill = 0f;

    public HudProgressBarWidget(int textureV) {
        this.textureV = textureV;
    }

    public void setProgress(int value, int target) {
        long now = Util.getMeasuringTimeMs();
        float newFill = MathHelper.clamp((float) value / target, 0f, 1f);
        float animDelta = animator.getParameter("FillDelta", now, Float.class).orElse(0f);
        float delta = (currentFill + animDelta) - newFill;
        currentFill = newFill;
        if (delta != 0f) animator.play(FILL_ANIMATION.apply(delta), now);
    }

    public int getHeight() {
        return BAR_HEIGHT + 1; // 1px бар + 1px тень
    }

    public void render(DrawContext context, long t, int x, int y, int width) {
        float fillDelta = animator.getParameter("FillDelta", t, Float.class).orElse(0f);
        int fillWidth = (int) (MathHelper.clamp(currentFill + fillDelta, 0f, 1f) * width);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        context.drawTexture(PROGRESS_BAR_TEXTURE, x, y, 0, BG_V, width, BAR_HEIGHT);

        if (fillWidth > 0) {
            RenderSystem.setShaderColor(0.25f, 0.25f, 0.25f, 1f);
            context.drawTexture(PROGRESS_BAR_TEXTURE, x + 1, y + 1, 0, textureV, fillWidth, BAR_HEIGHT);

            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            context.drawTexture(PROGRESS_BAR_TEXTURE, x, y, 0, textureV, fillWidth, BAR_HEIGHT);
        }

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }
}
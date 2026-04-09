package k4k.travelcorequesting.client.huds;

import com.mojang.blaze3d.systems.RenderSystem;
import k4k.travelcorequesting.common.animation.Animation;
import k4k.travelcorequesting.common.animation.Animator;
import k4k.travelcorequesting.common.animation.ParameterKey;
import static k4k.travelcorequesting.common.animation.ParameterAnimations.*;
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

    private static final ParameterKey<Float> FILL = new ParameterKey<>(Float.class, 0f);

    private static final Function<Float, Animation> FILL_ANIMATION = target -> new Animation.Builder()
            .addParameter(FILL, slideTo(target), 0, 300)
            .build();

    private final int textureV;
    private final Animator animator = new Animator(Util::getMeasuringTimeMs);

    public HudProgressBarWidget(int textureV) {
        this.textureV = textureV;
    }

    public void setProgress(int value, int target) {
        float newFill = MathHelper.clamp((float) value / target, 0f, 1f);
        animator.play(FILL_ANIMATION.apply(newFill));
    }

    public int getHeight() {
        return BAR_HEIGHT + 1; // 1px бар + 1px тень
    }

    public void render(DrawContext context, int x, int y, int width) {
        animator.tick();
        int fillWidth = (int) (animator.getParameter(FILL) * width);

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
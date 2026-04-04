package k4k.travelcorequesting.common.animation.parameter_animations;

import k4k.travelcorequesting.common.animation.ParameterAnimation;

/**
 * Линейная интерполяция прозрачности ({@code Float}).
 *
 * <pre>{@code
 * // Появление: opacity 0.0 → 1.0
 * FadeParameterAnimation.fadeIn()
 *
 * // Исчезновение: opacity 1.0 → 0.0
 * FadeParameterAnimation.fadeOut()
 * }</pre>
 *
 * <p>Параметр имеет тип {@code Float}, читать через:
 * <pre>{@code
 * float opacity = animator.getParameter("Opacity", t, Float.class).orElse(1f);
 * RenderSystem.setShaderColor(1, 1, 1, opacity);
 * }</pre>
 */
public class FadeParameterAnimation implements ParameterAnimation<Float> {
    private final float fadeFrom;
    private final float fadeTo;

    private FadeParameterAnimation(float fadeFrom, float fadeTo) {
        this.fadeFrom = fadeFrom;
        this.fadeTo = fadeTo;
    }

    /** Анимация появления: {@code 0.0 → 1.0} */
    public static FadeParameterAnimation fadeIn() {
        return new FadeParameterAnimation(0, 1);
    }

    /** Анимация исчезновения: {@code 1.0 → 0.0} */
    public static FadeParameterAnimation fadeOut() {
        return new FadeParameterAnimation(1, 0);
    }

    @Override
    public Float animate(double t) {
        return (float) (this.fadeFrom + (this.fadeTo - this.fadeFrom) * t);
    }
}
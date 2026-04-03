package k4k.travelcorequesting.common.animation.parameter_animations;

import k4k.travelcorequesting.common.animation.ParameterAnimation;

public class FadeParameterAnimation implements ParameterAnimation<Float> {
    private final float fadeFrom;
    private final float fadeTo;

    private FadeParameterAnimation(float fadeFrom, float fadeTo) {
        this.fadeFrom = fadeFrom;
        this.fadeTo = fadeTo;
    }

    public static FadeParameterAnimation fadeIn() {
        return new FadeParameterAnimation(0, 1);
    }

    public static FadeParameterAnimation fadeOut() {
        return new FadeParameterAnimation(1, 0);
    }

    @Override
    public Float animate(double t) {
        return (float) (this.fadeFrom + (this.fadeTo - this.fadeFrom) * t);
    }
}

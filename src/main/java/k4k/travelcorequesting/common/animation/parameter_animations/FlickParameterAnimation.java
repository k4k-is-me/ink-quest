package k4k.travelcorequesting.common.animation.parameter_animations;

import k4k.travelcorequesting.common.animation.ParameterAnimation;

public class FlickParameterAnimation implements ParameterAnimation<Boolean> {
    private final int flicks;

    public FlickParameterAnimation(int flicks) {
        if (flicks <= 0) throw new IllegalArgumentException("Number of flicks must be greater than zero");
        this.flicks = flicks;
    }

    @Override
    public Boolean animate(double t) {
        if (t <= 0.001) return false;
        if (t >= 0.999) return true;
        return Math.sin(t * Math.PI * 2 * this.flicks) > 0;
    }
}

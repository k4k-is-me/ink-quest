package k4k.travelcorequesting.common.animation.parameter_animations;

import k4k.travelcorequesting.common.animation.ParameterAnimation;

public class SwitchParameterAnimation implements ParameterAnimation<Boolean> {
    @Override
    public Boolean animate(double t) {
        return t > 0.0001;
    }
}

package k4k.travelcorequesting.common.animation.parameter_animations;

import k4k.travelcorequesting.common.animation.ParameterAnimation;

public class SwitchValueParameterAnimation<T> implements ParameterAnimation<T> {
    private final T valueBefore;
    private final T valueWhile;
    private final T valueAfter;

    private SwitchValueParameterAnimation(T valueBefore, T valueWhile, T valueAfter) {
        this.valueBefore = valueBefore;
        this.valueWhile = valueWhile;
        this.valueAfter = valueAfter;
    }

    public static <T> SwitchValueParameterAnimation<T> switchTo(T value) {
        return new SwitchValueParameterAnimation<>(null, value, value);
    }

    public static <T> SwitchValueParameterAnimation<T> temporarilySwitchTo(T value) {
        return new SwitchValueParameterAnimation<>(null, value, null);
    }

    public static <T> SwitchValueParameterAnimation<T> switchToDefaultFrom(T value) {
        return new SwitchValueParameterAnimation<>(value, null, null);
    }

    @Override
    public T animate(double t) {
        if (t <= 0.001) return this.valueBefore;
        if (t >= 0.999) return this.valueAfter;
        return this.valueWhile;
    }
}

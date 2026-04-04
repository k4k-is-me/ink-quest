package k4k.travelcorequesting.common.animation.parameter_animations;

import k4k.travelcorequesting.common.animation.ParameterAnimation;

/**
 * Мигание ({@code Boolean}): быстрое переключение {@code true/false} заданное количество раз.
 *
 * <p>Использует синус для мигания: {@code sin(t·π·2·flicks) > 0}.
 * В начале ({@code t=0}) и конце ({@code t=1}) всегда возвращает {@code false}/{@code true}.
 *
 * <pre>{@code
 * // 3 мигания за время анимации
 * new FlickParameterAnimation(3)
 * }</pre>
 *
 * <p>Полезно для привлечения внимания — например, мигание иконки предупреждения.
 * <pre>{@code
 * .addParameterAnimation("Visible", new FlickParameterAnimation(3),
 *     Animation.ONE_TIME, 600, Boolean.class)
 * }</pre>
 */
public class FlickParameterAnimation implements ParameterAnimation<Boolean> {
    private final int flicks;

    /**
     * @param flicks количество полных миганий за время анимации (должно быть > 0)
     */
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
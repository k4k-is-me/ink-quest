package k4k.travelcorequesting.common.animation.parameter_animations;

import k4k.travelcorequesting.common.animation.ParameterAnimation;

/**
 * Мгновенное переключение булевого флага ({@code Boolean}): {@code false → true}.
 *
 * <p>Возвращает {@code false} при {@code t = 0} и {@code true} при {@code t > 0}.
 * Аналог {@link SwitchValueParameterAnimation#switchTo(Object)}, но специфичен для булевых флагов
 * и не имеет состояния «до».
 *
 * <p>Типичное использование — включение эффекта (например, зачёркивания) в момент старта анимации:
 * <pre>{@code
 * .addParameterAnimation("Strikethrough", new SwitchParameterAnimation(),
 *     Animation.ONE_TIME, 1, 150, Boolean.class)
 * }</pre>
 *
 * <p>Если нужно больше контроля над значениями до/во время/после — используй
 * {@link SwitchValueParameterAnimation}.
 */
public class SwitchParameterAnimation implements ParameterAnimation<Boolean> {
    @Override
    public Boolean animate(double t) {
        return t > 0.0001;
    }
}
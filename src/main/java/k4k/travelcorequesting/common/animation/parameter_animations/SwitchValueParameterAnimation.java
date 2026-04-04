package k4k.travelcorequesting.common.animation.parameter_animations;

import k4k.travelcorequesting.common.animation.ParameterAnimation;

/**
 * Мгновенная смена значения в заданный момент ({@code T}).
 *
 * <p>В отличие от плавных анимаций, не интерполирует — просто переключает значение.
 * Удобно для смены иконки, флага или любого дискретного состояния через систему анимаций,
 * сохраняя возможность задать задержку через параметр {@code delay} в {@link k4k.travelcorequesting.common.animation.Animation.Builder}.
 *
 * <h2>Фабричные методы</h2>
 * <pre>{@code
 * // Значение null до t=0, затем value навсегда
 * SwitchValueParameterAnimation.switchTo(value)
 *
 * // Значение null до t=0, value пока анимация идёт, null после
 * SwitchValueParameterAnimation.flashTo(value)
 *
 * // Значение value до t=1, null после
 * SwitchValueParameterAnimation.dropFrom(value)
 * }</pre>
 *
 * <h2>Пример: смена иконки при завершении задачи</h2>
 * <pre>{@code
 * // Иконка переключается на U=16 сразу при старте анимации (delay=0):
 * .addParameterAnimation("IconU", SwitchValueParameterAnimation.switchTo(16),
 *     Animation.ONE_TIME, 1, Integer.class)
 *
 * // Иконка переключается на U=16 через 100мс после старта:
 * .addParameterAnimation("IconU", SwitchValueParameterAnimation.switchTo(16),
 *     Animation.ONE_TIME, 1, 100, Integer.class)
 * }</pre>
 *
 * <p>Параметр {@code duration=1} означает «1мс» — по сути мгновенно.
 * Реальная задержка задаётся через {@code delay}, а не через {@code duration}.
 */
public class SwitchValueParameterAnimation<T> implements ParameterAnimation<T> {
    private final T valueBefore;
    private final T valueWhile;
    private final T valueAfter;

    private SwitchValueParameterAnimation(T valueBefore, T valueWhile, T valueAfter) {
        this.valueBefore = valueBefore;
        this.valueWhile = valueWhile;
        this.valueAfter = valueAfter;
    }

    /**
     * {@code null → value → value}.
     * Значение устанавливается в момент старта анимации и остаётся навсегда.
     * Самый частый вариант — смена иконки или флага.
     */
    public static <T> SwitchValueParameterAnimation<T> switchTo(T value) {
        return new SwitchValueParameterAnimation<>(null, value, value);
    }

    /**
     * {@code null → value → null}.
     * Значение появляется во время анимации и исчезает после.
     * Полезно для временных эффектов.
     */
    public static <T> SwitchValueParameterAnimation<T> flashTo(T value) {
        return new SwitchValueParameterAnimation<>(null, value, null);
    }

    /**
     * {@code value → null → null}.
     * Значение есть «до» анимации и сбрасывается после.
     */
    public static <T> SwitchValueParameterAnimation<T> dropFrom(T value) {
        return new SwitchValueParameterAnimation<>(value, null, null);
    }

    @Override
    public T animate(double t) {
        if (t <= 0.001) return this.valueBefore;
        if (t >= 0.999) return this.valueAfter;
        return this.valueWhile;
    }
}
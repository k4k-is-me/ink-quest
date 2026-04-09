package k4k.travelcorequesting.common.animation;

/**
 * Функция интерполяции одного параметра анимации.
 *
 * <p>Принимает начальное значение параметра {@code initial} и прогресс {@code t ∈ [0.0, 1.0]},
 * возвращает значение параметра в данный момент анимации. За отображение реального времени
 * в {@code t} отвечает {@link Animation.AnimationFillMode}.
 *
 * <p>Значение {@code initial} — это значение параметра в момент начала анимации (из снимка
 * предыдущей анимации или дефолт из {@link ParameterKey}). Реализация может использовать
 * его как стартовую точку или игнорировать.
 *
 * @param <T> тип анимируемого значения (Float, Integer, Boolean и т.д.)
 * @see Animation.Builder#addParameter
 */
@FunctionalInterface
public interface ParameterAnimation<T> {
    /**
     * Вычисляет значение параметра в момент {@code t}.
     *
     * @param initial значение параметра в момент начала анимации
     * @param t       прогресс анимации от {@code 0.0} (начало) до {@code 1.0} (конец)
     * @return значение параметра в данный момент
     */
    T animate(T initial, double t);
}
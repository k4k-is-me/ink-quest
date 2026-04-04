package k4k.travelcorequesting.common.animation;

/**
 * Функция интерполяции одного параметра анимации.
 *
 * <p>Принимает {@code t ∈ [0.0, 1.0]} и возвращает значение параметра
 * в данный момент анимации. За отображение реального времени в {@code t}
 * отвечает {@link Animation.AnimationFillMode}.
 *
 * <p>Чтобы добавить новый тип анимации — реализуй этот интерфейс:
 * <pre>{@code
 * public class MyAnimation implements ParameterAnimation<Float> {
 *     public Float animate(double t) {
 *         return (float) Math.sin(t * Math.PI); // 0 → 1 → 0
 *     }
 * }
 * }</pre>
 *
 * @param <T> тип анимируемого значения (Float, Vector2d, Integer, Boolean и т.д.)
 * @see Animation.Builder#addParameterAnimation
 */
@FunctionalInterface
public interface ParameterAnimation<T> {
    /**
     * Вычисляет значение параметра в момент {@code t}.
     *
     * @param t прогресс анимации от {@code 0.0} (начало) до {@code 1.0} (конец)
     * @return значение параметра в данный момент
     */
    T animate(double t);
}
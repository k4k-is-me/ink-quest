package k4k.travelcorequesting.common.animation;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;

/**
 * Проигрыватель анимаций. Хранит текущую анимацию и очередь следующих.
 *
 * <p>Каждый объект с анимацией (виджет, иконка и т.д.) должен иметь свой {@code Animator}.
 *
 * <h2>Запуск анимации</h2>
 * <pre>{@code
 * Animator animator = new Animator();
 *
 * // Запустить немедленно, сбросив текущую
 * animator.play(myAnimation, Util.getMeasuringTimeMs());
 *
 * // Поставить следующей после текущей
 * animator.queue(idleAnimation);
 * }</pre>
 *
 * <h2>Чтение параметров при рендере</h2>
 * <pre>{@code
 * long t = Util.getMeasuringTimeMs();
 *
 * float opacity   = animator.getParameter("Opacity", t, Float.class).orElse(1f);
 * Vector2d offset = animator.getParameter("Position", t, Vector2d.class).orElseGet(Vector2d::new);
 * int iconU       = animator.getParameterOrDefault("IconU", t, 0, Integer.class);
 * }</pre>
 *
 * <p>Если параметр с таким именем не задан в текущей анимации — возвращается {@code empty}/default.
 * Это нормально: просто используй default-значение как «статичное» состояние.
 *
 * <h2>Поведение очереди</h2>
 * <ul>
 *   <li>{@link #play} немедленно сбрасывает очередь и начинает новую анимацию.
 *   <li>{@link #queue} добавляет анимацию после текущей.
 *   <li>Последняя анимация в очереди никогда не извлекается — она продолжает «играть» бесконечно.
 *       Для {@link Animation#ONE_TIME} параметры застывают на {@code t = 1.0}.
 *       Для {@link Animation#LOOP} и {@link Animation#SYNCED_LOOP} параметры продолжают циклиться.
 * </ul>
 */
public class Animator {
    private final Queue<Animation> animationQueue = new ArrayDeque<>();
    private long animationStartTime;

    /**
     * Немедленно запускает анимацию, сбрасывая текущую и очередь.
     *
     * @param animation анимация для воспроизведения
     * @param startTime абсолютное время начала (обычно {@code Util.getMeasuringTimeMs()})
     */
    public void play(Animation animation, long startTime) {
        this.animationQueue.clear();
        this.animationQueue.add(animation);
        this.animationStartTime = startTime;
    }

    /**
     * Добавляет анимацию в очередь — она начнётся после завершения текущих.
     * Если очередь пуста, анимация начнётся немедленно при следующем тике.
     */
    public void queue(Animation animation) {
        this.animationQueue.add(animation);
    }

    /**
     * Останавливает воспроизведение и очищает очередь.
     * После вызова все {@link #getParameter} будут возвращать {@code empty}.
     */
    public void stop() {
        this.animationQueue.clear();
    }

    private Optional<Animation> getPlayedAnimation(long currentTime) {
        // Переходим к следующей анимации в очереди, если текущая завершилась.
        // Последнюю анимацию не извлекаем.
        while (animationQueue.size() > 1 && currentTime > this.animationStartTime + (long) this.animationQueue.peek().getDuration()) {
            this.animationStartTime += (long) Objects.requireNonNull(this.animationQueue.poll()).getDuration();
        }

        if (animationQueue.isEmpty())
            return Optional.empty();

        return Optional.of(animationQueue.peek());
    }

    /**
     * Возвращает текущее значение именованного параметра из играющей анимации.
     *
     * @param parameterKey имя параметра, заданное в {@link Animation.Builder#addParameterAnimation}
     * @param currentTime  абсолютное текущее время (мс), обычно {@code Util.getMeasuringTimeMs()}
     * @param expectedType класс ожидаемого типа
     * @return значение параметра, или {@code empty} если нет играющей анимации или параметр не найден
     */
    public <T> Optional<T> getParameter(String parameterKey, long currentTime, Class<T> expectedType) {
        return this.getPlayedAnimation(currentTime)
                .flatMap(animation -> animation
                        .getParameter(parameterKey, currentTime - animationStartTime, expectedType));
    }

    /**
     * То же что {@link #getParameter}, но возвращает {@code defaultValue} вместо {@code empty}.
     */
    public <T> T getParameterOrDefault(String parameterKey, long currentTime, T defaultValue, Class<T> expectedType) {
        return this.getParameter(parameterKey, currentTime, expectedType).orElse(defaultValue);
    }

    /**
     * Возвращает {@code true} если аниматор простаивает — либо ничего не играло,
     * либо текущая анимация завершила своё время ({@code currentTime - startTime >= duration}).
     *
     * <p>Удобно для очистки: если аниматор простаивает — объект можно удалять.
     * <pre>{@code
     * if (outgoingAnimator.isIdle(t)) {
     *     outgoingTaskWidgets.clear();
     * }
     * }</pre>
     *
     */
    public boolean isIdle(long currentTime) {
        if (animationQueue.isEmpty()) return true;
        if (animationQueue.size() > 1) return false;
        return currentTime - animationStartTime >= (long) animationQueue.peek().getDuration();
    }
}
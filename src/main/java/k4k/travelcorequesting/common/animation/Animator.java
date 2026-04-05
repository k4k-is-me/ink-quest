package k4k.travelcorequesting.common.animation;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
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
 * <p>Если параметр с таким именем не задан в текущей анимации — возвращается значение из снимка
 * предыдущих анимаций. Снимок обновляется при каждом переходе между анимациями.
 * Если параметра нет ни в анимации, ни в снимке — возвращается {@code empty}/default.
 *
 * <h2>Поведение очереди</h2>
 * <ul>
 *   <li>{@link #play} немедленно сбрасывает очередь и начинает новую анимацию.
 *   <li>{@link #queue} добавляет анимацию после текущей.
 *   <li>Последняя анимация в очереди никогда не извлекается — она продолжает «играть» бесконечно.
 *       Для {@link Animation#ONE_TIME} параметры застывают на {@code t = 1.0}.
 *       Для {@link Animation#LOOP} и {@link Animation#ANIMATION_LOOP} параметры продолжают циклиться.
 * </ul>
 */
public class Animator {
    private final Queue<Animation> animationQueue = new ArrayDeque<>();
    private long animationStartTime;
    private final Map<String, Object> snapshot = new HashMap<>();

    /**
     * Немедленно запускает анимацию, сбрасывая текущую и очередь.
     * Текущие значения параметров сохраняются в снимок перед заменой.
     *
     * @param animation анимация для воспроизведения
     * @param startTime абсолютное время начала (обычно {@code Util.getMeasuringTimeMs()})
     */
    public void play(Animation animation, long startTime) {
        if (!animationQueue.isEmpty()) {
            mergeIntoSnapshot(animationQueue.peek(), startTime - animationStartTime);
        }
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
     * Останавливает воспроизведение, сохраняя текущие значения параметров в снимок.
     * После вызова {@link #getParameter} будет возвращать значения из снимка.
     *
     * @param currentTime абсолютное текущее время (мс)
     */
    public void stop(long currentTime) {
        if (!animationQueue.isEmpty()) {
            mergeIntoSnapshot(animationQueue.peek(), currentTime - animationStartTime);
        }
        this.animationQueue.clear();
    }

    /**
     * Полностью сбрасывает аниматор, включая снимок.
     * После вызова все {@link #getParameter} будут возвращать {@code empty}.
     */
    public void clear() {
        this.animationQueue.clear();
        this.snapshot.clear();
    }

    private Optional<Animation> getPlayedAnimation(long currentTime) {
        // Переходим к следующей анимации в очереди, если текущая завершилась.
        // Последнюю анимацию не извлекаем.
        while (animationQueue.size() > 1 && currentTime > this.animationStartTime + (long) this.animationQueue.peek().getDuration()) {
            Animation completed = Objects.requireNonNull(this.animationQueue.poll());
            mergeIntoSnapshot(completed, (long) completed.getDuration());
            this.animationStartTime += (long) completed.getDuration();
        }

        if (animationQueue.isEmpty())
            return Optional.empty();

        return Optional.of(animationQueue.peek());
    }

    /**
     * Возвращает текущее значение именованного параметра из играющей анимации.
     * Если параметр не определён в текущей анимации — возвращает значение из снимка предыдущих.
     *
     * @param parameterKey имя параметра, заданное в {@link Animation.Builder#addParameterAnimation}
     * @param currentTime  абсолютное текущее время (мс), обычно {@code Util.getMeasuringTimeMs()}
     * @param expectedType класс ожидаемого типа
     * @return значение параметра, или {@code empty} если нет играющей анимации и параметра нет в снимке
     */
    public <T> Optional<T> getParameter(String parameterKey, long currentTime, Class<T> expectedType) {
        Optional<T> result = this.getPlayedAnimation(currentTime)
                .flatMap(animation -> animation
                        .getParameter(parameterKey, currentTime - animationStartTime, expectedType));

        if (result.isPresent()) return result;

        Object snapshotValue = snapshot.get(parameterKey);
        if (expectedType.isInstance(snapshotValue)) {
            return Optional.of(expectedType.cast(snapshotValue));
        }
        return Optional.empty();
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

    private void mergeIntoSnapshot(Animation animation, long elapsed) {
        for (String key : animation.getParameterKeys()) {
            animation.getParameter(key, elapsed, Object.class).ifPresent(v -> snapshot.put(key, v));
        }
    }
}
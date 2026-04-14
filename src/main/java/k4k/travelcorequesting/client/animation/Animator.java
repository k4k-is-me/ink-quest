package k4k.travelcorequesting.client.animation;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.function.LongSupplier;

/**
 * Проигрыватель анимаций. Хранит текущую анимацию и очередь следующих.
 *
 * <p>Каждый объект с анимацией (виджет, иконка и т.д.) должен иметь свой {@code Animator}.
 * Время получает из {@link LongSupplier}, переданного при создании.
 *
 * <h2>Запуск анимации</h2>
 * <pre>{@code
 * Animator animator = new Animator(Util::getMeasuringTimeMs);
 *
 * // Запустить немедленно, сбросив текущую
 * animator.play(myAnimation);
 *
 * // Поставить следующей после текущей (если аниматор idle — ведёт себя как play)
 * animator.queue(idleAnimation);
 * }</pre>
 *
 * <h2>Чтение параметров при рендере</h2>
 * <pre>{@code
 * animator.tick(); // один раз в начале render(), захватывает t
 *
 * float opacity = animator.getParameter(OPACITY);
 * int   offsetX = animator.getParameter(POSITION);
 * int   iconU   = animator.getParameter(ICON_U);
 * }</pre>
 *
 * <p>Если параметр с таким именем не задан в текущей анимации — возвращается значение из снимка
 * предыдущих анимаций. Снимок обновляется при каждом переходе между анимациями.
 * Если параметра нет ни в анимации, ни в снимке — возвращается {@code empty}/default.
 *
 * <h2>Поведение очереди</h2>
 * <ul>
 *   <li>{@link #play} немедленно заменяет текущую анимацию, сбрасывая очередь.
 *   <li>{@link #queue} добавляет анимацию в очередь ожидания. Если аниматор простаивает —
 *       начинает немедленно (как {@link #play}).
 *   <li>Завершённая анимация остаётся «замороженной» на {@code t = 1.0} пока не придёт следующая.
 *       Для {@link Animation#LOOP} и {@link Animation#ANIMATION_LOOP} параметры продолжают циклиться.
 * </ul>
 */
public class Animator {
    private final LongSupplier timeSupplier;
    private @Nullable Animation current = null;
    private final Queue<Animation> pending = new ArrayDeque<>();
    private final Map<ParameterKey<?>, Object> snapshot = new HashMap<>();
    private long animationStartTime;
    private long tickedTime;

    public Animator(LongSupplier timeSupplier) {
        this.timeSupplier = timeSupplier;
    }

    /**
     * Захватывает текущее время и продвигает очередь анимаций.
     * Должен вызываться один раз в начале каждого кадра (перед первым {@link #getParameter}).
     * Гарантирует, что все параметры в одном кадре читаются в одной точке времени.
     */
    public void tick() {
        long now = timeSupplier.getAsLong();
        while (current != null && !pending.isEmpty() && now > animationStartTime + (long) current.getDuration()) {
            mergeIntoSnapshot(current, (long) current.getDuration());
            animationStartTime += (long) current.getDuration();
            current = pending.poll();
        }
        this.tickedTime = now;
    }

    /**
     * Немедленно запускает анимацию, заменяя текущую и сбрасывая очередь.
     * Текущие значения параметров сохраняются в снимок перед заменой.
     */
    public void play(Animation animation) {
        long now = timeSupplier.getAsLong();
        if (current != null) {
            mergeIntoSnapshot(current, now - animationStartTime);
        }
        this.current = animation;
        this.pending.clear();
        this.animationStartTime = now;
    }

    /**
     * Добавляет анимацию в очередь — она начнётся после завершения текущих.
     * Если аниматор простаивает ({@link #isIdle}), начинает анимацию немедленно.
     */
    public void queue(Animation animation) {
        if (isIdle()) {
            play(animation);
        } else {
            this.pending.add(animation);
        }
    }

    /**
     * Останавливает воспроизведение, сохраняя текущие значения параметров в снимок.
     * После вызова {@link #getParameter} будет возвращать значения из снимка.
     */
    public void stop() {
        long now = timeSupplier.getAsLong();
        if (current != null) {
            mergeIntoSnapshot(current, now - animationStartTime);
        }
        this.current = null;
        this.pending.clear();
    }

    /**
     * Полностью сбрасывает аниматор, включая снимок.
     * После вызова все {@link #getParameter} будут возвращать {@code empty}.
     */
    public void clear() {
        this.current = null;
        this.pending.clear();
        this.snapshot.clear();
    }

    /**
     * Возвращает текущее значение именованного параметра.
     * Приоритет: текущая анимация → снимок предыдущих → дефолт из {@link ParameterKey}.
     *
     * @param key ключ параметра, заданный в {@link Animation.Builder#addParameter}
     * @return значение параметра
     */
    public <T> T getParameter(ParameterKey<T> key) {
        if (current != null) {
            Optional<T> result = current.getParameter(key, getInitial(key), tickedTime - animationStartTime);
            if (result.isPresent()) return result.get();
        }
        return getInitial(key);
    }

    private <T> T getInitial(ParameterKey<T> key) {
        Object snapshotValue = snapshot.get(key);
        if (key.getType().isInstance(snapshotValue)) {
            return key.getType().cast(snapshotValue);
        }
        return key.getDefault();
    }

    /**
     * Возвращает {@code true} если аниматор простаивает — либо ничего не играло,
     * либо текущая анимация завершила своё время и очередь пуста.
     *
     * <p>Удобно для очистки: если аниматор простаивает — объект можно удалять.
     * <pre>{@code
     * if (animator.isIdle()) {
     *     outgoingTaskWidgets.clear();
     * }
     * }</pre>
     */
    public boolean isIdle() {
        if (current == null) return true;
        if (!pending.isEmpty()) return false;
        return timeSupplier.getAsLong() - animationStartTime >= (long) current.getDuration();
    }

    private void mergeIntoSnapshot(Animation animation, long elapsed) {
        for (ParameterKey<?> key : animation.getParameterKeys()) {
            mergeKeyIntoSnapshot(animation, key, elapsed);
        }
    }

    private <T> void mergeKeyIntoSnapshot(Animation animation, ParameterKey<T> key, long elapsed) {
        animation.getParameter(key, getInitial(key), elapsed).ifPresent(v -> snapshot.put(key, v));
    }
}
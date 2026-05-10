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
 *
 * // С колбэком на естественное завершение
 * animator.play(fadeOut, () -> widget.remove());
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
 *
 * <h2>Колбэки {@code onEnd}</h2>
 * <p>Перегрузки {@link #play(Animation, Runnable)} и {@link #queue(Animation, Runnable)} принимают
 * колбэк, который вызывается ровно один раз при <b>естественном</b> завершении конкретной анимации:
 * либо при переходе на следующую из очереди, либо при истечении длительности «замороженного хвоста»,
 * когда очередь пуста. Срабатывание происходит изнутри {@link #tick()} <b>после</b> обновления
 * состояния аниматора, поэтому из колбэка безопасно вызывать {@code play}/{@code queue}.
 *
 * <p>Колбэк <b>не вызывается</b> при отмене:
 * вытеснение через {@link #play}, {@link #stop}, {@link #clear}.
 */
public class Animator {
    private final LongSupplier timeSupplier;
    private @Nullable Animation current = null;
    private @Nullable Runnable currentCallback = null;
    private final Queue<PendingEntry> pending = new ArrayDeque<>();
    private final Map<ParameterKey<?>, Object> snapshot = new HashMap<>();
    private long animationStartTime;
    private long tickedTime;

    /** Запись в очереди ожидания: анимация и опциональный колбэк её естественного завершения. */
    private record PendingEntry(Animation animation, @Nullable Runnable onEnd) {}

    public Animator(LongSupplier timeSupplier) {
        this.timeSupplier = timeSupplier;
    }

    /**
     * Захватывает текущее время и продвигает очередь анимаций.
     * Должен вызываться один раз в начале каждого кадра (перед первым {@link #getParameter}).
     * Гарантирует, что все параметры в одном кадре читаются в одной точке времени.
     *
     * <p>В этом методе срабатывают колбэки {@code onEnd}: при каждом переходе на следующую
     * анимацию из очереди, а также при истечении длительности последней анимации (когда
     * очередь пуста — «замороженный хвост»). Колбэк вызывается после обновления состояния,
     * поэтому из него можно вызывать {@code play}/{@code queue}.
     */
    public void tick() {
        long now = timeSupplier.getAsLong();
        while (current != null && !pending.isEmpty() && now > animationStartTime + (long) current.getDuration()) {
            mergeIntoSnapshot(current, (long) current.getDuration());
            animationStartTime += (long) current.getDuration();

            Runnable finishedCb = currentCallback;

            PendingEntry next = pending.remove();
            current = next.animation();
            currentCallback = next.onEnd();

            if (finishedCb != null) finishedCb.run();
        }

        if (current != null && currentCallback != null
                && now > animationStartTime + (long) current.getDuration()) {
            Runnable cb = currentCallback;
            currentCallback = null;
            cb.run();
        }

        this.tickedTime = now;
    }

    /**
     * Немедленно запускает анимацию, заменяя текущую и сбрасывая очередь.
     * Текущие значения параметров сохраняются в снимок перед заменой.
     *
     * <p>Если предыдущая анимация была запущена с колбэком, он <b>не</b> вызывается —
     * это считается отменой.
     */
    public void play(Animation animation) {
        play(animation, null);
    }

    /**
     * То же, что {@link #play(Animation)}, но с колбэком на естественное завершение.
     *
     * <p>Колбэк {@code onEnd} вызывается ровно один раз из {@link #tick()}, когда
     * длительность {@code animation} истекла (либо при переходе на следующую анимацию из
     * очереди, либо когда очередь пуста и пройдено время «замороженного хвоста»).
     *
     * <p>Колбэк <b>не</b> срабатывает, если анимация была отменена: вытеснена другим
     * {@link #play}, или прервана через {@link #stop} / {@link #clear}.
     *
     * @param animation анимация для воспроизведения
     * @param onEnd     колбэк естественного завершения; {@code null} — без колбэка
     */
    public void play(Animation animation, @Nullable Runnable onEnd) {
        long now = timeSupplier.getAsLong();
        if (current != null) {
            mergeIntoSnapshot(current, now - animationStartTime);
        }
        this.current = animation;
        this.currentCallback = onEnd;
        this.pending.clear();
        this.animationStartTime = now;
    }

    /**
     * Добавляет анимацию в очередь — она начнётся после завершения текущих.
     * Если аниматор простаивает ({@link #isIdle}), начинает анимацию немедленно.
     */
    public void queue(Animation animation) {
        queue(animation, null);
    }

    /**
     * То же, что {@link #queue(Animation)}, но с колбэком на естественное завершение.
     *
     * <p>Колбэк {@code onEnd} вызывается ровно один раз из {@link #tick()}, когда
     * именно эта анимация естественно завершит своё время. Если на момент вызова
     * аниматор простаивает, поведение эквивалентно {@link #play(Animation, Runnable)}.
     *
     * <p>Колбэк <b>не</b> срабатывает, если анимация была отменена через {@link #play},
     * {@link #stop} или {@link #clear} до её естественного завершения.
     *
     * @param animation анимация для добавления в очередь
     * @param onEnd     колбэк естественного завершения; {@code null} — без колбэка
     */
    public void queue(Animation animation, @Nullable Runnable onEnd) {
        if (isIdle()) {
            play(animation, onEnd);
        } else {
            this.pending.add(new PendingEntry(animation, onEnd));
        }
    }

    /**
     * Останавливает воспроизведение, сохраняя текущие значения параметров в снимок.
     * После вызова {@link #getParameter} будет возвращать значения из снимка.
     *
     * <p>Колбэки {@code onEnd} текущей и поставленных в очередь анимаций <b>не</b> вызываются —
     * это считается отменой.
     */
    public void stop() {
        long now = timeSupplier.getAsLong();
        if (current != null) {
            mergeIntoSnapshot(current, now - animationStartTime);
        }
        this.current = null;
        this.currentCallback = null;
        this.pending.clear();
    }

    /**
     * Полностью сбрасывает аниматор, включая снимок.
     * После вызова все {@link #getParameter} будут возвращать {@code empty}.
     *
     * <p>Колбэки {@code onEnd} текущей и поставленных в очередь анимаций <b>не</b> вызываются —
     * это считается отменой.
     */
    public void clear() {
        this.current = null;
        this.currentCallback = null;
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
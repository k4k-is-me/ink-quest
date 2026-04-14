package k4k.travelcorequesting.client.animation;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Описание анимации — набор именованных параметров, каждый из которых
 * интерполируется по своей функции {@link ParameterAnimation}.
 *
 * <p>Анимация сама по себе не хранит текущее время и не «играет» —
 * она только описывает что и как анимировать. Воспроизведение берёт на себя {@link Animator}.
 *
 * <h2>Создание анимации</h2>
 * <pre>{@code
 * static final ParameterKey<Float>   OPACITY  = new ParameterKey<>(Float.class,   0f);
 * static final ParameterKey<Integer> POSITION = new ParameterKey<>(Integer.class, 0);
 *
 * Animation myAnimation = new Animation.Builder()
 *     .addParameter(OPACITY,  ParameterAnimations.fadeIn(),    0,   500)
 *     .addParameter(POSITION, ParameterAnimations.slideIn(-10), 0,  300)
 *     .build();
 * }</pre>
 *
 * <p>Общая длительность анимации автоматически устанавливается равной
 * максимальному значению {@code delay + duration} среди всех параметров.
 * Можно задать вручную через {@link Builder#setDuration}.
 *
 * <h2>Получение значения параметра</h2>
 * <pre>{@code
 * Optional<Float> opacity = animation.getParameter(OPACITY, relativeTime);
 * }</pre>
 * Обратите внимание: {@code time} здесь — время относительно начала анимации (не абсолютное).
 * Абсолютное время переводит в относительное {@link Animator}.
 *
 * <h2>Fill modes (режимы воспроизведения)</h2>
 * Fill mode определяет как абсолютное время переводится в {@code t ∈ [0, 1]}:
 * <ul>
 *   <li>{@link #ONE_TIME} — анимация проигрывается один раз. После завершения {@code t = 1.0}.
 *   <li>{@link #LOOP} — анимация зацикливается. {@code t} постоянно идёт от 0 к 1.
 *   <li>{@link #ANIMATION_LOOP} — то же что LOOP, но синхронизировано со временем мира
 *       (все объекты с одинаковой длительностью будут в одной фазе).
 * </ul>
 */
public class Animation {

    /**
     * Одноразовое воспроизведение. После завершения {@code t} застывает на {@code 1.0}.
     * Используется для большинства UI-анимаций: fade-in, slide-in, bop и т.д.
     */
    public static final AnimationFillMode ONE_TIME = (currentTime, sqDuration, anStartTime, anDuration) ->
            clamp01((currentTime - anStartTime) / anDuration);

    /**
     * Зацикливание параметра относительно <em>общей</em> длительности анимации ({@code sqDuration}).
     * Параметр проигрывается внутри каждого цикла общей анимации:
     * {@code t = 0} пока не наступит {@code delay}, {@code t = 1} после завершения параметра,
     * затем с началом нового цикла общей анимации — всё повторяется.
     * Полезно для синхронных пульсаций, где несколько параметров должны циклиться в одном ритме.
     */
    public static final AnimationFillMode ANIMATION_LOOP = (currentTime, sqDuration, anStartTime, anDuration) ->
            clamp01((mod1(currentTime / sqDuration) * sqDuration - anStartTime) / anDuration);

    /**
     * Зацикливание параметра относительно <em>себя</em>: как только параметр завершился —
     * он начинается заново, независимо от других параметров и общей длительности анимации.
     * {@code t} непрерывно идёт от {@code 0} к {@code 1} и повторяется.
     */
    public static final AnimationFillMode LOOP = (currentTime, sqDuration, anStartTime, anDuration) ->
            mod1((currentTime - anStartTime) / anDuration);

    private final Map<ParameterKey<?>, ParameterDefinition<?>> animations;
    private final double duration;

    private Animation(Map<ParameterKey<?>, ParameterDefinition<?>> animations, double duration) {
        this.animations = animations;
        this.duration = duration;
    }

    /**
     * Возвращает текущее значение именованного параметра.
     *
     * @param key     ключ параметра, заданный при создании через {@link Builder}
     * @param initial значение параметра в момент начала анимации (из снимка или дефолт ключа)
     * @param time    время относительно начала анимации (мс)
     * @return значение параметра, или {@code empty} если параметр не найден
     */
    public <T> Optional<T> getParameter(ParameterKey<T> key, T initial, long time) {
        ParameterDefinition<?> def = animations.get(key);
        if (def == null) return Optional.empty();

        @SuppressWarnings("unchecked")
        ParameterDefinition<T> typedDef = (ParameterDefinition<T>) def;

        var t = typedDef.fillMode.apply(time, this.duration, typedDef.delay, typedDef.duration);
        return Optional.ofNullable(typedDef.animation.animate(initial, t));
    }

    public float getDuration() {
        return (float) this.duration;
    }

    /**
     * Возвращает имена всех параметров, определённых в этой анимации.
     */
    public Set<ParameterKey<?>> getParameterKeys() {
        return Collections.unmodifiableSet(animations.keySet());
    }

    private static double mod1(double a) {
        return a - Math.floor(a);
    }

    private static double clamp01(double a) {
        return Math.max(0, Math.min(a, 1));
    }

    private record ParameterDefinition<T>(
        ParameterAnimation<T> animation,
        AnimationFillMode fillMode,
        long duration,
        long delay
    ) {}

    /**
     * Определяет как реальное время переводится в {@code t ∈ [0, 1]} для параметра анимации.
     *
     * @see Animation#ONE_TIME
     * @see Animation#LOOP
     * @see Animation#ANIMATION_LOOP
     */
    @FunctionalInterface
    public interface AnimationFillMode {
        /**
         * @param currentTime  время с момента начала анимации (мс)
         * @param sqDuration   общая длительность всей анимации (мс)
         * @param anStartTime  задержка (delay) данного параметра относительно начала анимации (мс)
         * @param anDuration   длительность данного параметра (мс)
         * @return {@code t ∈ [0, 1]}
         */
        double apply(double currentTime, double sqDuration, double anStartTime, double anDuration);
    }

    /**
     * Строитель анимации.
     *
     * <h2>Сигнатура addParameter</h2>
     * <pre>{@code
     * // ONE_TIME по умолчанию:
     * .addParameter(KEY, animation, delayMs, durationMs)
     * // Явный fill mode (в конце):
     * .addParameter(KEY, animation, delayMs, durationMs, fillMode)
     * }</pre>
     *
     * <h2>Пример с задержкой</h2>
     * <pre>{@code
     * static final ParameterKey<Integer> ICON_U   = new ParameterKey<>(Integer.class, 0);
     * static final ParameterKey<Integer> POSITION = new ParameterKey<>(Integer.class, 0);
     *
     * new Animation.Builder()
     *     // Иконка переключается в момент t=250мс (delay=250, duration=1)
     *     .addParameter(ICON_U,   ParameterAnimations.switchTo(16), 250, 1)
     *     // Позиция анимируется сразу 500мс (delay=0, duration=500)
     *     .addParameter(POSITION, ParameterAnimations.slideIn(-10),  0,  500)
     *     .build();
     * }</pre>
     */
    public static class Builder {
        private final Map<ParameterKey<?>, ParameterDefinition<?>> animations = new ConcurrentHashMap<>();
        private double duration = 1;
        private boolean isExplicitDuration = false;

        /**
         * Добавляет параметр анимации.
         */
        public <T> Builder addParameter(ParameterKey<T> key, ParameterAnimation<T> animation, long delay, long duration, AnimationFillMode fillMode) {
            if (duration <= 0) throw new IllegalArgumentException("Duration must be greater than zero");
            animations.put(key, new ParameterDefinition<>(animation, fillMode, duration, delay));
            if (!this.isExplicitDuration && duration > this.duration) this.duration = duration;
            return this;
        }

        /**
         * Добавляет параметр анимации.
         */
        public <T> Builder addParameter(ParameterKey<T> key, ParameterAnimation<T> animation, long delay, long duration) {
            if (duration <= 0) throw new IllegalArgumentException("Duration must be greater than zero");
            animations.put(key, new ParameterDefinition<>(animation, ONE_TIME, duration, delay));
            if (!this.isExplicitDuration && duration > this.duration) this.duration = duration;
            return this;
        }

        /**
         * Задаёт общую длительность анимации вручную.
         * По умолчанию она равна максимуму {@code delay + duration} среди всех параметров.
         * Актуально для {@link #ANIMATION_LOOP}: длительность влияет на период синхронизации.
         */
        public Builder setDuration(long duration) {
            if (duration <= 0) throw new IllegalArgumentException("Duration must be greater than zero");
            this.duration = duration;
            this.isExplicitDuration = true;
            return this;
        }

        public Animation build() {
            return new Animation(this.animations, this.duration);
        }
    }
}
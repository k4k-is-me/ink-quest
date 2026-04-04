package k4k.travelcorequesting.common.animation;

import java.util.Map;
import java.util.Optional;
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
 * Animation myAnimation = new Animation.Builder()
 *     .addParameterAnimation("Opacity", FadeParameterAnimation.fadeIn(), Animation.ONE_TIME, 500, Float.class)
 *     .addParameterAnimation("Position", SlideParameterAnimation.slideIn(-10, 0), Animation.ONE_TIME, 300, Vector2d.class)
 *     .build();
 * }</pre>
 *
 * <p>Общая длительность анимации автоматически устанавливается равной
 * максимальному значению {@code delay + duration} среди всех параметров.
 * Можно задать вручную через {@link Builder#setDuration}.
 *
 * <h2>Получение значения параметра</h2>
 * <pre>{@code
 * Optional<Float> opacity = animation.getParameter("Opacity", relativeTime, Float.class);
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

    private final Map<String, AnimationDefinition<?>> animations;
    private final double duration;

    private Animation(Map<String, AnimationDefinition<?>> animations, double duration) {
        this.animations = animations;
        this.duration = duration;
    }

    /**
     * Возвращает текущее значение именованного параметра.
     *
     * @param parameterKey имя параметра, заданное при создании через {@link Builder}
     * @param time         время относительно начала анимации (мс)
     * @param expectedType ожидаемый тип значения
     * @return значение параметра, или {@code empty} если параметр не найден или тип не совпадает
     */
    public <T> Optional<T> getParameter(String parameterKey, long time, Class<T> expectedType) {
        AnimationDefinition<?> def = animations.get(parameterKey);
        if (def == null || !expectedType.isAssignableFrom(def.type)) {
            return Optional.empty();
        }

        @SuppressWarnings("unchecked")
        AnimationDefinition<T> typedDef = (AnimationDefinition<T>) def;

        var t = typedDef.fillMode.apply(time, this.duration, typedDef.delay, typedDef.duration);
        return Optional.ofNullable(typedDef.animation.animate(t));
    }

    public float getDuration() {
        return (float) this.duration;
    }

    private static double mod1(double a) {
        return a - Math.floor(a);
    }

    private static double clamp01(double a) {
        return Math.max(0, Math.min(a, 1));
    }

    private record AnimationDefinition<T>(
        ParameterAnimation<T> animation,
        AnimationFillMode fillMode,
        long duration,
        long delay,
        Class<T> type
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
     * <h2>Сигнатуры addParameterAnimation</h2>
     * <pre>{@code
     * // Без задержки:
     * .addParameterAnimation("Key", animation, fillMode, durationMs, ValueType.class)
     *
     * // С задержкой (параметр начнёт анимироваться через delayMs после старта анимации):
     * .addParameterAnimation("Key", animation, fillMode, durationMs, delayMs, ValueType.class)
     * }</pre>
     *
     * <h2>Пример с задержкой</h2>
     * <pre>{@code
     * new Animation.Builder()
     *     // Иконка переключается в момент t=250мс
     *     .addParameterAnimation("IconU", SwitchValueParameterAnimation.switchTo(16),
     *         Animation.ONE_TIME, 1, 250, Integer.class)
     *     // Позиция анимируется сразу, 500мс
     *     .addParameterAnimation("Position", SlideParameterAnimation.slideIn(-10, 0),
     *         Animation.ONE_TIME, 500, Vector2d.class)
     *     .build();
     * }</pre>
     */
    public static class Builder {
        private final Map<String, AnimationDefinition<?>> animations = new ConcurrentHashMap<>();
        private double duration = 1;
        private boolean isExplicitDuration = false;

        /**
         * Добавляет параметр анимации без задержки.
         *
         * @param parameterKey имя параметра (используется при вызове {@link Animator#getParameter})
         * @param animation    функция интерполяции
         * @param fillMode     режим воспроизведения ({@link #ONE_TIME}, {@link #LOOP}, {@link #ANIMATION_LOOP})
         * @param duration     длительность в мс
         * @param type         класс типа значения (нужен для типобезопасного извлечения)
         */
        public <T> Builder addParameterAnimation(String parameterKey, ParameterAnimation<T> animation, AnimationFillMode fillMode, long duration, Class<T> type) {
            if (duration <= 0) throw new IllegalArgumentException("Duration must be greater than zero");
            animations.put(parameterKey, new AnimationDefinition<>(animation, fillMode, duration, 0, type));
            if (!this.isExplicitDuration && duration > this.duration) this.duration = duration;
            return this;
        }

        /**
         * Добавляет параметр анимации с задержкой.
         *
         * @param delay задержка в мс — через сколько после старта анимации начнёт анимироваться этот параметр
         */
        public <T> Builder addParameterAnimation(String parameterKey, ParameterAnimation<T> animation, AnimationFillMode fillMode, long duration, long delay, Class<T> type) {
            if (duration <= 0) throw new IllegalArgumentException("Duration must be greater than zero");
            animations.put(parameterKey, new AnimationDefinition<>(animation, fillMode, duration, delay, type));
            if (!this.isExplicitDuration && delay + duration > this.duration) this.duration = delay + duration;
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
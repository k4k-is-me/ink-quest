package k4k.travelcorequesting.common.animation;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class Animation {
    public static final AnimationFillMode ONE_TIME = (currentTime, sqDuration, anStartTime, anDuration) ->
            clamp01((currentTime - anStartTime) / anDuration);
    public static final AnimationFillMode SYNCED_LOOP = (currentTime, sqDuration, anStartTime, anDuration) ->
            clamp01((mod1(currentTime / sqDuration) * sqDuration - anStartTime) / anDuration);
    public static final AnimationFillMode LOOP = (currentTime, sqDuration, anStartTime, anDuration) ->
            mod1((currentTime - anStartTime) / anDuration);

    private final Map<String, AnimationDefinition<?>> animations;
    private final double duration;

    private Animation(Map<String, AnimationDefinition<?>> animations, double duration) {
        this.animations = animations;
        this.duration = duration;
    }

    public <T> Optional<T> getParameter(String parameterKey, long time, Class<T> expectedType) {
        AnimationDefinition<?> def = animations.get(parameterKey);
        if (def == null || !expectedType.isAssignableFrom(def.type)) {
            return Optional.empty(); // Нет такой анимации
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

    private record AnimationDefinition<T> (
        ParameterAnimation<T> animation,
        AnimationFillMode fillMode,
        long duration,
        long delay,
        Class<T> type
    ) {}

    @FunctionalInterface
    public interface AnimationFillMode {
        /**
         * Насчитывает параметр t [0, 1) для отправки в анимацию
         * @param currentTime Текущее время относительно начала анимации
         * @param sqDuration общая длительность анимации
         * @param anStartTime время начала анимации параметра относительно начала анимации
         * @param anDuration длительность анимации параметра
         * @return Значение в диапазоне [0, 1)
         */
        double apply(double currentTime, double sqDuration, double anStartTime, double anDuration);
    }

    public static class Builder {
        private final Map<String, AnimationDefinition<?>> animations = new ConcurrentHashMap<>();
        private double duration = 1;
        private boolean isExplicitDuration = false;

        public <T> Builder addParameterAnimation(String parameterKey, ParameterAnimation<T> animation, AnimationFillMode fillMode, long duration, Class<T> type) {
            if (duration <= 0) throw new IllegalArgumentException("Duration must be greater than zero");
            animations.put(parameterKey, new AnimationDefinition<>(animation, fillMode, duration, 0, type));
            if (!this.isExplicitDuration && duration > this.duration) this.duration = duration;
            return this;
        }

        public <T> Builder addParameterAnimation(String parameterKey, ParameterAnimation<T> animation, AnimationFillMode fillMode, long duration, long delay, Class<T> type) {
            if (duration <= 0) throw new IllegalArgumentException("Duration must be greater than zero");
            animations.put(parameterKey, new AnimationDefinition<>(animation, fillMode, duration, delay, type));
            if (!this.isExplicitDuration && delay + duration > this.duration) this.duration = delay + duration;
            return this;
        }

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

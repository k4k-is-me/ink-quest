package k4k.travelcorequesting.common.animation;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;

public class Animator {
    private final Queue<Animation> animationQueue = new ArrayDeque<>();
    private long animationStartTime;

//    private @Nullable PlayedAnimation currentlyPlayedAnimation = null;

    public void play(Animation animation, long startTime) {
        this.animationQueue.clear();
        this.animationQueue.add(animation);
        this.animationStartTime = startTime;
//        this.currentlyPlayedAnimation = new PlayedAnimation(animation, startTime);
    }

    public void queue(Animation animation) {
        this.animationQueue.add(animation);
    }

    public void stop() {
        this.animationQueue.clear();
    }

    private Optional<Animation> getPlayedAnimation(long currentTime) {
        // NOTE: Перестаём извлекать элементы если остался один, чтобы последняя анимация продолжала проигрываться
        while (animationQueue.size() > 1 && currentTime > this.animationStartTime + (long) this.animationQueue.peek().getDuration()) {
            this.animationStartTime += (long) Objects.requireNonNull(this.animationQueue.poll()).getDuration();
        }

        if (animationQueue.isEmpty())
            return Optional.empty();

        return Optional.of(animationQueue.peek());
    }

    public <T> Optional<T> getParameter(String parameterKey, long currentTime, Class<T> expectedType) {
        return this.getPlayedAnimation(currentTime)
                .flatMap(animation -> animation
                        .getParameter(parameterKey, currentTime - animationStartTime, expectedType));
    }

    public <T> T getParameterOrDefault(String parameterKey, long currentTime, T defaultValue, Class<T> expectedType) {
        return this.getParameter(parameterKey, currentTime, expectedType).orElse(defaultValue);
    }

//    private record PlayedAnimation (
//            Animation animation,
//            Long startTime
//    ) {}
}

package k4k.travelcorequesting.common.animation;

@FunctionalInterface
public interface ParameterAnimation<T> {
    T animate(double t);
}

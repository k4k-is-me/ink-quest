package k4k.travelcorequesting.common.animation;

public class ParameterAnimations {
    /** Въезжает из смещения {@code dv} к 0. Игнорирует initial. */
    public static ParameterAnimation<Integer> slideIn(int dv) {
        return (initial, t) -> (int) (dv * (1 - t));
    }

    /** Въезжает из смещения {@code dv} к 0. Игнорирует initial. */
    public static ParameterAnimation<Float> slideIn(float dv) {
        return (initial, t) -> dv * (float) (1 - t);
    }

    /** Уезжает от 0 к смещению {@code dv}. Игнорирует initial. */
    public static ParameterAnimation<Integer> slideOut(int dv) {
        return (initial, t) -> (int) (dv * t);
    }

    /** Уезжает от 0 к смещению {@code dv}. Игнорирует initial. */
    public static ParameterAnimation<Float> slideOut(float dv) {
        return (initial, t) -> dv * (float) t;
    }

    /** Плавно переходит от {@code initial} к {@code target}. */
    public static ParameterAnimation<Integer> slideTo(int target) {
        return (initial, t) -> (int) (initial + (target - initial) * t);
    }

    /** Плавно переходит от {@code initial} к {@code target}. */
    public static ParameterAnimation<Float> slideTo(float target) {
        return (initial, t) -> (float) (initial + (target - initial) * t);
    }

    /** Отскок: 0 → dv → 0 по синусоиде. Игнорирует initial. */
    public static ParameterAnimation<Integer> bop(int dv) {
        return (initial, t) -> (int) (Math.sin(Math.PI * t) * dv);
    }

    /** Затухание от {@code initial} до 0. */
    public static ParameterAnimation<Float> fadeOut() {
        return (initial, t) -> initial * (float) (1.0 - t);
    }

    /** Появление от {@code initial} до 1. */
    public static ParameterAnimation<Float> fadeIn() {
        return (initial, t) -> initial + (1.0f - initial) * (float) t;
    }

    /** Мгновенное переключение на значение {@code to}. Игнорирует initial. */
    public static <T> ParameterAnimation<T> switchTo(T to) {
        return (initial, t) -> to;
    }
}
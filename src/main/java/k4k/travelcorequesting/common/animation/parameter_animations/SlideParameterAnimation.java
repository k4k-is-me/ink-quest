package k4k.travelcorequesting.common.animation.parameter_animations;

import k4k.travelcorequesting.common.animation.ParameterAnimation;
import org.joml.Vector2d;

/**
 * Линейное смещение позиции ({@code Vector2d}).
 *
 * <pre>{@code
 * // Въезд слева: смещение (-10, 0) → (0, 0)
 * SlideParameterAnimation.slideIn(-10, 0)
 *
 * // Выезд вправо: смещение (0, 0) → (10, 0)
 * SlideParameterAnimation.slideOut(10, 0)
 * }</pre>
 *
 * <p>Параметр имеет тип {@code Vector2d}, читать через:
 * <pre>{@code
 * int offsetX = (int) animator.getParameter("Position", t, Vector2d.class)
 *                             .orElseGet(Vector2d::new).x;
 * int drawX = x + offsetX;
 * }</pre>
 */
public class SlideParameterAnimation implements ParameterAnimation<Vector2d> {
    private final Vector2d vecFrom;
    private final Vector2d vecTo;

    private SlideParameterAnimation(Vector2d vecFrom, Vector2d vecTo) {
        this.vecFrom = vecFrom;
        this.vecTo = vecTo;
    }

    /**
     * Въезд из смещения {@code (dx, dy)} в исходную позицию {@code (0, 0)}.
     * Отрицательный {@code dx} — въезд слева.
     */
    public static SlideParameterAnimation slideIn(int dx, int dy) {
        return new SlideParameterAnimation(new Vector2d(dx, dy), new Vector2d(0, 0));
    }

    /**
     * Выезд из исходной позиции {@code (0, 0)} в смещение {@code (dx, dy)}.
     * Положительный {@code dx} — выезд вправо.
     */
    public static SlideParameterAnimation slideOut(int dx, int dy) {
        return new SlideParameterAnimation(new Vector2d(0, 0), new Vector2d(dx, dy));
    }

    @Override
    public Vector2d animate(double t) {
        return new Vector2d(this.vecFrom).lerp(this.vecTo, t);
    }
}
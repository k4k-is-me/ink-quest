package k4k.travelcorequesting.common.animation.parameter_animations;

import k4k.travelcorequesting.common.animation.ParameterAnimation;
import org.joml.Vector2d;

/**
 * Отскок по синусоиде ({@code Vector2d}): позиция уходит в сторону и возвращается обратно.
 *
 * <p>Траектория: {@code (0,0) → (dx,dy) → (0,0)} по кривой {@code sin(π·t)}.
 * Начальная и конечная позиция совпадают.
 *
 * <pre>{@code
 * // Отскок вправо на 2px
 * BopParameterAnimation.bopRight(2)
 *
 * // Отскок влево на 3px
 * BopParameterAnimation.bopLeft(3)
 * }</pre>
 *
 * <p>Читать так же, как {@link SlideParameterAnimation}:
 * <pre>{@code
 * int offsetX = (int) animator.getParameter("Position", t, Vector2d.class)
 *                             .orElseGet(Vector2d::new).x;
 * }</pre>
 *
 * <p>Используется для анимации завершения задачи (bop при success/failure/skip).
 */
public class BopParameterAnimation implements ParameterAnimation<Vector2d> {
    private final float dx;
    private final float dy;

    private BopParameterAnimation(float dx, float dy) {
        this.dx = dx;
        this.dy = dy;
    }

    /** Отскок вправо */
    public static BopParameterAnimation bopRight(float amount) {
        return new BopParameterAnimation(amount, 0);
    }

    /** Отскок влево */
    public static BopParameterAnimation bopLeft(float amount) {
        return new BopParameterAnimation(-amount, 0);
    }

    /** Отскок вниз */
    public static BopParameterAnimation bopDown(float amount) {
        return new BopParameterAnimation(0, amount);
    }

    /** Отскок вверх */
    public static BopParameterAnimation bopUp(float amount) {
        return new BopParameterAnimation(0, -amount);
    }

    @Override
    public Vector2d animate(double t) {
        var s = Math.sin(Math.PI * t);
        return new Vector2d(s * dx, s * dy);
    }
}
package k4k.travelcorequesting.common.animation.parameter_animations;

import k4k.travelcorequesting.common.animation.ParameterAnimation;
import org.joml.Vector2d;

public class BopParameterAnimation implements ParameterAnimation<Vector2d> {
    private final float dx;
    private final float dy;

    private BopParameterAnimation(float dx, float dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public static BopParameterAnimation bopRight(float amount) {
        return new BopParameterAnimation(amount, 0);
    }

    public static BopParameterAnimation bopLeft(float amount) {
        return new BopParameterAnimation(-amount, 0);
    }

    public static BopParameterAnimation bopDown(float amount) {
        return new BopParameterAnimation(amount, 0);
    }

    public static BopParameterAnimation bopUp(float amount) {
        return new BopParameterAnimation(-amount, 0);
    }

    @Override
    public Vector2d animate(double t) {
        var s = Math.sin(Math.PI * t);
        return new Vector2d(s * dx, s * dy);
    }
}

package k4k.travelcorequesting.common.animation.parameter_animations;

import k4k.travelcorequesting.common.animation.ParameterAnimation;
import org.joml.Vector2d;

public class SlideParameterAnimation implements ParameterAnimation<Vector2d> {
    private final Vector2d vecFrom;
    private final Vector2d vecTo;

    private SlideParameterAnimation(Vector2d vecFrom, Vector2d vecTo) {
        this.vecFrom = vecFrom;
        this.vecTo = vecTo;
    }

    public static SlideParameterAnimation slideIn(int dx, int dy) {
        return new SlideParameterAnimation(new Vector2d(dx, dy), new Vector2d(0, 0));
    }

    public static SlideParameterAnimation slideOut(int dx, int dy) {
        return new SlideParameterAnimation(new Vector2d(0, 0), new Vector2d(dx, dy));
    }

    @Override
    public Vector2d animate(double t) {
        return new Vector2d(this.vecFrom).lerp(this.vecTo, t);
    }
}

package k4k.travelcorequesting.client.animation;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для {@link Animation}: интерполяция параметров, fill modes, delay, duration.
 */
class AnimationTest {

    private static final ParameterKey<Float> OPACITY = new ParameterKey<>(Float.class, 0f);
    private static final ParameterKey<Integer> POSITION = new ParameterKey<>(Integer.class, 0);

    // --- getParameter ---

    @Test
    void getParameter_unknownKey_returnsEmpty() {
        Animation anim = new Animation.Builder()
                .addParameter(OPACITY, ParameterAnimations.fadeIn(), 0, 500)
                .build();

        Optional<Integer> result = anim.getParameter(POSITION, 0, 250);
        assertTrue(result.isEmpty());
    }

    @Test
    void getParameter_atStart_returnsInitialInfluencedValue() {
        // fadeIn: initial + (1 - initial) * t. При t=0 → initial (0f по умолчанию → 0f)
        Animation anim = new Animation.Builder()
                .addParameter(OPACITY, ParameterAnimations.fadeIn(), 0, 500)
                .build();

        float value = anim.getParameter(OPACITY, 0f, 0).orElseThrow();
        assertEquals(0f, value, 0.001f);
    }

    @Test
    void getParameter_atEnd_returnsFinalValue() {
        // fadeIn: t=1 → 1.0
        Animation anim = new Animation.Builder()
                .addParameter(OPACITY, ParameterAnimations.fadeIn(), 0, 500)
                .build();

        float value = anim.getParameter(OPACITY, 0f, 500).orElseThrow();
        assertEquals(1f, value, 0.001f);
    }

    @Test
    void getParameter_midway_returnsInterpolatedValue() {
        // slideIn(-10): dv*(1-t). При t=0.5 → -5
        Animation anim = new Animation.Builder()
                .addParameter(POSITION, ParameterAnimations.slideIn(-10), 0, 1000)
                .build();

        int value = anim.getParameter(POSITION, 0, 500).orElseThrow();
        assertEquals(-5, value);
    }

    // --- delay ---

    @Test
    void delay_beforeDelay_parameterAtT0() {
        // Параметр стартует в delay=300мс. До этого момента t=0
        Animation anim = new Animation.Builder()
                .addParameter(OPACITY, ParameterAnimations.fadeIn(), 300, 500)
                .build();

        // t=0 при time < delay
        float value = anim.getParameter(OPACITY, 0f, 100).orElseThrow();
        assertEquals(0f, value, 0.001f);
    }

    @Test
    void delay_afterDelay_parameterProgresses() {
        // delay=300, duration=500. После delay=300 начинается анимация.
        // time=800 → elapsed внутри = 500 → t=1 → fadeIn = 1
        Animation anim = new Animation.Builder()
                .addParameter(OPACITY, ParameterAnimations.fadeIn(), 300, 500)
                .build();

        float value = anim.getParameter(OPACITY, 0f, 800).orElseThrow();
        assertEquals(1f, value, 0.001f);
    }

    // --- getDuration ---

    @Test
    void getDuration_autoComputed_isMaxDelayPlusDuration() {
        Animation anim = new Animation.Builder()
                .addParameter(OPACITY, ParameterAnimations.fadeIn(), 0, 300)
                .addParameter(POSITION, ParameterAnimations.slideIn(-5), 200, 400)  // ends at 600
                .build();

        assertEquals(600f, anim.getDuration(), 0.001f);
    }

    @Test
    void getDuration_explicit_overridesAuto() {
        Animation anim = new Animation.Builder()
                .addParameter(OPACITY, ParameterAnimations.fadeIn(), 0, 300)
                .setDuration(1000)
                .build();

        assertEquals(1000f, anim.getDuration(), 0.001f);
    }

    @Test
    void builder_zeroDuration_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                new Animation.Builder().addParameter(OPACITY, ParameterAnimations.fadeIn(), 0, 0)
        );
    }

    // --- ONE_TIME fill mode ---

    @Test
    void fillMode_oneTime_afterEnd_tFrozenAt1() {
        // slideIn(-10): при t=1 → 0. Время > duration — должно вернуть 0
        Animation anim = new Animation.Builder()
                .addParameter(POSITION, ParameterAnimations.slideIn(-10), 0, 500)
                .build();

        int value = anim.getParameter(POSITION, 0, 9999).orElseThrow();
        assertEquals(0, value);
    }

    // --- LOOP vs ANIMATION_LOOP ---
    //
    // Общая установка: animation duration=1000мс, parameter duration=500мс.
    // slideIn(-10): t=0 → -10, t=0.5 → -5, t=1 → 0.
    //
    // LOOP циклится по длительности самого параметра (500мс):
    //   t=0   → цикл 1, t_p=0   → -10
    //   t=250 → цикл 1, t_p=0.5 → -5
    //   t=500 → цикл 2, t_p=0   → -10  ← новый цикл параметра
    //   t=750 → цикл 2, t_p=0.5 → -5
    //
    // ANIMATION_LOOP циклится по длительности всей анимации (1000мс):
    //   t=0   → t_p=0   → -10
    //   t=250 → t_p=0.5 → -5
    //   t=500 → t_p=1.0 → 0   ← параметр завершён, заморожен до конца цикла анимации
    //   t=750 → t_p=1.0 → 0   ← всё ещё заморожен
    //   t=1000 → новый цикл анимации, t_p=0 → -10

    @Test
    void fillMode_loop_cyclesEveryParameterDuration() {
        // Через 500мс (= duration параметра) LOOP начинает новый цикл
        Animation anim = new Animation.Builder()
                .setDuration(1000)
                .addParameter(POSITION, ParameterAnimations.slideIn(-10), 0, 500, Animation.LOOP)
                .build();

        int atStart     = anim.getParameter(POSITION, 0, 0).orElseThrow();    // t_p=0   → -10
        int atHalf      = anim.getParameter(POSITION, 0, 250).orElseThrow();  // t_p=0.5 → -5
        int atNewCycle  = anim.getParameter(POSITION, 0, 500).orElseThrow();  // t_p=0 снова → -10
        int atHalfAgain = anim.getParameter(POSITION, 0, 750).orElseThrow();  // t_p=0.5 → -5

        assertEquals(-10, atStart);
        assertEquals(-5,  atHalf);
        assertEquals(-10, atNewCycle,  "LOOP должен сбросить t в 0 через 500мс");
        assertEquals(atHalf, atHalfAgain);
    }

    @Test
    void fillMode_animationLoop_frozenAfterParameterEnd_resetsWithAnimation() {
        // ANIMATION_LOOP: параметр длится первую половину цикла анимации,
        // затем заморожен на t=1 вплоть до начала следующего цикла анимации (1000мс)
        Animation anim = new Animation.Builder()
                .setDuration(1000)
                .addParameter(POSITION, ParameterAnimations.slideIn(-10), 0, 500, Animation.ANIMATION_LOOP)
                .build();

        int atHalf       = anim.getParameter(POSITION, 0, 500).orElseThrow();   // t_p=1 → 0
        int atThreeQuart = anim.getParameter(POSITION, 0, 750).orElseThrow();   // t_p=1 → 0, заморожен
        int atNewCycle   = anim.getParameter(POSITION, 0, 1000).orElseThrow();  // новый цикл → -10

        assertEquals(0,   atHalf,       "параметр должен завершиться (t=1) к середине цикла анимации");
        assertEquals(0,   atThreeQuart, "параметр заморожен до конца цикла анимации");
        assertEquals(-10, atNewCycle,   "ANIMATION_LOOP сбрасывает t только на границе цикла анимации");
    }

    @Test
    void fillMode_loop_vs_animationLoop_differAtHalfAnimationDuration() {
        // Ключевое различие: в момент t=500 (середина анимации, конец параметра)
        // LOOP уже начал новый цикл параметра → t_p=0
        // ANIMATION_LOOP только завершил параметр → t_p=1
        Animation loopAnim = new Animation.Builder()
                .setDuration(1000)
                .addParameter(POSITION, ParameterAnimations.slideIn(-10), 0, 500, Animation.LOOP)
                .build();
        Animation animLoopAnim = new Animation.Builder()
                .setDuration(1000)
                .addParameter(POSITION, ParameterAnimations.slideIn(-10), 0, 500, Animation.ANIMATION_LOOP)
                .build();

        int loopAt500     = loopAnim.getParameter(POSITION, 0, 500).orElseThrow();
        int animLoopAt500 = animLoopAnim.getParameter(POSITION, 0, 500).orElseThrow();

        assertEquals(-10, loopAt500,     "LOOP: новый цикл параметра, t_p=0 → -10");
        assertEquals(0,   animLoopAt500, "ANIMATION_LOOP: конец параметра, t_p=1 → 0");
    }
}

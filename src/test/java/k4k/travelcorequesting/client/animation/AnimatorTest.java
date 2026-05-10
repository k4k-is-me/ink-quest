package k4k.travelcorequesting.client.animation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для {@link Animator}: play/queue/stop/clear, snapshot, isIdle, очередь.
 */
class AnimatorTest {

    private static final ParameterKey<Float> OPACITY = new ParameterKey<>(Float.class, 0f);
    private static final ParameterKey<Integer> OFFSET = new ParameterKey<>(Integer.class, 0);

    /** Управляемые часы для тестов. */
    private long now;
    private Animator animator;

    @BeforeEach
    void setUp() {
        now = 0L;
        animator = new Animator(() -> now);
    }

    // --- Начальное состояние ---

    @Test
    void initial_getParameter_returnsDefault() {
        animator.tick();
        assertEquals(0f, animator.getParameter(OPACITY));
        assertEquals(0, animator.getParameter(OFFSET));
    }

    @Test
    void initial_isIdle_true() {
        assertTrue(animator.isIdle());
    }

    // --- play ---

    @Test
    void play_isIdle_false() {
        animator.play(fadeIn500());
        assertFalse(animator.isIdle());
    }

    @Test
    void play_atStart_opacityIsInitial() {
        animator.play(fadeIn500());
        animator.tick();
        // fadeIn: initial + (1-initial)*t. initial=0, t=0 → 0
        assertEquals(0f, animator.getParameter(OPACITY), 0.001f);
    }

    @Test
    void play_atEnd_opacityIsOne() {
        animator.play(fadeIn500());
        now = 500;
        animator.tick();
        assertEquals(1f, animator.getParameter(OPACITY), 0.001f);
    }

    @Test
    void play_replacesCurrentAnimation() {
        animator.play(fadeIn500());
        now = 250;
        animator.play(opacityStatic1());  // анимация с фиксированным opacity=1
        animator.tick();
        assertEquals(1f, animator.getParameter(OPACITY), 0.001f);
    }

    @Test
    void play_savesSnapshot() {
        // Проиграть половину fadeIn (opacity ~ 0.5), потом сменить анимацию
        animator.play(fadeIn500());
        now = 250;
        // Сохраняем снимок через play (текущее значение opacity сохраняется)
        animator.play(noOpAnimation());  // анимация без OPACITY параметра

        now = 300;
        animator.tick();
        // OPACITY не задан в noOpAnimation, поэтому берётся из снимка (~0.5)
        float snapshotValue = animator.getParameter(OPACITY);
        assertTrue(snapshotValue > 0.3f && snapshotValue < 0.7f,
                "Ожидалось значение из снимка ~0.5, получено: " + snapshotValue);
    }

    @Test
    void play_clearsQueue() {
        animator.play(fadeIn500());
        animator.queue(opacityStatic1());  // поставить в очередь
        animator.play(noOpAnimation());    // play должен сбросить очередь

        now = 600;  // после noOpAnimation (duration=1)
        animator.tick();
        // Если очередь сброшена — opacityStatic1 не запустится, OPACITY из снимка
        assertTrue(animator.isIdle());
    }

    // --- queue ---

    @Test
    void queue_whenIdle_startsImmediately() {
        assertTrue(animator.isIdle());
        animator.queue(fadeIn500());
        assertFalse(animator.isIdle());
    }

    @Test
    void queue_afterCurrent_executesInOrder() {
        Animation first = fadeIn500();           // 0..500: fadeIn
        Animation second = opacityStatic1();     // 500..: opacity=1 всегда

        animator.play(first);
        animator.queue(second);

        now = 500;
        animator.tick();
        // После 500мс first закончился, second запустился
        assertEquals(1f, animator.getParameter(OPACITY), 0.001f);
    }

    @Test
    void queue_tickTransitionsToNext() {
        // Убеждаемся, что tick() переключает анимацию при наступлении времени
        animator.play(fadeIn500());
        animator.queue(offsetSlideIn());

        now = 600;
        animator.tick();
        // После перехода должен идти offsetSlideIn, OPACITY — из снимка
        assertNotEquals(0, animator.getParameter(OFFSET));  // уже началось или началось
    }

    // --- isIdle ---

    @Test
    void isIdle_duringAnimation_false() {
        animator.play(fadeIn500());
        now = 250;
        assertFalse(animator.isIdle());
    }

    @Test
    void isIdle_afterAnimationEnd_true() {
        animator.play(fadeIn500());
        now = 500;
        assertTrue(animator.isIdle());
    }

    @Test
    void isIdle_withPending_false() {
        animator.play(fadeIn500());
        animator.queue(opacityStatic1());  // есть следующая в очереди
        now = 400;
        assertFalse(animator.isIdle());
    }

    // --- stop ---

    @Test
    void stop_savesSnapshot() {
        animator.play(fadeIn500());
        now = 500;
        animator.stop();

        animator.tick();
        // После stop — opacity зафиксирован в 1 из снимка
        assertEquals(1f, animator.getParameter(OPACITY), 0.001f);
    }

    @Test
    void stop_isIdle_true() {
        animator.play(fadeIn500());
        animator.stop();
        assertTrue(animator.isIdle());
    }

    // --- clear ---

    @Test
    void clear_resetsToDefault() {
        animator.play(fadeIn500());
        now = 500;
        animator.tick();
        animator.clear();

        animator.tick();
        // После clear снимок тоже очищен → дефолт
        assertEquals(0f, animator.getParameter(OPACITY));
    }

    @Test
    void clear_isIdle_true() {
        animator.play(fadeIn500());
        animator.clear();
        assertTrue(animator.isIdle());
    }

    // --- snapshot как initial для следующей анимации ---

    @Test
    void snapshot_usedAsInitialForNextAnimation() {
        // fadeIn стартует с initial=0, но если в снимке opacity=0.5,
        // следующий fadeIn должен стартовать с 0.5
        animator.play(fadeIn500());
        now = 250; // opacity ≈ 0.5
        animator.play(fadeIn500());  // snapshot сохранён, fadeIn стартует с ~0.5

        animator.tick();
        float startValue = animator.getParameter(OPACITY);
        assertTrue(startValue > 0.3f, "fadeIn должен начинать не с нуля: " + startValue);
    }

    // --- callbacks ---

    @Test
    void play_onEnd_firesAfterDuration() {
        boolean[] fired = {false};
        animator.play(fadeIn500(), () -> fired[0] = true);
        now = 600;
        animator.tick();
        assertTrue(fired[0], "колбэк должен сработать после истечения длительности");
    }

    @Test
    void play_onEnd_firesOnceOnly() {
        int[] fired = {0};
        animator.play(fadeIn500(), () -> fired[0]++);
        now = 600;
        animator.tick();
        now = 1500;
        animator.tick();
        assertEquals(1, fired[0], "колбэк должен сработать ровно один раз");
    }

    @Test
    void play_onEnd_notFiredBeforeDuration() {
        boolean[] fired = {false};
        animator.play(fadeIn500(), () -> fired[0] = true);
        now = 499;
        animator.tick();
        assertFalse(fired[0], "колбэк не должен сработать до истечения длительности");
    }

    @Test
    void play_onEnd_notFiredAtExactDuration() {
        // Граница строгая: now > animationStartTime + duration
        boolean[] fired = {false};
        animator.play(fadeIn500(), () -> fired[0] = true);
        now = 500;
        animator.tick();
        assertFalse(fired[0], "колбэк не должен сработать ровно в момент окончания (граница строгая)");
    }

    @Test
    void queue_onEnd_firesOnTransition() {
        boolean[] firedA = {false};
        boolean[] firedB = {false};
        animator.play(fadeIn500(), () -> firedA[0] = true);
        animator.queue(fadeIn500(), () -> firedB[0] = true);

        now = 501;
        animator.tick();
        assertTrue(firedA[0], "cbA должен сработать при переходе на B");
        assertFalse(firedB[0], "cbB не должен срабатывать пока B играет");

        now = 1500;
        animator.tick();
        assertTrue(firedB[0], "cbB должен сработать после истечения длительности B");
    }

    @Test
    void play_replacing_doesNotFireCallback() {
        boolean[] fired = {false};
        animator.play(fadeIn500(), () -> fired[0] = true);
        now = 200;
        animator.play(opacityStatic1());
        now = 600;
        animator.tick();
        assertFalse(fired[0], "колбэк отменённой анимации не должен срабатывать");
    }

    @Test
    void stop_doesNotFireCallback() {
        boolean[] fired = {false};
        animator.play(fadeIn500(), () -> fired[0] = true);
        now = 200;
        animator.stop();
        now = 600;
        animator.tick();
        assertFalse(fired[0], "stop не должен вызывать колбэк");
    }

    @Test
    void clear_doesNotFireCallback() {
        boolean[] fired = {false};
        animator.play(fadeIn500(), () -> fired[0] = true);
        now = 200;
        animator.clear();
        now = 600;
        animator.tick();
        assertFalse(fired[0], "clear не должен вызывать колбэк");
    }

    @Test
    void queue_whenIdle_callbackFiresOnEnd() {
        boolean[] fired = {false};
        animator.queue(fadeIn500(), () -> fired[0] = true);
        assertFalse(animator.isIdle(), "queue на idle-аниматоре должен запустить анимацию");
        now = 600;
        animator.tick();
        assertTrue(fired[0], "колбэк должен сработать после естественного окончания");
    }

    @Test
    void play_callbackCanReplayInsideCallback() {
        boolean[] aFired = {false};
        animator.play(fadeIn500(), () -> {
            aFired[0] = true;
            animator.play(opacityStatic1());
        });
        now = 600;
        animator.tick();
        assertTrue(aFired[0], "cbA должен сработать");
        // Внутри cbA запущен opacityStatic1 — он стал текущей анимацией
        assertEquals(1f, animator.getParameter(OPACITY), 0.001f,
                "после колбэка должна играть новая анимация (opacityStatic1)");
    }

    // --- Вспомогательные анимации ---

    /** fadeIn по OPACITY за 500мс. */
    private Animation fadeIn500() {
        return new Animation.Builder()
                .addParameter(OPACITY, ParameterAnimations.fadeIn(), 0, 500)
                .build();
    }

    /** Анимация с OPACITY=1 на 1мс. */
    private Animation opacityStatic1() {
        return new Animation.Builder()
                .addParameter(OPACITY, ParameterAnimations.switchTo(1f), 0, 1)
                .build();
    }

    /** Анимация без каких-либо параметров (только duration). */
    private Animation noOpAnimation() {
        return new Animation.Builder()
                .addParameter(OFFSET, ParameterAnimations.switchTo(0), 0, 1)
                .setDuration(1)
                .build();
    }

    /** slideIn(-10) по OFFSET за 500мс. */
    private Animation offsetSlideIn() {
        return new Animation.Builder()
                .addParameter(OFFSET, ParameterAnimations.slideIn(-10), 0, 500)
                .build();
    }
}
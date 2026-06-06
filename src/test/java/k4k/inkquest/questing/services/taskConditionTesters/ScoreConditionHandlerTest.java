package k4k.inkquest.questing.services.taskConditionTesters;

import k4k.inkquest.domain.models.taskConditions.ScoreCondition;
import net.minecraft.scoreboard.ScoreboardCriterion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScoreConditionHandlerTest {
    private ScoreConditionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ScoreConditionHandler();
    }

    /** Восходящее условие: from=0, to=target, reset=true (дефолт). */
    private static ScoreCondition score(String objective, int to) {
        return new ScoreCondition(objective, ScoreboardCriterion.DUMMY, 0, to, true);
    }

    /** Условие с явным from; reset=true. */
    private static ScoreCondition score(String objective, int from, int to) {
        return new ScoreCondition(objective, ScoreboardCriterion.DUMMY, from, to, true);
    }

    /** Условие с явным from и reset. */
    private static ScoreCondition score(String objective, int from, int to, boolean reset) {
        return new ScoreCondition(objective, ScoreboardCriterion.DUMMY, from, to, reset);
    }

    // ── ascending ─────────────────────────────────────────────────────────────

    @Test
    void ascending_belowTarget_returnsFalse() {
        var context = new StubConditionContext().withScore("kills", 4);
        assertFalse(handler.test(score("kills", 5), context));
    }

    @Test
    void ascending_atTarget_returnsTrue() {
        var context = new StubConditionContext().withScore("kills", 5);
        assertTrue(handler.test(score("kills", 5), context));
    }

    @Test
    void ascending_aboveTarget_returnsTrue() {
        var context = new StubConditionContext().withScore("kills", 10);
        assertTrue(handler.test(score("kills", 5), context));
    }

    // ── descending ─────────────────────────────────────────────────────────────

    @Test
    void descending_aboveTarget_returnsFalse() {
        // from=10 > to=0 → нисходящее, нужно score <= 0
        var context = new StubConditionContext().withScore("countdown", 5);
        assertFalse(handler.test(score("countdown", 10, 0), context));
    }

    @Test
    void descending_atTarget_returnsTrue() {
        var context = new StubConditionContext().withScore("countdown", 0);
        assertTrue(handler.test(score("countdown", 10, 0), context));
    }

    @Test
    void descending_belowTarget_returnsTrue() {
        var context = new StubConditionContext().withScore("countdown", -1);
        assertTrue(handler.test(score("countdown", 10, 0), context));
    }

    // ── getCurrentValue (directional progress) ─────────────────────────────────

    @Test
    void getCurrentValue_ascending_returnsClamped() {
        var context = new StubConditionContext().withScore("kills", 7);
        // from=0, to=10 → progress = 7-0 = 7
        assertEquals(7, handler.getCurrentValue(score("kills", 10), context));
    }

    @Test
    void getCurrentValue_ascending_clampsBelowZero() {
        // score ниже from — прогресс 0
        var context = new StubConditionContext().withScore("kills", -5);
        assertEquals(0, handler.getCurrentValue(score("kills", 0, 10), context));
    }

    @Test
    void getCurrentValue_ascending_clampsAboveRange() {
        // score выше to — прогресс = range
        var context = new StubConditionContext().withScore("kills", 15);
        assertEquals(10, handler.getCurrentValue(score("kills", 0, 10), context));
    }

    @Test
    void getCurrentValue_descending_returnsClamped() {
        // from=10, to=0, score=6 → progress = 10-6 = 4
        var context = new StubConditionContext().withScore("countdown", 6);
        assertEquals(4, handler.getCurrentValue(score("countdown", 10, 0), context));
    }

    @Test
    void missingObjective_defaultsToZeroProgress() {
        var context = new StubConditionContext();
        // score=0, from=0, to=5 → progress=0
        assertEquals(0, handler.getCurrentValue(score("unknown", 5), context));
    }

    // ── load / reset ───────────────────────────────────────────────────────────

    @Test
    void load_resetTrue_writesFromIntoScore() {
        var context = new StubConditionContext().withScore("countdown", 0);
        handler.load(score("countdown", 10, 0, true), context);
        // from=10 должен быть записан в scoreboard
        assertEquals(10, context.getScore("countdown", null));
    }

    @Test
    void load_resetFalse_keepsExistingScore() {
        var context = new StubConditionContext().withScore("kills", 7);
        handler.load(score("kills", 0, 10, false), context);
        assertEquals(7, context.getScore("kills", null));
    }

    @Test
    void load_doesNotThrow() {
        var context = new StubConditionContext();
        assertDoesNotThrow(() -> handler.load(score("kills", 5), context));
    }

    // ── getTargetValue ─────────────────────────────────────────────────────────

    @Test
    void getTargetValue_ascending_returnsRange() {
        // from=0, to=10 → range=10
        var context = new StubConditionContext();
        assertEquals(10, handler.getTargetValue(score("kills", 0, 10), context));
    }

    @Test
    void getTargetValue_descending_returnsRange() {
        // from=10, to=0 → range=10
        var context = new StubConditionContext();
        assertEquals(10, handler.getTargetValue(score("countdown", 10, 0), context));
    }

    // ── evaluate ───────────────────────────────────────────────────────────────

    @Test
    void evaluate_ascending_returnsProgressAndMet() {
        var context = new StubConditionContext().withScore("kills", 5);
        var result = handler.evaluate(score("kills", 5), context);
        assertEquals(5, result.value());
        assertTrue(result.met());
    }

    @Test
    void evaluate_descending_belowTarget_returnsProgressAndMet() {
        // from=10, to=0, score=0 → progress=10, met=true
        var context = new StubConditionContext().withScore("countdown", 0);
        var result = handler.evaluate(score("countdown", 10, 0), context);
        assertEquals(10, result.value());
        assertTrue(result.met());
    }
}

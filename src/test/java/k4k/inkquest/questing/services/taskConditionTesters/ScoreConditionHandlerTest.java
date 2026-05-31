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

    private static ScoreCondition score(String objective, int target) {
        return new ScoreCondition(objective, ScoreboardCriterion.DUMMY, null, null, target);
    }

    private static ScoreCondition score(String objective, int target, int initial) {
        return new ScoreCondition(objective, ScoreboardCriterion.DUMMY, null, initial, target);
    }

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

    @Test
    void descending_aboveTarget_returnsFalse() {
        // initial=10 > target=0 → descending, need score <= 0
        var context = new StubConditionContext().withScore("countdown", 5);
        assertFalse(handler.test(score("countdown", 0, 10), context));
    }

    @Test
    void descending_atTarget_returnsTrue() {
        var context = new StubConditionContext().withScore("countdown", 0);
        assertTrue(handler.test(score("countdown", 0, 10), context));
    }

    @Test
    void descending_belowTarget_returnsTrue() {
        var context = new StubConditionContext().withScore("countdown", -1);
        assertTrue(handler.test(score("countdown", 0, 10), context));
    }

    @Test
    void getCurrentValue_returnsScoreFromContext() {
        var context = new StubConditionContext().withScore("kills", 7);
        assertEquals(7, handler.getCurrentValue(score("kills", 10), context));
    }

    @Test
    void missingObjective_defaultsToZero() {
        var context = new StubConditionContext();
        assertEquals(0, handler.getCurrentValue(score("unknown", 5), context));
    }

    @Test
    void load_callsEnsureOnContext() {
        // Проверяем что load не бросает исключений (StubConditionContext — no-op)
        var context = new StubConditionContext();
        assertDoesNotThrow(() -> handler.load(score("kills", 5), context));
    }

    @Test
    void load_withInitial_writesInitialIntoScore() {
        var context = new StubConditionContext().withScore("countdown", 0);
        handler.load(score("countdown", 0, 10), context);
        assertEquals(10, handler.getCurrentValue(score("countdown", 0, 10), context));
    }

    @Test
    void load_withoutInitial_keepsExistingScore() {
        var context = new StubConditionContext().withScore("kills", 7);
        handler.load(score("kills", 10), context);
        assertEquals(7, handler.getCurrentValue(score("kills", 10), context));
    }
}

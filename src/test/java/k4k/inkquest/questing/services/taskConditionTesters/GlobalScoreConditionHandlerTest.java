package k4k.inkquest.questing.services.taskConditionTesters;

import k4k.inkquest.domain.models.taskConditions.GlobalScoreCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GlobalScoreConditionHandlerTest {
    private GlobalScoreConditionHandler handler;
    private static final String PLAYER = "#GLOBAL";

    @BeforeEach
    void setUp() {
        handler = new GlobalScoreConditionHandler();
    }

    private static GlobalScoreCondition gs(String objective, int from, int to) {
        return new GlobalScoreCondition(objective, PLAYER, from, to);
    }

    private static GlobalScoreCondition gs(String objective, String player, int from, int to) {
        return new GlobalScoreCondition(objective, player, from, to);
    }

    /** Вспомогательный: контекст с заданным значением для GLOBAL-holder'а. */
    private static StubConditionContext ctx(String objective, int value) {
        return new StubConditionContext().withScoreForHolder(objective, PLAYER, value);
    }

    // ── ascending ──────────────────────────────────────────────────────────────

    @Test
    void ascending_belowTarget_returnsFalse() {
        assertFalse(handler.test(gs("donations", 0, 5), ctx("donations", 4)));
    }

    @Test
    void ascending_atTarget_returnsTrue() {
        assertTrue(handler.test(gs("donations", 0, 5), ctx("donations", 5)));
    }

    @Test
    void ascending_aboveTarget_returnsTrue() {
        assertTrue(handler.test(gs("donations", 0, 5), ctx("donations", 10)));
    }

    // ── descending ─────────────────────────────────────────────────────────────

    @Test
    void descending_aboveTarget_returnsFalse() {
        assertFalse(handler.test(gs("health", 100, 0), ctx("health", 5)));
    }

    @Test
    void descending_atTarget_returnsTrue() {
        assertTrue(handler.test(gs("health", 100, 0), ctx("health", 0)));
    }

    // ── getCurrentValue (directional progress) ─────────────────────────────────

    @Test
    void getCurrentValue_ascending_returnsProgress() {
        // from=0, to=10, score=7 → progress=7
        assertEquals(7, handler.getCurrentValue(gs("donations", 0, 10), ctx("donations", 7)));
    }

    @Test
    void getCurrentValue_descending_returnsProgress() {
        // from=100, to=0, score=60 → progress=40
        assertEquals(40, handler.getCurrentValue(gs("health", 100, 0), ctx("health", 60)));
    }

    @Test
    void getCurrentValue_ascending_clampsBelowZero() {
        assertEquals(0, handler.getCurrentValue(gs("donations", 0, 10), ctx("donations", -3)));
    }

    @Test
    void getCurrentValue_ascending_clampsAboveRange() {
        assertEquals(10, handler.getCurrentValue(gs("donations", 0, 10), ctx("donations", 15)));
    }

    // ── load — не пишет в scoreboard ──────────────────────────────────────────

    @Test
    void load_doesNotWriteScore() {
        var context = ctx("donations", 42);
        handler.load(gs("donations", 0, 10), context);
        // значение не должно измениться
        assertEquals(42, context.getScore("donations", PLAYER));
    }

    @Test
    void load_doesNotThrow() {
        assertDoesNotThrow(() -> handler.load(gs("donations", 0, 10), new StubConditionContext()));
    }

    // ── holder-маршрутизация ───────────────────────────────────────────────────

    @Test
    void readsFromHolder_notNullPlayer() {
        // GLOBAL holder имеет значение 8; контекстный (null) — 0
        var context = new StubConditionContext()
                .withScoreForHolder("events", "#GLOBAL", 8);
        // контекстный (null) ничего не имеет → 0
        assertEquals(0, context.getScore("events", null));
        // holder #GLOBAL → 8
        assertEquals(8, handler.getCurrentValue(gs("events", "#GLOBAL", 0, 10), context));
    }

    @Test
    void customHolder_isRespected() {
        var context = new StubConditionContext()
                .withScoreForHolder("boss_hp", "#BOSS", 7);
        var condition = gs("boss_hp", "#BOSS", 0, 10);
        assertEquals(7, handler.getCurrentValue(condition, context));
    }

    // ── getTargetValue ─────────────────────────────────────────────────────────

    @Test
    void getTargetValue_ascending_returnsRange() {
        // from=0, to=10 → range=10
        assertEquals(10, handler.getTargetValue(gs("donations", 0, 10), new StubConditionContext()));
    }

    @Test
    void getTargetValue_descending_returnsRange() {
        // from=100, to=0 → range=100
        assertEquals(100, handler.getTargetValue(gs("health", 100, 0), new StubConditionContext()));
    }

    // ── evaluate ───────────────────────────────────────────────────────────────

    @Test
    void evaluate_ascending_returnsProgressAndMet() {
        var result = handler.evaluate(gs("donations", 0, 10), ctx("donations", 10));
        assertEquals(10, result.value());
        assertTrue(result.met());
    }
}

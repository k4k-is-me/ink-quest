package k4k.inkquest.questing.services.taskConditionTesters;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScoreEvalTest {

    // ── range ──────────────────────────────────────────────────────────────────

    @Test
    void range_ascending() {
        assertEquals(10, ScoreEval.range(0, 10));
    }

    @Test
    void range_descending() {
        assertEquals(10, ScoreEval.range(10, 0));
    }

    @Test
    void range_equal_isZero() {
        assertEquals(0, ScoreEval.range(5, 5));
    }

    // ── descending detection ───────────────────────────────────────────────────

    @Test
    void descending_fromGreaterThanTo_isTrue() {
        assertTrue(ScoreEval.descending(10, 0));
    }

    @Test
    void descending_fromLessThanTo_isFalse() {
        assertFalse(ScoreEval.descending(0, 10));
    }

    @Test
    void descending_fromEqualsTo_isFalse() {
        assertFalse(ScoreEval.descending(5, 5));
    }

    // ── progress ascending ─────────────────────────────────────────────────────

    @Test
    void progress_ascending_middleOfRange() {
        assertEquals(5, ScoreEval.progress(5, 0, 10));
    }

    @Test
    void progress_ascending_clampsBelowZero() {
        assertEquals(0, ScoreEval.progress(-3, 0, 10));
    }

    @Test
    void progress_ascending_clampsAboveRange() {
        assertEquals(10, ScoreEval.progress(15, 0, 10));
    }

    @Test
    void progress_ascending_atFrom_isZero() {
        assertEquals(0, ScoreEval.progress(0, 0, 10));
    }

    @Test
    void progress_ascending_atTo_isRange() {
        assertEquals(10, ScoreEval.progress(10, 0, 10));
    }

    @Test
    void progress_ascending_nonZeroFrom() {
        // from=3, to=13, score=7 → 7-3 = 4
        assertEquals(4, ScoreEval.progress(7, 3, 13));
    }

    // ── progress descending ────────────────────────────────────────────────────

    @Test
    void progress_descending_middleOfRange() {
        // from=10, to=0, score=5 → 10-5 = 5
        assertEquals(5, ScoreEval.progress(5, 10, 0));
    }

    @Test
    void progress_descending_atFrom_isZero() {
        assertEquals(0, ScoreEval.progress(10, 10, 0));
    }

    @Test
    void progress_descending_atTo_isRange() {
        assertEquals(10, ScoreEval.progress(0, 10, 0));
    }

    @Test
    void progress_descending_clampsAboveRange() {
        // score ниже to: 10-(-5) = 15, но range=10
        assertEquals(10, ScoreEval.progress(-5, 10, 0));
    }

    @Test
    void progress_descending_clampsBelowZero() {
        // score выше from: 10-15 = -5 → clamped to 0
        assertEquals(0, ScoreEval.progress(15, 10, 0));
    }

    // ── met ────────────────────────────────────────────────────────────────────

    @Test
    void met_ascending_aboveTarget_isTrue() {
        assertTrue(ScoreEval.met(11, 0, 10));
    }

    @Test
    void met_ascending_atTarget_isTrue() {
        assertTrue(ScoreEval.met(10, 0, 10));
    }

    @Test
    void met_ascending_belowTarget_isFalse() {
        assertFalse(ScoreEval.met(9, 0, 10));
    }

    @Test
    void met_descending_belowTarget_isTrue() {
        assertTrue(ScoreEval.met(-1, 10, 0));
    }

    @Test
    void met_descending_atTarget_isTrue() {
        assertTrue(ScoreEval.met(0, 10, 0));
    }

    @Test
    void met_descending_aboveTarget_isFalse() {
        assertFalse(ScoreEval.met(1, 10, 0));
    }

    // ── edge-case: from == to ──────────────────────────────────────────────────
    // from == to трактуется как ascending (from > to = false), т.е. score >= to.

    @Test
    void met_fromEqualsTo_scoreAtTarget_isTrue() {
        assertTrue(ScoreEval.met(5, 5, 5));
    }

    @Test
    void met_fromEqualsTo_scoreAboveTarget_isTrue() {
        assertTrue(ScoreEval.met(10, 5, 5));
    }

    @Test
    void met_fromEqualsTo_scoreBelowTarget_isFalse() {
        // score=0 < to=5 → ascending → false
        assertFalse(ScoreEval.met(0, 5, 5));
    }

    @Test
    void progress_fromEqualsTo_isZero() {
        assertEquals(0, ScoreEval.progress(5, 5, 5));
    }

    // ── evaluate ───────────────────────────────────────────────────────────────

    @Test
    void evaluate_combines_progressAndMet() {
        var result = ScoreEval.evaluate(7, 0, 10);
        assertEquals(7, result.value());
        assertFalse(result.met());
    }

    @Test
    void evaluate_met_returnsCorrectPair() {
        var result = ScoreEval.evaluate(10, 0, 10);
        assertEquals(10, result.value());
        assertTrue(result.met());
    }
}

package k4k.inkquest.questing.services.taskConditionTesters;

import k4k.inkquest.domain.enums.CompletionStatus;
import k4k.inkquest.domain.models.taskConditions.OptionalsCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OptionalsConditionHandlerTest {
    private OptionalsConditionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OptionalsConditionHandler();
    }

    // ── Базовый пул: все optional этапа ─────────────────────────────────────

    @Test
    void allOptionals_allComplete_returnsTrue() {
        var condition = new OptionalsCondition(null, null);
        var context = new StubConditionContext()
                .withActiveStageOptionalTaskIds("o1", "o2")
                .withCompletedTask("o1", CompletionStatus.SUCCESS)
                .withCompletedTask("o2", CompletionStatus.FAILURE);
        // оба завершены с любым статусом → count=2, target=2 → true
        assertTrue(handler.test(condition, context));
    }

    @Test
    void allOptionals_partialComplete_returnsFalse() {
        var condition = new OptionalsCondition(null, null);
        var context = new StubConditionContext()
                .withActiveStageOptionalTaskIds("o1", "o2")
                .withCompletedTask("o1", CompletionStatus.SUCCESS);
        // только o1 → count=1, target=2 → false
        assertFalse(handler.test(condition, context));
    }

    // ── Пустой пул: вакуумная истина ─────────────────────────────────────────

    @Test
    void emptyPool_noOtherOptionals_returnsTrue() {
        var condition = new OptionalsCondition(null, null);
        var context = new StubConditionContext()
                .withActiveStageOptionalTaskIds();
        // пул пуст → target=0, count=0 → 0 >= 0 → true
        assertTrue(handler.test(condition, context));
    }

    @Test
    void emptyPool_getCurrentValueIsZero() {
        var condition = new OptionalsCondition(null, null);
        var context = new StubConditionContext()
                .withActiveStageOptionalTaskIds();
        assertEquals(0, handler.getCurrentValue(condition, context));
    }

    // ── Конкретный статус ────────────────────────────────────────────────────

    @Test
    void specificStatus_onlyMatchingCounted() {
        var condition = new OptionalsCondition(CompletionStatus.SUCCESS, null);
        var context = new StubConditionContext()
                .withActiveStageOptionalTaskIds("o1", "o2")
                .withCompletedTask("o1", CompletionStatus.SUCCESS)
                .withCompletedTask("o2", CompletionStatus.FAILURE);
        // только o1 совпадает → count=1, target=2 → false
        assertFalse(handler.test(condition, context));
    }

    @Test
    void specificStatus_allMatch_returnsTrue() {
        var condition = new OptionalsCondition(CompletionStatus.SUCCESS, null);
        var context = new StubConditionContext()
                .withActiveStageOptionalTaskIds("o1", "o2")
                .withCompletedTask("o1", CompletionStatus.SUCCESS)
                .withCompletedTask("o2", CompletionStatus.SUCCESS);
        assertTrue(handler.test(condition, context));
    }

    // ── Поле min ─────────────────────────────────────────────────────────────

    @Test
    void min_meetsMinimum_returnsTrue() {
        var condition = new OptionalsCondition(null, 1);
        var context = new StubConditionContext()
                .withActiveStageOptionalTaskIds("o1", "o2", "o3")
                .withCompletedTask("o1", CompletionStatus.SUCCESS);
        // count=1, target=1 → true
        assertTrue(handler.test(condition, context));
    }

    @Test
    void min_belowMinimum_returnsFalse() {
        var condition = new OptionalsCondition(null, 2);
        var context = new StubConditionContext()
                .withActiveStageOptionalTaskIds("o1", "o2", "o3")
                .withCompletedTask("o1", CompletionStatus.SUCCESS);
        // count=1, target=2 → false
        assertFalse(handler.test(condition, context));
    }

    @Test
    void getCurrentValue_countsMatchingTasks() {
        var condition = new OptionalsCondition(CompletionStatus.SUCCESS, null);
        var context = new StubConditionContext()
                .withActiveStageOptionalTaskIds("o1", "o2", "o3")
                .withCompletedTask("o1", CompletionStatus.SUCCESS)
                .withCompletedTask("o2", CompletionStatus.FAILURE)
                .withCompletedTask("o3", CompletionStatus.SUCCESS);
        assertEquals(2, handler.getCurrentValue(condition, context));
    }

    // ── EvalResult ───────────────────────────────────────────────────────────

    @Test
    void evaluate_returnsValueAndMet() {
        var condition = new OptionalsCondition(null, 2);
        var context = new StubConditionContext()
                .withActiveStageOptionalTaskIds("o1", "o2", "o3")
                .withCompletedTask("o1", CompletionStatus.SUCCESS)
                .withCompletedTask("o2", CompletionStatus.SUCCESS);
        var result = handler.evaluate(condition, context);
        assertEquals(2, result.value());
        assertTrue(result.met());
    }
}

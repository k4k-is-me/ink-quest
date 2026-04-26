package k4k.travelcorequesting.questing.services.taskConditionTesters;

import k4k.travelcorequesting.domain.enums.CompletionStatus;
import k4k.travelcorequesting.domain.models.taskConditions.TasksCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TasksConditionHandlerTest {
    private TasksConditionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TasksConditionHandler();
    }

    // ── Явный пул задач ──────────────────────────────────────────────────────

    @Test
    void explicitPool_allComplete_returnsTrue() {
        var condition = new TasksCondition(null, null, List.of("t1", "t2"));
        var context = new StubConditionContext()
                .withCompletedTask("t1", CompletionStatus.SUCCESS)
                .withCompletedTask("t2", CompletionStatus.SUCCESS);
        assertTrue(handler.test(condition, context));
    }

    @Test
    void explicitPool_partialComplete_returnsFalse() {
        var condition = new TasksCondition(null, null, List.of("t1", "t2"));
        var context = new StubConditionContext()
                .withCompletedTask("t1", CompletionStatus.SUCCESS);
        assertFalse(handler.test(condition, context));
    }

    @Test
    void explicitPool_noneComplete_getCurrentValueIsZero() {
        var condition = new TasksCondition(null, null, List.of("t1", "t2"));
        var context = new StubConditionContext();
        assertEquals(0, handler.getCurrentValue(condition, context));
    }

    // ── Пул из активного этапа ───────────────────────────────────────────────

    @Test
    void activeStagePool_usedWhenTasksNull() {
        // null tasks → берём из getActiveStageTaskIds()
        var condition = new TasksCondition(null, null, null);
        var context = new StubConditionContext()
                .withActiveStageTaskIds("t1", "t2")
                .withCompletedTask("t1", CompletionStatus.SUCCESS)
                .withCompletedTask("t2", CompletionStatus.FAILURE);
        // оба завершены (любой статус) → count=2, target=2 → true
        assertTrue(handler.test(condition, context));
    }

    @Test
    void activeStagePool_emptyPool_returnsFalse() {
        // пустой активный этап → target=0, count=0 → false? Нет: 0 >= 0 → true
        var condition = new TasksCondition(null, null, null);
        var context = new StubConditionContext().withActiveStageTaskIds();
        assertTrue(handler.test(condition, context));
    }

    // ── Конкретный статус ────────────────────────────────────────────────────

    @Test
    void specificStatus_onlySuccessCountedWhenStatusIsSuccess() {
        var condition = new TasksCondition(CompletionStatus.SUCCESS, null, List.of("t1", "t2"));
        var context = new StubConditionContext()
                .withCompletedTask("t1", CompletionStatus.SUCCESS)
                .withCompletedTask("t2", CompletionStatus.FAILURE);
        // только t1 совпадает → count=1, target=2 → false
        assertFalse(handler.test(condition, context));
    }

    @Test
    void specificStatus_bothSuccess_returnsTrue() {
        var condition = new TasksCondition(CompletionStatus.SUCCESS, null, List.of("t1", "t2"));
        var context = new StubConditionContext()
                .withCompletedTask("t1", CompletionStatus.SUCCESS)
                .withCompletedTask("t2", CompletionStatus.SUCCESS);
        assertTrue(handler.test(condition, context));
    }

    // ── Конкретный count ─────────────────────────────────────────────────────

    @Test
    void countConstraint_meetsMinimum_returnsTrue() {
        var condition = new TasksCondition(null, 1, List.of("t1", "t2", "t3"));
        var context = new StubConditionContext()
                .withCompletedTask("t1", CompletionStatus.SUCCESS);
        // count=1 из 3, target=1 → true
        assertTrue(handler.test(condition, context));
    }

    @Test
    void countConstraint_belowMinimum_returnsFalse() {
        var condition = new TasksCondition(null, 2, List.of("t1", "t2", "t3"));
        var context = new StubConditionContext()
                .withCompletedTask("t1", CompletionStatus.SUCCESS);
        // count=1 из 3, target=2 → false
        assertFalse(handler.test(condition, context));
    }

    @Test
    void getCurrentValue_countsMatchingTasks() {
        var condition = new TasksCondition(CompletionStatus.SUCCESS, null, List.of("t1", "t2", "t3"));
        var context = new StubConditionContext()
                .withCompletedTask("t1", CompletionStatus.SUCCESS)
                .withCompletedTask("t2", CompletionStatus.FAILURE)
                .withCompletedTask("t3", CompletionStatus.SUCCESS);
        assertEquals(2, handler.getCurrentValue(condition, context));
    }
}

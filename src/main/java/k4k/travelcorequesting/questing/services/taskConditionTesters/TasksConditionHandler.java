package k4k.travelcorequesting.questing.services.taskConditionTesters;

import k4k.travelcorequesting.domain.enums.CompletionStatus;
import k4k.travelcorequesting.domain.models.taskConditions.TasksCondition;
import k4k.travelcorequesting.questing.abstractions.EvalResult;
import k4k.travelcorequesting.questing.abstractions.IConditionContext;
import k4k.travelcorequesting.questing.abstractions.ITaskConditionHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Обработчик {@link TasksCondition}: считает задачи из пула, у которых статус совпадает с ожидаемым.
 *
 * <p>Пул — явный список task ID или, если он не задан, задачи активного этапа
 * (за исключением задачи, которой принадлежит условие).
 * Цель — поле {@code count} или размер пула.
 */
public class TasksConditionHandler implements ITaskConditionHandler<TasksCondition> {

    @Override
    public boolean test(TasksCondition condition, IConditionContext context) {
        return getCurrentValue(condition, context) >= resolveTarget(condition, context);
    }

    @Override
    public int getCurrentValue(TasksCondition condition, IConditionContext context) {
        var pool = resolvePool(condition, context);
        return (int) pool.stream()
                .filter(taskId -> matchesStatus(context, taskId, condition.status()))
                .count();
    }

    @Override
    public EvalResult evaluate(TasksCondition condition, IConditionContext context) {
        var pool = resolvePool(condition, context);
        int target = condition.count() != null ? condition.count() : pool.size();
        int count = 0;
        for (var taskId : pool) {
            if (matchesStatus(context, taskId, condition.status())) count++;
        }
        return new EvalResult(count, count >= target);
    }

    /**
     * Возвращает пул задач: явный список из условия или задачи активного этапа.
     */
    private List<String> resolvePool(TasksCondition condition, IConditionContext context) {
        var tasks = condition.tasks();
        if (tasks != null && !tasks.isEmpty()) return tasks;
        return context.getActiveStageTaskIds();
    }

    /**
     * Фактическая цель: {@code count} из условия или размер пула.
     */
    private int resolveTarget(TasksCondition condition, IConditionContext context) {
        if (condition.count() != null) return condition.count();
        return resolvePool(condition, context).size();
    }

    /**
     * Проверяет, соответствует ли статус задачи ожидаемому.
     * {@code null} статус — любой терминальный.
     */
    private boolean matchesStatus(IConditionContext context, String taskId, @Nullable CompletionStatus expected) {
        if (expected == null) return context.isTaskComplete(taskId);
        return context.isTaskComplete(taskId, expected);
    }
}

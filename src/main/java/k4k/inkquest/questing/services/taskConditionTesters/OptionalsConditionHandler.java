package k4k.inkquest.questing.services.taskConditionTesters;

import k4k.inkquest.domain.enums.CompletionStatus;
import k4k.inkquest.domain.models.taskConditions.OptionalsCondition;
import k4k.inkquest.questing.abstractions.EvalResult;
import k4k.inkquest.questing.abstractions.IConditionContext;
import k4k.inkquest.questing.abstractions.ITaskConditionHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Обработчик {@link OptionalsCondition}: считает optional-задачи активного этапа,
 * у которых статус совпадает с ожидаемым.
 *
 * <p>Пул всегда берётся из {@link IConditionContext#getActiveStageOptionalTaskIds()} —
 * optional-задачи активного этапа без required-задачи и без задачи, владеющей условием.
 * Цель — поле {@code min} или размер пула (все optional).
 */
public class OptionalsConditionHandler implements ITaskConditionHandler<OptionalsCondition> {

    /**
     * Возвращает фактическую цель: {@code min} или размер пула optional-задач этапа.
     */
    @Override
    public int getTargetValue(OptionalsCondition condition, IConditionContext context) {
        return resolveTarget(condition, context.getActiveStageOptionalTaskIds());
    }

    @Override
    public boolean test(OptionalsCondition condition, IConditionContext context) {
        var pool = context.getActiveStageOptionalTaskIds();
        return countMatched(condition, context, pool) >= resolveTarget(condition, pool);
    }

    @Override
    public int getCurrentValue(OptionalsCondition condition, IConditionContext context) {
        return countMatched(condition, context, context.getActiveStageOptionalTaskIds());
    }

    @Override
    public EvalResult evaluate(OptionalsCondition condition, IConditionContext context) {
        var pool = context.getActiveStageOptionalTaskIds();
        int matched = countMatched(condition, context, pool);
        return new EvalResult(matched, matched >= resolveTarget(condition, pool));
    }

    /**
     * Считает задачи из пула, статус которых совпадает с ожидаемым.
     */
    private int countMatched(OptionalsCondition condition, IConditionContext context, List<String> pool) {
        return (int) pool.stream()
                .filter(taskId -> matchesStatus(context, taskId, condition.status()))
                .count();
    }

    /**
     * Фактическая цель: {@code min} из условия или размер пула.
     */
    private int resolveTarget(OptionalsCondition condition, List<String> pool) {
        return condition.min() != null ? condition.min() : pool.size();
    }

    /**
     * Проверяет совпадение статуса задачи с ожидаемым.
     * {@code null} статус — любой терминальный.
     */
    private boolean matchesStatus(IConditionContext context, String taskId, @Nullable CompletionStatus expected) {
        if (expected == null) return context.isTaskComplete(taskId);
        return context.isTaskComplete(taskId, expected);
    }
}

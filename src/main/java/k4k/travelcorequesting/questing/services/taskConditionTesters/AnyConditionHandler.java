package k4k.travelcorequesting.questing.services.taskConditionTesters;

import k4k.travelcorequesting.domain.abstractions.ITaskCondition;
import k4k.travelcorequesting.domain.models.taskConditions.AnyCondition;
import k4k.travelcorequesting.questing.abstractions.IConditionContext;
import k4k.travelcorequesting.questing.abstractions.ITaskConditionHandler;

/** Обработчик {@link AnyCondition}: условие выполнено, если хотя бы одно подусловие выполнено. */
public class AnyConditionHandler implements ITaskConditionHandler<AnyCondition> {
    private final ITaskConditionHandler<ITaskCondition> dispatcher;

    public AnyConditionHandler(ITaskConditionHandler<ITaskCondition> dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public boolean test(AnyCondition condition, IConditionContext context) {
        return condition.subConditions().stream()
                .anyMatch(subCondition -> this.dispatcher.test(subCondition, context));
    }

    @Override
    public int getCurrentValue(AnyCondition condition, IConditionContext context) {
        return this.test(condition, context) ? 1 : 0;
    }
}

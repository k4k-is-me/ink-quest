package k4k.travelcorequesting.questing.services.taskConditionTesters;

import k4k.travelcorequesting.domain.abstractions.ITaskCondition;
import k4k.travelcorequesting.domain.models.taskConditions.NoneCondition;
import k4k.travelcorequesting.questing.abstractions.IConditionContext;
import k4k.travelcorequesting.questing.abstractions.ITaskConditionHandler;

/** Обработчик {@link NoneCondition}: условие выполнено, если ни одно подусловие не выполнено. */
public class NoneConditionHandler implements ITaskConditionHandler<NoneCondition> {
    private final ITaskConditionHandler<ITaskCondition> dispatcher;

    public NoneConditionHandler(ITaskConditionHandler<ITaskCondition> dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public boolean test(NoneCondition condition, IConditionContext context) {
        return condition.subConditions().stream()
                .noneMatch(subCondition -> this.dispatcher.test(subCondition, context));
    }

    @Override
    public int getCurrentValue(NoneCondition condition, IConditionContext context) {
        return this.test(condition, context) ? 1 : 0;
    }
}

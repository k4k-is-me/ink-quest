package k4k.travelcorequesting.questing.services.taskConditionTesters;

import k4k.travelcorequesting.domain.abstractions.ITaskCondition;
import k4k.travelcorequesting.domain.models.taskConditions.AllCondition;
import k4k.travelcorequesting.questing.abstractions.IConditionContext;
import k4k.travelcorequesting.questing.abstractions.ITaskConditionHandler;

/**
 * Обработчик {@link AllCondition}: условие выполнено только если все подусловия выполнены.
 * Прогресс — число выполненных подусловий.
 */
public class AllConditionHandler implements ITaskConditionHandler<AllCondition> {
    private final ITaskConditionHandler<ITaskCondition> dispatcher;

    public AllConditionHandler(ITaskConditionHandler<ITaskCondition> dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public boolean test(AllCondition condition, IConditionContext context) {
        return condition.subConditions().stream()
                .allMatch(subCondition -> this.dispatcher.test(subCondition, context));
    }

    @Override
    public int getCurrentValue(AllCondition condition, IConditionContext context) {
        return (int) condition.subConditions().stream()
                .filter(subCondition -> this.dispatcher.test(subCondition, context))
                .count();
    }
}

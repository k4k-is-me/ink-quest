package k4k.travelcorequesting.questing.services.taskConditionTesters;

import k4k.travelcorequesting.domain.models.taskConditions.PredicateCondition;
import k4k.travelcorequesting.questing.abstractions.EvalResult;
import k4k.travelcorequesting.questing.abstractions.IConditionContext;
import k4k.travelcorequesting.questing.abstractions.ITaskConditionHandler;

/**
 * Обработчик {@link PredicateCondition}: вычисляет Minecraft predicate против игрока.
 * Бинарное условие — возвращает 0 или 1.
 */
public class PredicateConditionHandler implements ITaskConditionHandler<PredicateCondition> {

    @Override
    public boolean test(PredicateCondition condition, IConditionContext context) {
        return context.testPredicate(condition.predicateId());
    }

    @Override
    public int getCurrentValue(PredicateCondition condition, IConditionContext context) {
        return this.test(condition, context) ? 1 : 0;
    }

    @Override
    public EvalResult evaluate(PredicateCondition condition, IConditionContext context) {
        boolean met = context.testPredicate(condition.predicateId());
        return new EvalResult(met ? 1 : 0, met);
    }
}

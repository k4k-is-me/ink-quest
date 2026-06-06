package k4k.inkquest.questing.services.taskConditionTesters;

import k4k.inkquest.domain.abstractions.ITaskCondition;
import k4k.inkquest.domain.models.taskConditions.AllCondition;
import k4k.inkquest.questing.abstractions.EvalResult;
import k4k.inkquest.questing.abstractions.IConditionContext;
import k4k.inkquest.questing.abstractions.ITaskConditionHandler;

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
    public void load(AllCondition condition, IConditionContext context) {
        condition.subConditions().forEach(
                subCondition -> this.dispatcher.load(subCondition, context)
        );
    }

    /** Возвращает число подусловий — цель для прогресс-бара. */
    @Override
    public int getTargetValue(AllCondition condition, IConditionContext context) {
        return condition.subConditions().size();
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

    @Override
    public EvalResult evaluate(AllCondition condition, IConditionContext context) {
        int satisfied = 0;
        for (var sub : condition.subConditions()) {
            if (!this.dispatcher.evaluate(sub, context).met())
                return new EvalResult(satisfied, false);
            satisfied++;
        }
        return new EvalResult(satisfied, true);
    }
}

package k4k.inkquest.questing.services.taskConditionTesters;

import k4k.inkquest.domain.abstractions.ITaskCondition;
import k4k.inkquest.domain.models.taskConditions.AnyCondition;
import k4k.inkquest.questing.abstractions.EvalResult;
import k4k.inkquest.questing.abstractions.IConditionContext;
import k4k.inkquest.questing.abstractions.ITaskConditionHandler;

/** Обработчик {@link AnyCondition}: условие выполнено, если хотя бы одно подусловие выполнено. */
public class AnyConditionHandler implements ITaskConditionHandler<AnyCondition> {
    private final ITaskConditionHandler<ITaskCondition> dispatcher;

    public AnyConditionHandler(ITaskConditionHandler<ITaskCondition> dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public void load(AnyCondition condition, IConditionContext context) {
        condition.subConditions().forEach(
                subCondition -> this.dispatcher.load(subCondition, context)
        );
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

    @Override
    public EvalResult evaluate(AnyCondition condition, IConditionContext context) {
        for (var sub : condition.subConditions()) {
            if (this.dispatcher.evaluate(sub, context).met()) return new EvalResult(1, true);
        }
        return new EvalResult(0, false);
    }
}

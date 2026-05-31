package k4k.inkquest.questing.services.taskConditionTesters;

import k4k.inkquest.domain.abstractions.ITaskCondition;
import k4k.inkquest.domain.models.taskConditions.NoneCondition;
import k4k.inkquest.questing.abstractions.EvalResult;
import k4k.inkquest.questing.abstractions.IConditionContext;
import k4k.inkquest.questing.abstractions.ITaskConditionHandler;

/** Обработчик {@link NoneCondition}: условие выполнено, если ни одно подусловие не выполнено. */
public class NoneConditionHandler implements ITaskConditionHandler<NoneCondition> {
    private final ITaskConditionHandler<ITaskCondition> dispatcher;

    public NoneConditionHandler(ITaskConditionHandler<ITaskCondition> dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public void load(NoneCondition condition, IConditionContext context) {
        condition.subConditions().forEach(
                subCondition -> this.dispatcher.load(subCondition, context)
        );
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

    @Override
    public EvalResult evaluate(NoneCondition condition, IConditionContext context) {
        for (var sub : condition.subConditions()) {
            if (this.dispatcher.evaluate(sub, context).met()) return new EvalResult(0, false);
        }
        return new EvalResult(1, true);
    }
}

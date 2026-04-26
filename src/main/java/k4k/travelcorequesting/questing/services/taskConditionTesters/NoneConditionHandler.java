package k4k.travelcorequesting.questing.services.taskConditionTesters;

import k4k.travelcorequesting.domain.abstractions.ITaskCondition;
import k4k.travelcorequesting.domain.models.taskConditions.NoneCondition;
import k4k.travelcorequesting.questing.abstractions.ITaskConditionHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/** Обработчик {@link NoneCondition}: условие выполнено, если ни одно подусловие не выполнено. */
public class NoneConditionHandler implements ITaskConditionHandler<NoneCondition> {
    private final ITaskConditionHandler<ITaskCondition> dispatcher;

    public NoneConditionHandler(ITaskConditionHandler<ITaskCondition> dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public boolean test(NoneCondition condition, ServerPlayerEntity player, Identifier questId, String taskId) {
        return condition.subConditions().stream()
                .noneMatch(subCondition -> this.dispatcher.test(subCondition, player, questId, taskId));
    }

    @Override
    public int getCurrentValue(NoneCondition condition, ServerPlayerEntity player, Identifier questId, String taskId) {
        return this.test(condition, player, questId, taskId) ? 1 : 0;
    }
}

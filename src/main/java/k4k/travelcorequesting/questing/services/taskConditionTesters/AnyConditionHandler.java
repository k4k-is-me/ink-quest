package k4k.travelcorequesting.questing.services.taskConditionTesters;

import k4k.travelcorequesting.domain.abstractions.ITaskCondition;
import k4k.travelcorequesting.domain.models.taskConditions.AnyCondition;
import k4k.travelcorequesting.questing.abstractions.ITaskConditionHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/** Обработчик {@link AnyCondition}: условие выполнено, если хотя бы одно подусловие выполнено. */
public class AnyConditionHandler implements ITaskConditionHandler<AnyCondition> {
    private final ITaskConditionHandler<ITaskCondition> dispatcher;

    public AnyConditionHandler(ITaskConditionHandler<ITaskCondition> dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public boolean test(AnyCondition condition, ServerPlayerEntity player, Identifier questId, String taskId) {
        return condition.subConditions().stream()
                .anyMatch(subCondition -> this.dispatcher.test(subCondition, player, questId, taskId));
    }

    @Override
    public int getCurrentValue(AnyCondition condition, ServerPlayerEntity player, Identifier questId, String taskId) {
        return this.test(condition, player, questId, taskId) ? 1 : 0;
    }
}

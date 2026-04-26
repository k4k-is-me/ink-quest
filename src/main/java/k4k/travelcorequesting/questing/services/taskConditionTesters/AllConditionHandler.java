package k4k.travelcorequesting.questing.services.taskConditionTesters;

import k4k.travelcorequesting.domain.abstractions.ITaskCondition;
import k4k.travelcorequesting.domain.models.taskConditions.AllCondition;
import k4k.travelcorequesting.questing.abstractions.ITaskConditionHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class AllConditionHandler implements ITaskConditionHandler<AllCondition> {
    private final ITaskConditionHandler<ITaskCondition> dispatcher;

    public AllConditionHandler(ITaskConditionHandler<ITaskCondition> dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public boolean test(AllCondition condition, ServerPlayerEntity player, Identifier questId, String taskId) {
        return condition.subConditions().stream()
                .allMatch(subCondition -> this.dispatcher.test(subCondition, player, questId, taskId));
    }

    @Override
    public int getCurrentValue(AllCondition condition, ServerPlayerEntity player, Identifier questId, String taskId) {
        return (int) condition.subConditions().stream()
                .map(subCondition -> this.dispatcher.test(subCondition, player, questId, taskId))
                .filter(result -> result)
                .count();
    }
}

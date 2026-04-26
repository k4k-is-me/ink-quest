package k4k.travelcorequesting.questing.services.taskConditionTesters;

import k4k.travelcorequesting.domain.models.taskConditions.PredicateCondition;
import k4k.travelcorequesting.questing.abstractions.ITaskConditionHandler;
import net.minecraft.loot.LootDataType;
import net.minecraft.loot.context.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Objects;

public class PredicateConditionHandler implements ITaskConditionHandler<PredicateCondition> {
    @Override
    public boolean test(PredicateCondition condition, ServerPlayerEntity player, Identifier questId, String taskId) {
        var server = Objects.requireNonNull(player.getServer());
        var predicate = server.getLootManager().getElement(LootDataType.PREDICATES, condition.predicateId());
        if (predicate == null) return false;
        var parameterSet = new LootContextParameterSet.Builder(player.getServerWorld())
                .add(LootContextParameters.THIS_ENTITY, player)
                .add(LootContextParameters.ORIGIN, player.getPos())
                .build(LootContextTypes.COMMAND);
        var context = new LootContext.Builder(parameterSet).build(null);
        return predicate.test(context);
    }

    @Override
    public int getCurrentValue(PredicateCondition condition, ServerPlayerEntity player, Identifier questId, String taskId) {
        return this.test(condition, player, questId, taskId) ? 1 : 0;
    }
}

package k4k.travelcorequesting.questing.abstractions;

import k4k.travelcorequesting.domain.abstractions.ITaskCondition;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public interface ITaskConditionHandler<T extends ITaskCondition> {
    default void load(T condition, ServerPlayerEntity player, Identifier questId, String taskId) {}
    default void tick(T condition, ServerPlayerEntity player, Identifier questId, String taskId) {}
    boolean test(T condition, ServerPlayerEntity player, Identifier questId, String taskId);
    int getCurrentValue(T condition, ServerPlayerEntity player, Identifier questId, String taskId);
}

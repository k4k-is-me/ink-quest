package k4k.travelcorequesting.questing.abstractions;

import k4k.travelcorequesting.domain.abstractions.ITaskCondition;
import net.minecraft.server.network.ServerPlayerEntity;

public interface ITaskConditionHandler<T extends ITaskCondition> {
    default void load(T condition, ServerPlayerEntity player) {}
    default void tick(T condition, ServerPlayerEntity player) {}
    boolean test(T condition, ServerPlayerEntity player);
    int getCurrentValue(T condition, ServerPlayerEntity player);
}

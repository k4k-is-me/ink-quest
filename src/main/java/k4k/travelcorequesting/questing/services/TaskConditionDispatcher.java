package k4k.travelcorequesting.questing.services;

import k4k.travelcorequesting.domain.abstractions.ITaskCondition;
import k4k.travelcorequesting.questing.abstractions.ITaskConditionHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class TaskConditionDispatcher implements ITaskConditionHandler<ITaskCondition> {
    private final Map<Class<?>, RegistryEntry> registry = new HashMap<>();

    public <T extends ITaskCondition> TaskConditionDispatcher register(Class<T> type, ITaskConditionHandler<T> handler) {
        registry.put(type, new RegistryEntry(
                (condition, player, questId, taskId) -> handler.load(type.cast(condition), player, questId, taskId),
                (condition, player, questId, taskId) -> handler.tick(type.cast(condition), player, questId, taskId),
                (condition, player, questId, taskId) -> handler.test(type.cast(condition), player, questId, taskId),
                (condition, player, questId, taskId) -> handler.getCurrentValue(type.cast(condition), player, questId, taskId)
        ));
        return this;
    }

    private RegistryEntry getRegistryEntry(ITaskCondition condition) {
        var type = condition.getClass();
        var entry = this.registry.get(type);
        if (entry == null) throw new NotImplementedException("There is no registered handler for provided condition '%s'".formatted(type.getSimpleName()));
        return entry;
    }

    @Override
    public void load(@Nullable ITaskCondition condition, ServerPlayerEntity player, Identifier questId, String taskId) {
        if (condition == null) return;
        this.getRegistryEntry(condition).load.accept(condition, player, questId, taskId);
    }

    @Override
    public void tick(@Nullable ITaskCondition condition, ServerPlayerEntity player, Identifier questId, String taskId) {
        if (condition == null) return;
        this.getRegistryEntry(condition).tick.accept(condition, player, questId, taskId);
    }

    @Override
    public boolean test(@Nullable ITaskCondition condition, ServerPlayerEntity player, Identifier questId, String taskId) {
        if (condition == null) return false;
        return this.getRegistryEntry(condition).test.test(condition, player, questId, taskId);
    }

    @Override
    public int getCurrentValue(@Nullable ITaskCondition condition, ServerPlayerEntity player, Identifier questId, String taskId) {
        if (condition == null) return 0;
        return this.getRegistryEntry(condition).getCurrentValue.get(condition, player, questId, taskId);
    }

    @FunctionalInterface private interface ConditionConsumer {
        void accept(ITaskCondition condition, ServerPlayerEntity player, Identifier questId, String taskId);
    }

    @FunctionalInterface private interface ConditionTestFn {
        boolean test(ITaskCondition condition, ServerPlayerEntity player, Identifier questId, String taskId);
    }

    @FunctionalInterface private interface ConditionValueFn {
        int get(ITaskCondition condition, ServerPlayerEntity player, Identifier questId, String taskId);
    }

    private record RegistryEntry(
        ConditionConsumer load,
        ConditionConsumer tick,
        ConditionTestFn test,
        ConditionValueFn getCurrentValue
    ) {}
}

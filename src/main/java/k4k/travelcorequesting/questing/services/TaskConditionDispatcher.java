package k4k.travelcorequesting.questing.services;

import k4k.travelcorequesting.domain.abstractions.ITaskCondition;
import k4k.travelcorequesting.questing.abstractions.ITaskConditionHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class TaskConditionDispatcher implements ITaskConditionHandler<ITaskCondition> {
    private final Map<Class<?>, RegistryEntry> registry = new HashMap<>();

    public <T extends ITaskCondition> TaskConditionDispatcher register(Class<T> type, ITaskConditionHandler<T> handler) {
        registry.put(type, new RegistryEntry(
                (condition, player) -> handler.load(type.cast(condition), player),
                (condition, player) -> handler.tick(type.cast(condition), player),
                (condition, player) -> handler.test(type.cast(condition), player),
                (condition, player) -> handler.getCurrentValue(type.cast(condition), player)
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
    public void load(@Nullable ITaskCondition condition, ServerPlayerEntity player) {
        if (condition == null) return;
        this.getRegistryEntry(condition).load.accept(condition, player);
    }

    @Override
    public void tick(@Nullable ITaskCondition condition, ServerPlayerEntity player) {
        if (condition == null) return;
        this.getRegistryEntry(condition).tick.accept(condition, player);
    }

    @Override
    public boolean test(@Nullable ITaskCondition condition, ServerPlayerEntity player) {
        if (condition == null) return false;
        return this.getRegistryEntry(condition).test.apply(condition, player);
    }

    @Override
    public int getCurrentValue(@Nullable ITaskCondition condition, ServerPlayerEntity player) {
        if (condition == null) return 0;
        return this.getRegistryEntry(condition).getCurrentValue.apply(condition, player);
    }

    private record RegistryEntry(
        BiConsumer<ITaskCondition, ServerPlayerEntity> load,
        BiConsumer<ITaskCondition, ServerPlayerEntity> tick,
        BiFunction<ITaskCondition, ServerPlayerEntity, Boolean> test,
        BiFunction<ITaskCondition, ServerPlayerEntity, Integer> getCurrentValue
    ) {}
}

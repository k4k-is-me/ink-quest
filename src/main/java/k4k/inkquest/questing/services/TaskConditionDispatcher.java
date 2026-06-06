package k4k.inkquest.questing.services;

import k4k.inkquest.domain.abstractions.ITaskCondition;
import k4k.inkquest.questing.abstractions.EvalResult;
import k4k.inkquest.questing.abstractions.IConditionContext;
import k4k.inkquest.questing.abstractions.ITaskConditionHandler;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Диспетчер обработчиков условий задач (паттерн Strategy).
 *
 * <p>Хранит реестр обработчиков, сопоставленных с типами условий.
 * При вызове делегирует выполнение в зарегистрированный обработчик.
 * Сам реализует {@link ITaskConditionHandler} для использования как единственная
 * точка входа для проверки любого условия.
 */
public class TaskConditionDispatcher implements ITaskConditionHandler<ITaskCondition> {
    private static final EvalResult EMPTY_RESULT = new EvalResult(0, false);

    private final Map<Class<?>, RegistryEntry> registry = new HashMap<>();

    /**
     * Регистрирует обработчик для конкретного типа условия.
     *
     * @param type    класс типа условия
     * @param handler обработчик для этого типа
     * @return this (fluent API)
     */
    public <T extends ITaskCondition> TaskConditionDispatcher register(Class<T> type, ITaskConditionHandler<T> handler) {
        registry.put(type, new RegistryEntry(
                (condition, context) -> handler.load(type.cast(condition), context),
                (condition, context) -> handler.tick(type.cast(condition), context),
                (condition, context) -> handler.test(type.cast(condition), context),
                (condition, context) -> handler.getTargetValue(type.cast(condition), context),
                (condition, context) -> handler.getCurrentValue(type.cast(condition), context),
                (condition, context) -> handler.evaluate(type.cast(condition), context)
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
    public void load(@Nullable ITaskCondition condition, IConditionContext context) {
        if (condition == null) return;
        this.getRegistryEntry(condition).load.accept(condition, context);
    }

    @Override
    public void tick(@Nullable ITaskCondition condition, IConditionContext context) {
        if (condition == null) return;
        this.getRegistryEntry(condition).tick.accept(condition, context);
    }

    @Override
    public boolean test(@Nullable ITaskCondition condition, IConditionContext context) {
        if (condition == null) return false;
        return this.getRegistryEntry(condition).test.test(condition, context);
    }

    @Override
    public int getTargetValue(@Nullable ITaskCondition condition, IConditionContext context) {
        if (condition == null) return 1;
        return this.getRegistryEntry(condition).getTargetValue.get(condition, context);
    }

    @Override
    public int getCurrentValue(@Nullable ITaskCondition condition, IConditionContext context) {
        if (condition == null) return 0;
        return this.getRegistryEntry(condition).getCurrentValue.get(condition, context);
    }

    @Override
    public EvalResult evaluate(@Nullable ITaskCondition condition, IConditionContext context) {
        if (condition == null) return EMPTY_RESULT;
        return this.getRegistryEntry(condition).evaluate.evaluate(condition, context);
    }

    @FunctionalInterface private interface ConditionConsumer {
        void accept(ITaskCondition condition, IConditionContext context);
    }

    @FunctionalInterface private interface ConditionTestFn {
        boolean test(ITaskCondition condition, IConditionContext context);
    }

    @FunctionalInterface private interface ConditionValueFn {
        int get(ITaskCondition condition, IConditionContext context);
    }

    @FunctionalInterface private interface ConditionEvaluateFn {
        EvalResult evaluate(ITaskCondition condition, IConditionContext context);
    }

    private record RegistryEntry(
        ConditionConsumer load,
        ConditionConsumer tick,
        ConditionTestFn test,
        ConditionValueFn getTargetValue,
        ConditionValueFn getCurrentValue,
        ConditionEvaluateFn evaluate
    ) {}
}

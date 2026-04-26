package k4k.travelcorequesting.questing.abstractions;

import k4k.travelcorequesting.domain.abstractions.ITaskCondition;

/**
 * Обработчик условия задачи конкретного типа.
 *
 * <p>Все методы принимают {@link IConditionContext} вместо {@code ServerPlayerEntity},
 * что позволяет тестировать обработчики без запущенного Minecraft-сервера.
 *
 * @param <T> тип условия, которое обрабатывает данный обработчик
 */
public interface ITaskConditionHandler<T extends ITaskCondition> {

    /**
     * Вызывается один раз при переходе задачи в активный этап.
     * Используется для подготовки внешнего состояния (например, создания scoreboard objective).
     */
    default void load(T condition, IConditionContext context) {}

    /**
     * Вызывается каждый тик, пока задача активна.
     * Используется для обновления вспомогательного состояния условия.
     */
    default void tick(T condition, IConditionContext context) {}

    /**
     * Проверяет, выполнено ли условие.
     *
     * @return {@code true} если условие выполнено
     */
    boolean test(T condition, IConditionContext context);

    /**
     * Возвращает текущий прогресс выполнения условия.
     * Для бинарных условий — 0 или 1. Для градуальных — значение от 0 до target.
     */
    int getCurrentValue(T condition, IConditionContext context);
}

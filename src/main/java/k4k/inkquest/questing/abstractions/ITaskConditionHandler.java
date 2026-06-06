package k4k.inkquest.questing.abstractions;

import k4k.inkquest.domain.abstractions.ITaskCondition;

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
     * Возвращает целевое значение условия — знаменатель прогресс-бара.
     *
     * <p>Для бинарных условий ({@code isGradual() == false}) возвращает 1.
     * Для градуальных — фактическую цель, которая может зависеть от {@code context}
     * (например, для {@code optionals} — размер пула optional-задач этапа).
     *
     * <p>Default-реализация возвращает 1 (бинарные условия).
     */
    default int getTargetValue(T condition, IConditionContext context) {
        return 1;
    }

    /**
     * Возвращает текущий прогресс выполнения условия.
     * Для бинарных условий — 0 или 1. Для градуальных — значение от 0 до target.
     */
    int getCurrentValue(T condition, IConditionContext context);

    /**
     * Оценивает условие за один проход: возвращает и текущее значение, и факт
     * выполнения. Default — два независимых вызова {@code getCurrentValue} и
     * {@code test}; конкретные хэндлеры переопределяют, чтобы избежать
     * дублирующих вычислений (например, второго вызова predicate).
     */
    default EvalResult evaluate(T condition, IConditionContext context) {
        return new EvalResult(getCurrentValue(condition, context), test(condition, context));
    }
}

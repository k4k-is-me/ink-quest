package k4k.inkquest.domain.abstractions;

/**
 * Маркерный интерфейс условия задачи.
 *
 * <p>Используется как bound-тип для {@code ITaskConditionHandler<T extends ITaskCondition>}.
 * Прогресс-бар показывается тогда, когда {@code getTargetValue(condition, context) > 1}.
 */
public interface ITaskCondition {
}

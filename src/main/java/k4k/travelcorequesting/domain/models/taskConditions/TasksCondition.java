package k4k.travelcorequesting.domain.models.taskConditions;

import k4k.travelcorequesting.domain.abstractions.ITaskCondition;
import k4k.travelcorequesting.domain.enums.CompletionStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Условие, проверяющее статус других задач того же квеста.
 *
 * <p>Прогресс = число задач из пула, у которых статус совпадает с ожидаемым.
 * Цель = {@code count} (или размер пула, если {@code count} не задан).
 *
 * @param status ожидаемый статус задач; {@code null} = любой терминальный статус
 * @param count  минимальное число задач с нужным статусом; {@code null} = все задачи из пула
 * @param tasks  явный список task ID для проверки; {@code null}/пустой = задачи активного этапа
 */
public record TasksCondition(
        @Nullable CompletionStatus status,
        @Nullable Integer count,
        @Nullable List<String> tasks
) implements ITaskCondition {

    /**
     * Возвращает {@code true} если статически известная цель больше 1.
     * Когда пул динамический (tasks не задан и count не задан), цель = 0 и условие бинарное.
     */
    @Override
    public boolean isGradual() {
        return getTargetValue() > 1;
    }

    /**
     * Статически известная цель: {@code count}, иначе размер списка {@code tasks}, иначе 0 (динамический пул).
     * Для динамического пула фактическая цель определяется хендлером во время выполнения.
     */
    @Override
    public int getTargetValue() {
        if (count != null) return count;
        if (tasks != null && !tasks.isEmpty()) return tasks.size();
        return 0;
    }
}

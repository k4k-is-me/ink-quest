package k4k.inkquest.domain.models.taskConditions;

import k4k.inkquest.domain.abstractions.ITaskCondition;
import k4k.inkquest.domain.enums.CompletionStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Условие, выполняющееся когда выполнены optional-задачи активного этапа.
 *
 * <p>Пул — все optional-задачи активного этапа, за исключением задачи,
 * на которой висит данное условие. Required-задача этапа в пул не входит.
 * Пустой пул → условие выполнено сразу (вакуумная истина).
 *
 * @param status ожидаемый статус задач из пула; {@code null} = любой терминальный
 * @param min    минимальное число задач с нужным статусом; {@code null} = все задачи пула
 */
public record OptionalsCondition(
        @Nullable CompletionStatus status,
        @Nullable Integer min
) implements ITaskCondition {

    /**
     * Всегда {@code true} — условие градуальное независимо от значения {@code min}
     * и размера пула. Реальная цель вычисляется хендлером через {@code IConditionContext}.
     */
    @Override
    public boolean isGradual() {
        return true;
    }
}

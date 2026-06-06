package k4k.inkquest.domain.models.taskConditions;

import k4k.inkquest.domain.abstractions.ITaskCondition;

import java.util.List;

/** Условие выполнено, если хотя бы одно подусловие выполнено (логическое OR). */
public record AnyCondition(
        List<ITaskCondition> subConditions
) implements ITaskCondition {
}

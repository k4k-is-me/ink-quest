package k4k.inkquest.domain.models.taskConditions;

import k4k.inkquest.domain.abstractions.ITaskCondition;

import java.util.List;

/** Условие выполнено, если ни одно подусловие не выполнено (логическое NOT ANY). */
public record NoneCondition(
        List<ITaskCondition> subConditions
) implements ITaskCondition {
    @Override
    public boolean isGradual() {
        return false;
    }

    @Override
    public int getTargetValue() {
        return 1;
    }
}

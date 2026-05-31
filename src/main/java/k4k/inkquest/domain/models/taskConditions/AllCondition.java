package k4k.inkquest.domain.models.taskConditions;

import k4k.inkquest.domain.abstractions.ITaskCondition;

import java.util.List;

public record AllCondition (
        List<ITaskCondition> subConditions
) implements ITaskCondition {
    @Override
    public boolean isGradual() {
        return true;
    }

    @Override
    public int getTargetValue() {
        return this.subConditions.size();
    }
}

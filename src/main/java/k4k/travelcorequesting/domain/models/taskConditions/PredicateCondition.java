package k4k.travelcorequesting.domain.models.taskConditions;

import k4k.travelcorequesting.domain.abstractions.ITaskCondition;
import net.minecraft.util.Identifier;

public record PredicateCondition(
        Identifier predicateId
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

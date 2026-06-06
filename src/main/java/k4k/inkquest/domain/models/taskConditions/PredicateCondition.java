package k4k.inkquest.domain.models.taskConditions;

import k4k.inkquest.domain.abstractions.ITaskCondition;
import net.minecraft.util.Identifier;

public record PredicateCondition(
        Identifier predicateId
) implements ITaskCondition {
    @Override
    public boolean isGradual() {
        return false;
    }
}

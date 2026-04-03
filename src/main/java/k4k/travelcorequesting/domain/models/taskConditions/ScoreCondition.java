package k4k.travelcorequesting.domain.models.taskConditions;

import k4k.travelcorequesting.domain.abstractions.ITaskCondition;
import net.minecraft.scoreboard.ScoreboardCriterion;
import org.jetbrains.annotations.Nullable;

public record ScoreCondition(
        String objective,
        ScoreboardCriterion criterion,
        @Nullable String player,
        @Nullable Integer initial,
        int target
) implements ITaskCondition {
    @Override
    public boolean isGradual() {
        return true;
    }

    @Override
    public int getTargetValue() {
        return this.target;
    }
}

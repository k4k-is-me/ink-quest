package k4k.travelcorequesting.questing.services.taskConditionTesters;

import k4k.travelcorequesting.domain.models.taskConditions.ScoreCondition;
import k4k.travelcorequesting.questing.abstractions.ITaskConditionHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Objects;

public class ScoreConditionHandler implements ITaskConditionHandler<ScoreCondition> {
    public void load(ScoreCondition condition, ServerPlayerEntity player) {
        var scoreboard = Objects.requireNonNull(player.getServer())
                .getScoreboard();

        if (scoreboard.containsObjective(condition.objective()))
            return;

        scoreboard.addObjective(
                condition.objective(),
                condition.criterion(),
                Text.literal(condition.objective()),
                condition.criterion().getDefaultRenderType()
        );
    }

    @Override
    public boolean test(ScoreCondition condition, ServerPlayerEntity player) {
        if (condition.initial() != null && condition.initial() > condition.target())
            return this.getCurrentValue(condition, player) <= condition.target();
        return this.getCurrentValue(condition, player) >= condition.target();
    }

    public int getCurrentValue(ScoreCondition condition, ServerPlayerEntity player) {
        var scoreboard = Objects.requireNonNull(player.getServer())
                .getScoreboard();
        var objective = scoreboard.getObjective(condition.objective());
        var score = scoreboard.getPlayerScore(
                Objects.requireNonNullElse(condition.player(), player.getEntityName()),
                objective
        );
        return score.getScore();
    }
}

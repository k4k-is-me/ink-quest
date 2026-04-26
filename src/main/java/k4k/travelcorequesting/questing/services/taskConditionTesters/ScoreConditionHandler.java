package k4k.travelcorequesting.questing.services.taskConditionTesters;

import k4k.travelcorequesting.domain.models.taskConditions.ScoreCondition;
import k4k.travelcorequesting.questing.abstractions.IConditionContext;
import k4k.travelcorequesting.questing.abstractions.ITaskConditionHandler;

/**
 * Обработчик {@link ScoreCondition}: проверяет значение scoreboard objective.
 *
 * <p>Поддерживает восходящие (score &gt;= target) и нисходящие (score &lt;= target,
 * если initial &gt; target) условия.
 */
public class ScoreConditionHandler implements ITaskConditionHandler<ScoreCondition> {

    @Override
    public void load(ScoreCondition condition, IConditionContext context) {
        context.ensureScoreboardObjective(condition.objective(), condition.criterion());
    }

    @Override
    public boolean test(ScoreCondition condition, IConditionContext context) {
        if (condition.initial() != null && condition.initial() > condition.target())
            return this.getCurrentValue(condition, context) <= condition.target();
        return this.getCurrentValue(condition, context) >= condition.target();
    }

    @Override
    public int getCurrentValue(ScoreCondition condition, IConditionContext context) {
        return context.getScore(condition.objective(), condition.player());
    }
}

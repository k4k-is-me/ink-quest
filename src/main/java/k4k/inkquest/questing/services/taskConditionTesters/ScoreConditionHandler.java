package k4k.inkquest.questing.services.taskConditionTesters;

import k4k.inkquest.domain.models.taskConditions.ScoreCondition;
import k4k.inkquest.questing.abstractions.EvalResult;
import k4k.inkquest.questing.abstractions.IConditionContext;
import k4k.inkquest.questing.abstractions.ITaskConditionHandler;

/**
 * Обработчик {@link ScoreCondition}: проверяет значение scoreboard objective
 * контекстного игрока.
 *
 * <p>При {@code reset=true} записывает {@code initial} в scoreboard при загрузке.
 * Прогресс считается относительно {@code initial}: пройденный путь в направлении,
 * зажатый в [{@code 0}, {@code |target - initial|}]. Делегирует математику в
 * {@link ScoreEval}.
 */
public class ScoreConditionHandler implements ITaskConditionHandler<ScoreCondition> {

    @Override
    public void load(ScoreCondition condition, IConditionContext context) {
        context.ensureScoreboardObjective(condition.objective(), condition.criterion());
        if (condition.reset()) {
            context.setScore(condition.objective(), null, condition.initial());
        }
    }

    /** Возвращает размах диапазона {@code |target - initial|}. */
    @Override
    public int getTargetValue(ScoreCondition condition, IConditionContext context) {
        return ScoreEval.range(condition.initial(), condition.target());
    }

    @Override
    public boolean test(ScoreCondition condition, IConditionContext context) {
        int value = context.getScore(condition.objective(), null);
        return ScoreEval.met(value, condition.initial(), condition.target());
    }

    @Override
    public int getCurrentValue(ScoreCondition condition, IConditionContext context) {
        int value = context.getScore(condition.objective(), null);
        return ScoreEval.progress(value, condition.initial(), condition.target());
    }

    @Override
    public EvalResult evaluate(ScoreCondition condition, IConditionContext context) {
        int value = context.getScore(condition.objective(), null);
        return ScoreEval.evaluate(value, condition.initial(), condition.target());
    }
}

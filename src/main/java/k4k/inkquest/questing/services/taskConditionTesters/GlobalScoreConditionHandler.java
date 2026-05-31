package k4k.inkquest.questing.services.taskConditionTesters;

import k4k.inkquest.domain.models.taskConditions.GlobalScoreCondition;
import k4k.inkquest.questing.abstractions.EvalResult;
import k4k.inkquest.questing.abstractions.IConditionContext;
import k4k.inkquest.questing.abstractions.ITaskConditionHandler;
import net.minecraft.scoreboard.ScoreboardCriterion;

/**
 * Обработчик {@link GlobalScoreCondition}: проверяет значение scoreboard objective
 * на фиксированном holder'е (например, {@code "#GLOBAL"}).
 *
 * <p>При загрузке гарантирует существование objective с критерием {@code dummy},
 * но намеренно <b>не</b> записывает начальное значение в scoreboard —
 * при наличии нескольких игроков каждый load иначе перезаписывал бы общий счёт.
 * Счётчиком управляют снаружи через функции датапака или команды.
 *
 * <p>Делегирует математику прогресса и выполненности в {@link ScoreEval}.
 */
public class GlobalScoreConditionHandler implements ITaskConditionHandler<GlobalScoreCondition> {

    @Override
    public void load(GlobalScoreCondition condition, IConditionContext context) {
        context.ensureScoreboardObjective(condition.objective(), ScoreboardCriterion.DUMMY);
    }

    @Override
    public boolean test(GlobalScoreCondition condition, IConditionContext context) {
        int value = context.getScore(condition.objective(), condition.player());
        return ScoreEval.met(value, condition.initial(), condition.target());
    }

    @Override
    public int getCurrentValue(GlobalScoreCondition condition, IConditionContext context) {
        int value = context.getScore(condition.objective(), condition.player());
        return ScoreEval.progress(value, condition.initial(), condition.target());
    }

    @Override
    public EvalResult evaluate(GlobalScoreCondition condition, IConditionContext context) {
        int value = context.getScore(condition.objective(), condition.player());
        return ScoreEval.evaluate(value, condition.initial(), condition.target());
    }
}

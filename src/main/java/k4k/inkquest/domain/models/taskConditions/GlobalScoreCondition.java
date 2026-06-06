package k4k.inkquest.domain.models.taskConditions;

import k4k.inkquest.domain.abstractions.ITaskCondition;

/**
 * Условие по значению scoreboard objective фиксированного holder'а.
 *
 * <p>В отличие от {@link ScoreCondition}, holder задаётся явно через поле {@code player}
 * (дефолт {@code "#GLOBAL"}) и не является контекстным игроком.
 * Тип критерия всегда {@code dummy} — счётчиком управляют снаружи функции/команды.
 * Этот тип никогда не записывает значения в scoreboard при загрузке задачи,
 * что безопасно при одновременном прохождении квеста несколькими игроками.
 *
 * <p>Направление определяется так же, как у {@link ScoreCondition}:
 * {@code initial > target} → нисходящее ({@code score <= target}),
 * иначе восходящее ({@code score >= target}).
 */
public record GlobalScoreCondition(
        String objective,
        String player,
        int initial,
        int target
) implements ITaskCondition {
}

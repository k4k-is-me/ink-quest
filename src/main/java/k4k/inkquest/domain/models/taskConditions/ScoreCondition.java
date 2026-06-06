package k4k.inkquest.domain.models.taskConditions;

import k4k.inkquest.domain.abstractions.ITaskCondition;
import net.minecraft.scoreboard.ScoreboardCriterion;

/**
 * Условие по значению scoreboard objective контекстного игрока.
 *
 * <p>Направление определяется парой {@code initial}/{@code target}:
 * если {@code initial > target} — нисходящее ({@code score <= target}),
 * иначе восходящее ({@code score >= target}).
 *
 * <p>Поле {@code reset}: при {@code true} (дефолт) значение {@code initial}
 * записывается в scoreboard при загрузке задачи.
 * При {@code false} счёт не трогается — условие отслеживает текущее значение.
 */
public record ScoreCondition(
        String objective,
        ScoreboardCriterion criterion,
        int initial,
        int target,
        boolean reset
) implements ITaskCondition {
}

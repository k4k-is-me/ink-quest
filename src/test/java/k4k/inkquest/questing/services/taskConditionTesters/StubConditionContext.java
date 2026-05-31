package k4k.inkquest.questing.services.taskConditionTesters;

import k4k.inkquest.domain.enums.CompletionStatus;
import k4k.inkquest.questing.abstractions.IConditionContext;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Тестовый стаб {@link IConditionContext}. Не требует запущенного Minecraft-сервера.
 * Позволяет задавать ответы на все запросы контекста через fluent API.
 */
class StubConditionContext implements IConditionContext {
    private final Map<String, Integer> scores = new HashMap<>();
    private final Map<Identifier, Boolean> predicates = new HashMap<>();
    private final Map<String, CompletionStatus> taskStatuses = new HashMap<>();
    private List<String> activeStageTaskIds = List.of();

    /** Задаёт значение score для objective. */
    StubConditionContext withScore(String objectiveName, int value) {
        scores.put(objectiveName, value);
        return this;
    }

    /** Задаёт результат predicate. */
    StubConditionContext withPredicate(Identifier predicateId, boolean result) {
        predicates.put(predicateId, result);
        return this;
    }

    /** Помечает задачу завершённой с заданным статусом. */
    StubConditionContext withCompletedTask(String taskId, CompletionStatus status) {
        taskStatuses.put(taskId, status);
        return this;
    }

    /** Задаёт список задач активного этапа (без текущей задачи контекста). */
    StubConditionContext withActiveStageTaskIds(String... taskIds) {
        this.activeStageTaskIds = List.of(taskIds);
        return this;
    }

    @Override
    public void ensureScoreboardObjective(String objectiveName, ScoreboardCriterion criterion) {
        // no-op в тестах
    }

    @Override
    public int getScore(String objectiveName, @Nullable String playerOverride) {
        return scores.getOrDefault(objectiveName, 0);
    }

    @Override
    public void setScore(String objectiveName, @Nullable String playerOverride, int value) {
        scores.put(objectiveName, value);
    }

    @Override
    public boolean testPredicate(Identifier predicateId) {
        return predicates.getOrDefault(predicateId, false);
    }

    @Override
    public boolean isTaskComplete(String taskId) {
        return taskStatuses.containsKey(taskId);
    }

    @Override
    public boolean isTaskComplete(String taskId, CompletionStatus expected) {
        return taskStatuses.get(taskId) == expected;
    }

    @Override
    public List<String> getActiveStageTaskIds() {
        return activeStageTaskIds;
    }
}

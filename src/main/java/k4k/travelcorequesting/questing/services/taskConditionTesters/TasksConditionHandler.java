package k4k.travelcorequesting.questing.services.taskConditionTesters;

import k4k.travelcorequesting.domain.enums.CompletionStatus;
import k4k.travelcorequesting.domain.models.taskConditions.TasksCondition;
import k4k.travelcorequesting.questing.abstractions.ITaskConditionHandler;
import k4k.travelcorequesting.questing.abstractions.ServerQuestManagerContainer;
import k4k.travelcorequesting.questing.services.ServerQuestManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Обработчик {@link TasksCondition}: считает задачи из пула, у которых статус совпадает с ожидаемым.
 *
 * <p>Пул — явный список task ID или, если он не задан, задачи активного этапа.
 * Цель — поле {@code count} или размер пула.
 */
public class TasksConditionHandler implements ITaskConditionHandler<TasksCondition> {

    @Override
    public boolean test(TasksCondition condition, ServerPlayerEntity player, Identifier questId, String taskId) {
        return getCurrentValue(condition, player, questId, taskId) >= resolveTarget(condition, player, questId, taskId);
    }

    @Override
    public int getCurrentValue(TasksCondition condition, ServerPlayerEntity player, Identifier questId, String taskId) {
        var pool = resolvePool(condition, player, questId, taskId);
        var manager = getQuestManager(player);
        return (int) pool.stream()
                .filter(tid -> matchesStatus(manager, player, questId, tid, condition.status()))
                .count();
    }

    /**
     * Возвращает пул задач: явный список из условия или задачи активного этапа.
     */
    private List<String> resolvePool(TasksCondition condition, ServerPlayerEntity player, Identifier questId, String taskId) {
        var tasks = condition.tasks();
        if (tasks != null && !tasks.isEmpty()) return tasks;

        var manager = getQuestManager(player);
        var quest = manager.getQuestResolver().getQuest(questId);
        if (quest == null) return List.of();

        return manager.getActiveStage(questId, player)
                .map(quest::getStage)
                .map(ts -> ts.stream().filter(tid -> !Objects.equals(tid, taskId)).toList())
                .orElse(List.of());
    }

    /**
     * Фактическая цель: {@code count} из условия или размер пула.
     */
    private int resolveTarget(TasksCondition condition, ServerPlayerEntity player, Identifier questId, String taskId) {
        if (condition.count() != null) return condition.count();
        return resolvePool(condition, player, questId, taskId).size();
    }

    /**
     * Проверяет, соответствует ли статус задачи ожидаемому.
     * {@code null} статус = любой терминальный статус.
     */
    private boolean matchesStatus(
            ServerQuestManager manager,
            ServerPlayerEntity player,
            Identifier questId,
            String taskId,
            @Nullable CompletionStatus expected
    ) {
        if (expected == null) return manager.isTaskComplete(questId, taskId, player);
        return manager.isTaskComplete(questId, taskId, player, expected);
    }

    private ServerQuestManager getQuestManager(ServerPlayerEntity player) {
        return ServerQuestManagerContainer.getQuestManager(Objects.requireNonNull(player.getServer()));
    }
}

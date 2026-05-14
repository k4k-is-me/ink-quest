package k4k.travelcorequesting.infra.handlers;

import k4k.travelcorequesting.infra.networking.HudQuestRemoveS2CPacket;
import k4k.travelcorequesting.infra.networking.HudSetQuestStageS2CPacket;
import k4k.travelcorequesting.infra.networking.HudTaskAddS2CPacket;
import k4k.travelcorequesting.infra.networking.HudTaskCompleteS2CPacket;
import k4k.travelcorequesting.infra.networking.HudTaskPinS2CPacket;
import k4k.travelcorequesting.infra.networking.HudTaskRemoveS2CPacket;
import k4k.travelcorequesting.infra.networking.HudTaskSetProgressS2CPacket;
import k4k.travelcorequesting.infra.utils.HudQuests;
import k4k.travelcorequesting.infra.utils.HudTasks;
import k4k.travelcorequesting.questing.abstractions.ServerQuestManagerContainer;
import k4k.travelcorequesting.questing.events.QuestEvents;
import k4k.travelcorequesting.questing.events.QuestProgressEvents;
import k4k.travelcorequesting.questing.models.HudTask;
import k4k.travelcorequesting.questing.models.QuestEntry;
import k4k.travelcorequesting.questing.services.ServerQuestManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Серверный обработчик, синхронизирующий состояние HUD-оверлея на клиенте.
 * Переводит события квестовой системы в S2C-пакеты.
 */
public class QuestHudSyncHandler {

    /**
     * Текущий активный сервер. Нужен для итерации по игрокам при {@code QUEST_MODIFIED},
     * которое не несёт ни игрока, ни сервера в сигнатуре.
     */
    private static @Nullable MinecraftServer currentServer;

    /** Регистрирует все обработчики событий синхронизации HUD. */
    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> currentServer = server);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> currentServer = null);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                resyncPlayer(handler.player));

        QuestEvents.QUEST_PINNED.register((questEntry, player) -> {
            var questManager = ServerQuestManagerContainer.getQuestManager(player.getServer());
            var stage = questManager.getActiveStage(questEntry.questId(), player).orElse(null);
            sendQuestStagePacket(player, questManager, questEntry, stage);
        });

        QuestProgressEvents.STAGE_CHANGED.register((questEntry, stage, player) -> {
            if (stage == null) return;
            var questManager = ServerQuestManagerContainer.getQuestManager(player.getServer());
            if (!questManager.isQuestPinned(questEntry.questId(), player)) return;
            sendQuestStagePacket(player, questManager, questEntry, stage);
        });

        QuestProgressEvents.TASK_COMPLETED.register((taskEntry, player, status) -> {
            var questManager = ServerQuestManagerContainer.getQuestManager(player.getServer());
            if (!questManager.isQuestPinned(taskEntry.questId(), player)) return;
            ServerPlayNetworking.send(player, new HudTaskCompleteS2CPacket(
                    taskEntry.questId(),
                    taskEntry.taskId(),
                    status
            ));
        });

        QuestEvents.QUEST_UNPINNED.register((questId, player) ->
                ServerPlayNetworking.send(player, new HudQuestRemoveS2CPacket(questId))
        );

        QuestProgressEvents.TASK_SUCCESS_PROGRESS_CHANGED.register((taskEntry, player, newValue) -> {
            var questManager = ServerQuestManagerContainer.getQuestManager(player.getServer());
            if (!questManager.isQuestPinned(taskEntry.questId(), player)) return;
            ServerPlayNetworking.send(player, new HudTaskSetProgressS2CPacket(
                    taskEntry.questId(), taskEntry.taskId(), newValue, true));
        });

        QuestProgressEvents.TASK_FAILURE_PROGRESS_CHANGED.register((taskEntry, player, newValue) -> {
            var questManager = ServerQuestManagerContainer.getQuestManager(player.getServer());
            if (!questManager.isQuestPinned(taskEntry.questId(), player)) return;
            ServerPlayNetworking.send(player, new HudTaskSetProgressS2CPacket(
                    taskEntry.questId(), taskEntry.taskId(), newValue, false));
        });

        QuestEvents.TASK_PINNED.register((questId, taskId, player) ->
                ServerPlayNetworking.send(player, new HudTaskPinS2CPacket(questId, taskId))
        );

        QuestProgressEvents.TASK_LOADED.register((taskEntry, player, stageChanged) -> {
            if (stageChanged) return;
            var questManager = ServerQuestManagerContainer.getQuestManager(player.getServer());
            if (!questManager.isQuestPinned(taskEntry.questId(), player)) return;
            ServerPlayNetworking.send(player, new HudTaskAddS2CPacket(
                    taskEntry.questId(),
                    taskEntry.taskId(),
                    HudTasks.fromTask(taskEntry.task())
            ));
        });

        QuestProgressEvents.TASK_UNLOADED.register((taskEntry, player, stageChanged) -> {
            if (stageChanged) return;
            var questManager = ServerQuestManagerContainer.getQuestManager(player.getServer());
            if (!questManager.isQuestPinned(taskEntry.questId(), player)) return;
            // Завершённые задачи не требуют пакета удаления — HudTaskCompleteS2CPacket уже был отправлен
            if (questManager.isTaskComplete(taskEntry.questId(), taskEntry.taskId(), player)) return;
            ServerPlayNetworking.send(player, new HudTaskRemoveS2CPacket(
                    taskEntry.questId(),
                    taskEntry.taskId()
            ));
        });

        QuestEvents.QUEST_MODIFIED.register(questEntry -> {
            if (currentServer == null) return;
            for (var player : currentServer.getPlayerManager().getPlayerList()) {
                resyncQuestForPlayer(player, questEntry);
            }
        });
    }

    /**
     * Переотправляет данные всех закреплённых квестов всем онлайн-игрокам.
     * Вызывается после перезагрузки датапаков, чтобы HUD отразил обновлённые данные квестов.
     *
     * @param server сервер
     */
    public static void resyncAll(MinecraftServer server) {
        for (var player : server.getPlayerManager().getPlayerList()) {
            resyncPlayer(player);
        }
    }

    /**
     * Синхронизирует HUD конкретного игрока: переотправляет данные этапа и закреплённой задачи
     * для каждого закреплённого квеста. Вызывается при входе игрока и после перезагрузки датапаков.
     *
     * @param player игрок
     */
    private static void resyncPlayer(ServerPlayerEntity player) {
        var questManager = ServerQuestManagerContainer.getQuestManager(player.getServer());
        questManager.getTrackedQuests(player).stream()
                .filter(entry -> questManager.isQuestPinned(entry.questId(), player))
                .forEach(entry -> resyncQuestForPlayer(player, entry));
    }

    /**
     * Синхронизирует HUD конкретного игрока для одного квеста: отправляет пакет активного этапа
     * с полным tracking-состоянием (прогресс, статусы завершения, pin).
     *
     * @param player игрок
     * @param entry  запись квеста
     */
    private static void resyncQuestForPlayer(ServerPlayerEntity player, QuestEntry entry) {
        var questManager = ServerQuestManagerContainer.getQuestManager(player.getServer());
        if (!questManager.isQuestPinned(entry.questId(), player)) return;
        var stage = questManager.getActiveStage(entry.questId(), player).orElse(null);
        sendQuestStagePacket(player, questManager, entry, stage);
    }

    /**
     * Отправляет игроку пакет смены этапа для закреплённого квеста.
     * Если {@code stage} равен {@code null} (квест завершён или нет активного этапа),
     * отправляет пакет удаления квеста из HUD.
     *
     * <p>Каждая задача этапа дополняется tracking-данными: текущим прогрессом (для gradual
     * условий активных задач) и статусом завершения (для уже завершённых задач).
     * {@code pinnedTaskId} упакован в {@link k4k.travelcorequesting.questing.models.HudQuest}.
     *
     * @param player       игрок-получатель
     * @param questManager менеджер квестов
     * @param entry        запись квеста
     * @param stage        активный этап или {@code null}
     */
    private static void sendQuestStagePacket(
            ServerPlayerEntity player,
            ServerQuestManager questManager,
            QuestEntry entry,
            @Nullable Integer stage
    ) {
        if (stage == null) {
            ServerPlayNetworking.send(player, new HudQuestRemoveS2CPacket(entry.questId()));
            return;
        }

        var questId = entry.questId();
        Map<String, HudTask> tasks = new HashMap<>();

        for (var taskId : entry.quest().getStage(stage)) {
            var task = entry.quest().getTask(taskId);
            if (task == null) continue;

            var completionStatus = questManager.getTaskCompletionStatus(questId, taskId, player).orElse(null);

            Integer currentSuccessProgress = null;
            Integer currentFailureProgress = null;

            if (completionStatus == null) {
                var successCond = task.successCondition();
                if (successCond != null && successCond.isGradual()) {
                    currentSuccessProgress = questManager.getTaskSuccessCompletion(questId, taskId, player);
                }
                var failureCond = task.failureCondition();
                if (failureCond != null && failureCond.isGradual()) {
                    currentFailureProgress = questManager.getTaskFailureCompletion(questId, taskId, player);
                }
            }

            tasks.put(taskId, HudTasks.fromTask(task, currentSuccessProgress, currentFailureProgress, completionStatus));
        }

        var pinnedTaskId = questManager.getPinnedTaskId(questId, player).orElse(null);

        ServerPlayNetworking.send(player, new HudSetQuestStageS2CPacket(
                questId,
                HudQuests.fromQuest(entry.quest(), stage, pinnedTaskId),
                tasks
        ));
    }
}

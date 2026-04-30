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
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

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
            sendQuestStagePacket(player, questEntry, stage);
        });

        QuestProgressEvents.STAGE_CHANGED.register((questEntry, stage, player) -> {
            if (stage == null) return;
            var questManager = ServerQuestManagerContainer.getQuestManager(player.getServer());
            if (!questManager.isQuestPinned(questEntry.questId(), player)) return;
            sendQuestStagePacket(player, questEntry, stage);
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
     * Синхронизирует HUD конкретного игрока для одного квеста: отправляет данные активного этапа
     * и закреплённой задачи, если квест закреплён у этого игрока.
     *
     * @param player игрок
     * @param entry  запись квеста
     */
    private static void resyncQuestForPlayer(ServerPlayerEntity player, QuestEntry entry) {
        var questManager = ServerQuestManagerContainer.getQuestManager(player.getServer());
        if (!questManager.isQuestPinned(entry.questId(), player)) return;
        var stage = questManager.getActiveStage(entry.questId(), player).orElse(null);
        sendQuestStagePacket(player, entry, stage);
        questManager.getPinnedTaskId(entry.questId(), player).ifPresent(taskId ->
                ServerPlayNetworking.send(player, new HudTaskPinS2CPacket(entry.questId(), taskId))
        );
    }

    /**
     * Отправляет игроку пакет смены этапа для закреплённого квеста.
     * Если {@code stage} равен {@code null} (квест завершён или нет активного этапа),
     * отправляет пакет удаления квеста из HUD.
     *
     * @param player игрок-получатель
     * @param entry  запись квеста
     * @param stage  активный этап или {@code null}
     */
    private static void sendQuestStagePacket(ServerPlayerEntity player, QuestEntry entry, Integer stage) {
        if (stage == null) {
            ServerPlayNetworking.send(player, new HudQuestRemoveS2CPacket(entry.questId()));
            return;
        }

        Map<String, HudTask> tasks = entry.quest().getStage(stage).stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        taskId -> HudTasks.fromTask(Objects.requireNonNull(entry.quest().getTask(taskId)))
                ));

        ServerPlayNetworking.send(player, new HudSetQuestStageS2CPacket(
                entry.questId(),
                HudQuests.fromQuest(entry.quest(), stage),
                tasks
        ));
    }
}

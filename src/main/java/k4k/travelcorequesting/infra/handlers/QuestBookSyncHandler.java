package k4k.travelcorequesting.infra.handlers;

import k4k.travelcorequesting.infra.networking.QuestBookQuestListItemAddedS2CPacket;
import k4k.travelcorequesting.infra.networking.QuestBookQuestCompletedS2CPacket;
import k4k.travelcorequesting.infra.networking.QuestBookQuestRemovedS2CPacket;
import k4k.travelcorequesting.infra.networking.QuestBookSyncS2CPacket;
import k4k.travelcorequesting.questing.abstractions.ServerQuestManagerContainer;
import k4k.travelcorequesting.questing.events.QuestProgressEvents;
import k4k.travelcorequesting.questing.models.QuestBookQuestListItem;
import k4k.travelcorequesting.questing.models.QuestEntry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Серверный обработчик, поддерживающий список квестов на клиенте в актуальном состоянии.
 * Использует push-based синхронизацию: клиент никогда не запрашивает список сам.
 */
public class QuestBookSyncHandler {

    /** Регистрирует все обработчики событий синхронизации квестовой книги. */
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                syncFullList(handler.player));

        QuestProgressEvents.QUEST_GIVEN.register((questEntry, player) ->
                ServerPlayNetworking.send(player, new QuestBookQuestListItemAddedS2CPacket(
                        toQuestBookQuestListItem(questEntry, player)
                )));

        QuestProgressEvents.QUEST_DROPPED.register((questEntry, player) ->
                ServerPlayNetworking.send(player, new QuestBookQuestRemovedS2CPacket(
                        questEntry.questId()
                )));

        QuestProgressEvents.QUEST_COMPLETED.register((questEntry, status, player) ->
                ServerPlayNetworking.send(player, new QuestBookQuestCompletedS2CPacket(
                        questEntry.questId(), status
                )));
    }

    /**
     * Отправляет игроку полный список его квестов.
     *
     * @param player игрок, которому отправляется синхронизация
     */
    private static void syncFullList(ServerPlayerEntity player) {
        var questManager = ServerQuestManagerContainer.getQuestManager(player.getServer());
        var quests = questManager.getTrackedQuests(player).stream()
                .map(entry -> toQuestBookQuestListItem(entry, player))
                .toList();

        ServerPlayNetworking.send(player, new QuestBookSyncS2CPacket(quests));
    }

    /**
     * Строит {@link QuestBookQuestListItem} из записи квеста для конкретного игрока.
     *
     * @param entry  запись квеста
     * @param player игрок (нужен для получения статуса завершения)
     * @return данные карточки квеста
     */
    private static QuestBookQuestListItem toQuestBookQuestListItem(QuestEntry entry, ServerPlayerEntity player) {
        var questManager = ServerQuestManagerContainer.getQuestManager(player.getServer());
        var completionStatus = questManager.getQuestCompletionStatus(entry.questId(), player).orElse(null);

        return new QuestBookQuestListItem(
                entry.questId(),
                entry.quest().title(),
                entry.quest().description(),
                entry.quest().icon(),
                completionStatus,
                entry.quest().index()
        );
    }
}

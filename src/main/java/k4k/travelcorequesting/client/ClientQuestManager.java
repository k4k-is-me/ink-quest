package k4k.travelcorequesting.client;

import k4k.travelcorequesting.client.models.QuestBriefDto;
import k4k.travelcorequesting.questing.models.QuestDisplay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// Этот класс служит для отслеживания информации о квестах конкретного игрока на клиенте.
/// Экземпляр класса хранится в MinecraftClient и не должен быть инициализирован нигде,
/// кроме как там.
public class ClientQuestManager {
    private final List<QuestDisplay> pinnedQuests = new ArrayList<>();

//    private long lastSyncTimeMs = 0;
//    private long lastQuestReceivedTimeMs = 0;  // Maybe do the queue of events

    public List<QuestDisplay> getPinnedQuests() {
        return Collections.unmodifiableList(this.pinnedQuests);
    }

    /**
     * Полностью синхронизировать прогресс по квестам на клиенте с сервером.
     */
    public void syncWithServer() {
//        lastSyncTimeMs = Util.getMeasuringTimeMs();
//        ClientRequests.send(new QuestsSyncClientRequest()).thenAccept(response -> {
//            if (!response.quests().isEmpty())
//                lastQuestReceivedTimeMs = Util.getMeasuringTimeMs();
//
//            this.playerQuests.clear();
//            this.playerQuests.addAll(response.quests());
//        });
    }

    public void add(QuestBriefDto quest) {
//        lastQuestReceivedTimeMs = Util.getMeasuringTimeMs();
//        playerQuests.add(quest);
    }

    public List<QuestBriefDto> getPlayerQuests() {
        return Collections.emptyList();
    }
}

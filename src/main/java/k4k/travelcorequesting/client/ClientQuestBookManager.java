package k4k.travelcorequesting.client;

import k4k.travelcorequesting.common.datastructures.CacheEntry;
import k4k.travelcorequesting.common.requests.ClientRequests;
import k4k.travelcorequesting.domain.enums.CompletionStatus;
import k4k.travelcorequesting.infra.requests.GetQuestDetailsRequest;
import k4k.travelcorequesting.questing.models.QuestBookQuestListItem;
import k4k.travelcorequesting.questing.models.QuestBookQuest;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/// Клиентское состояние квестов игрока.
/// Список квестов поддерживается сервером через push-пакеты.
/// Детали квеста запрашиваются по требованию (pull) и кэшируются.
///
/// Экземпляр хранится в `MinecraftClient` и инициализируется только там.
public class ClientQuestBookManager {

    /** Время жизни кэша деталей квеста в миллисекундах. */
    public static final long DETAIL_CACHE_TTL_MS = 5_000L;

    /** Список квестов игрока, индексированный по questId для O(1) обновлений. */
    private final Map<Identifier, QuestBookQuestListItem> quests = new LinkedHashMap<>();

    /** Кэш деталей квестов. Инвалидируется при завершении или удалении квеста. */
    private final Map<Identifier, CacheEntry<QuestBookQuest>> detailsCache = new HashMap<>();

    // -------------------------------------------------------------------------
    // Push-обновления от сервера
    // -------------------------------------------------------------------------

    /**
     * Полностью заменяет список квестов.
     * Вызывается при входе игрока в мир ({@code QuestBookSyncS2CPacket}).
     *
     * @param questList полный список квестов игрока
     */
    public void onListSync(List<QuestBookQuestListItem> questList) {
        this.quests.clear();
        this.detailsCache.clear();
        for (var quest : questList) {
            this.quests.put(quest.questId(), quest);
        }
    }

    /**
     * Добавляет квест в список.
     * Вызывается при событии {@code QUEST_GIVEN} ({@code QuestBookQuestListItemAddedS2CPacket}).
     *
     * @param quest данные нового квеста
     */
    public void onQuestAdded(QuestBookQuestListItem quest) {
        this.quests.put(quest.questId(), quest);
    }

    /**
     * Удаляет квест из списка и инвалидирует его кэш деталей.
     * Вызывается при событии {@code QUEST_DROPPED} ({@code QuestBookQuestRemovedS2CPacket}).
     *
     * @param questId идентификатор удаляемого квеста
     */
    public void onQuestRemoved(Identifier questId) {
        this.quests.remove(questId);
        this.detailsCache.remove(questId);
    }

    /**
     * Обновляет статус завершения квеста и инвалидирует кэш его деталей.
     * Вызывается при событии {@code QUEST_COMPLETED} ({@code QuestBookQuestCompletedS2CPacket}).
     *
     * @param questId          идентификатор квеста
     * @param completionStatus новый статус завершения
     */
    public void onQuestCompleted(Identifier questId, CompletionStatus completionStatus) {
        var old = this.quests.get(questId);
        if (old == null) return;

        this.quests.put(questId, new QuestBookQuestListItem(
                old.questId(), old.title(), old.description(),
                old.icon(), completionStatus, old.index(), old.isPinned()
        ));
        // Инвалидируем кэш: задачи завершённого квеста изменили статус
        this.detailsCache.remove(questId);
    }

    /**
     * Обновляет флаг закрепления квеста в списке.
     * Вызывается при событии {@code QUEST_PINNED} / {@code QUEST_PIN_REMOVED} ({@code QuestBookQuestPinS2CPacket}).
     *
     * @param questId  идентификатор квеста
     * @param isPinned новый статус закрепления
     */
    public void onQuestPinChanged(Identifier questId, boolean isPinned) {
        var old = this.quests.get(questId);
        if (old == null) return;

        this.quests.put(questId, new QuestBookQuestListItem(
                old.questId(), old.title(), old.description(),
                old.icon(), old.completionStatus(), old.index(), isPinned
        ));
    }

    /**
     * Инвалидирует кэш деталей квеста.
     * Вызывается при получении HUD-пакетов, которые означают изменение состояния задач:
     * смена этапа, пин задачи, прогресс, загрузка/выгрузка задачи.
     *
     * @param questId идентификатор квеста
     */
    public void invalidateDetail(Identifier questId) {
        this.detailsCache.remove(questId);
    }

    /**
     * Возвращает {@code true}, если кэш деталей квеста существует и не устарел.
     *
     * @param questId идентификатор квеста
     */
    public boolean isDetailCacheFresh(Identifier questId) {
        var cached = this.detailsCache.get(questId);
        return cached != null && !cached.isExpired();
    }

    // -------------------------------------------------------------------------
    // Запросы от UI
    // -------------------------------------------------------------------------

    /**
     * Возвращает {@code true}, если квест присутствует в списке игрока.
     * Используется экраном книги для обнаружения удалённых квестов (reload, drop).
     *
     * @param questId идентификатор квеста
     */
    public boolean hasQuest(Identifier questId) {
        return this.quests.containsKey(questId);
    }

    /**
     * Возвращает список квестов игрока, отсортированный для отображения в книге квестов.
     * Порядок: активные (completionStatus == null) → завершённые; внутри группы — по index.
     *
     * @return неизменяемый отсортированный список
     */
    public List<QuestBookQuestListItem> getPlayerQuests() {
        return this.quests.values().stream()
                .sorted(Comparator
                        .comparingInt((QuestBookQuestListItem q) -> q.completionStatus() == null ? 0 : 1)
                        .thenComparingInt(QuestBookQuestListItem::index))
                .toList();
    }

    /**
     * Возвращает детали квеста, используя кэш.
     * При отсутствии или устаревании кэша отправляет C2S-запрос на сервер.
     *
     * @param questId идентификатор квеста
     * @return future, который завершится данными квеста или {@code null}, если квест не найден
     */
    public CompletableFuture<@Nullable QuestBookQuest> fetchQuestDetails(Identifier questId) {
        var cached = this.detailsCache.get(questId);
        if (cached != null && !cached.isExpired()) {
            return CompletableFuture.completedFuture(cached.value());
        }

        return ClientRequests.send(new GetQuestDetailsRequest(questId)).thenApply(response -> {
            if (response.data() != null) {
                cacheQuestDetails(questId, response.data());
            }
            return response.data();
        });
    }

    /**
     * Кладёт полученные от сервера детали квеста в кэш.
     * Вызывается обработчиком ответа на {@code GetQuestDetailsRequest}.
     *
     * @param questId идентификатор квеста
     * @param data    детали квеста (может быть {@code null}, если квест не найден)
     */
    public void cacheQuestDetails(Identifier questId, @Nullable QuestBookQuest data) {
        this.detailsCache.put(questId, CacheEntry.of(data, DETAIL_CACHE_TTL_MS));
    }
}

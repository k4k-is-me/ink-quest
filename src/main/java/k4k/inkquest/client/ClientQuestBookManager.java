package k4k.inkquest.client;

import k4k.inkquest.common.datastructures.CacheEntry;
import k4k.inkquest.common.requests.ClientRequests;
import k4k.inkquest.domain.enums.CompletionStatus;
import k4k.inkquest.infra.requests.GetQuestDetailsRequest;
import k4k.inkquest.questing.models.QuestBookQuestListItem;
import k4k.inkquest.questing.models.QuestBookQuest;
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
     * Сбрасывает кэш деталей: при повторной выдаче repeatable-квеста старый кэш
     * мог пережить дроп, и следующий fetch должен уйти на сервер для смены viewed.
     *
     * @param quest данные нового квеста
     */
    public void onQuestAdded(QuestBookQuestListItem quest) {
        this.quests.put(quest.questId(), quest);
        this.detailsCache.remove(quest.questId());
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
                old.icon(), completionStatus, old.index(), old.isPinned(), old.viewed()
        ));
        // Инвалидируем кэш: задачи завершённого квеста изменили статус
        this.detailsCache.remove(questId);
    }

    /**
     * Заменяет метаданные квеста в списке и инвалидирует кэш деталей.
     * Вызывается при событии {@code QUEST_MODIFIED} ({@code QuestBookQuestListItemUpdatedS2CPacket}).
     *
     * @param item обновлённые данные квеста
     */
    public void onQuestUpdated(QuestBookQuestListItem item) {
        this.quests.put(item.questId(), item);
        this.detailsCache.remove(item.questId());
    }

    /**
     * Обновляет флаг закрепления квеста в списке.
     * Вызывается при событии {@code QUEST_PINNED} / {@code QUEST_UNPINNED} ({@code QuestBookQuestPinS2CPacket}).
     *
     * @param questId  идентификатор квеста
     * @param isPinned новый статус закрепления
     */
    public void onQuestPinChanged(Identifier questId, boolean isPinned) {
        var old = this.quests.get(questId);
        if (old == null) return;

        this.quests.put(questId, new QuestBookQuestListItem(
                old.questId(), old.title(), old.description(),
                old.icon(), old.completionStatus(), old.index(), isPinned, old.viewed()
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
     * <p>Перед запросом оптимистично помечает квест просмотренным:
     * при cache hit сервер уже пометил viewed ранее, при cache miss — пометит
     * при обработке запроса деталей.
     *
     * @param questId идентификатор квеста
     * @return future, который завершится данными квеста или {@code null}, если квест не найден
     */
    public CompletableFuture<@Nullable QuestBookQuest> fetchQuestDetails(Identifier questId) {
        markViewedOptimistically(questId);

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
     * Оптимистично помечает квест просмотренным в клиентском списке.
     * Индикатор непросмотренного квеста исчезает мгновенно, не дожидаясь ответа сервера.
     *
     * @param questId идентификатор квеста
     */
    private void markViewedOptimistically(Identifier questId) {
        var old = this.quests.get(questId);
        if (old == null || old.viewed()) return;
        this.quests.put(questId, new QuestBookQuestListItem(
                old.questId(), old.title(), old.description(),
                old.icon(), old.completionStatus(), old.index(), old.isPinned(), true
        ));
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

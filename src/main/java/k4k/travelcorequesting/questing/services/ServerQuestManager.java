package k4k.travelcorequesting.questing.services;

import k4k.travelcorequesting.domain.abstractions.ITaskCondition;
import k4k.travelcorequesting.domain.abstractions.Quest;
import k4k.travelcorequesting.domain.models.MutableQuest;
import k4k.travelcorequesting.domain.enums.CompletionStatus;
import k4k.travelcorequesting.domain.enums.QuestPinMode;
import k4k.travelcorequesting.domain.models.taskConditions.AllCondition;
import k4k.travelcorequesting.domain.models.taskConditions.AnyCondition;
import k4k.travelcorequesting.domain.models.taskConditions.NoneCondition;
import k4k.travelcorequesting.domain.models.taskConditions.PredicateCondition;
import k4k.travelcorequesting.domain.models.taskConditions.ScoreCondition;
import k4k.travelcorequesting.questing.abstractions.IQuestRequirementChecker;
import k4k.travelcorequesting.questing.abstractions.ITaskConditionHandler;
import k4k.travelcorequesting.questing.abstractions.QuestModifier;
import k4k.travelcorequesting.questing.abstractions.QuestResolver;
import k4k.travelcorequesting.questing.events.QuestEvents;
import k4k.travelcorequesting.questing.events.QuestProgressEvents;
import k4k.travelcorequesting.questing.models.QuestEntry;
import k4k.travelcorequesting.questing.services.taskConditionTesters.AllConditionHandler;
import k4k.travelcorequesting.questing.services.taskConditionTesters.AnyConditionHandler;
import k4k.travelcorequesting.questing.services.taskConditionTesters.NoneConditionHandler;
import k4k.travelcorequesting.questing.services.taskConditionTesters.PredicateConditionHandler;
import k4k.travelcorequesting.questing.services.taskConditionTesters.ScoreConditionHandler;
import k4k.travelcorequesting.questing.states.ServerQuestManagerState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Центральный менеджер квестов на сервере.
 *
 * <p>Координирует весь жизненный цикл квестов: создание, выдачу игрокам,
 * отслеживание прогресса, проверку условий, закрепление в HUD и завершение.
 * Является единственной точкой входа для модификации состояния квестов —
 * все команды и внешние системы работают через него.
 *
 * <h2>Архитектура</h2>
 * <ul>
 *   <li>{@link QuestRepository} — хранилище квестов (static из датапаков + dynamic через команды)</li>
 *   <li>{@link PlayerProgressTracker} — трекер прогресса игрока (один на игрока)</li>
 *   <li>{@link QuestProgressTracker} — трекер прогресса конкретного квеста</li>
 *   <li>{@link TaskConditionDispatcher} — диспетчер проверки условий задач</li>
 * </ul>
 *
 * <h2>Тик</h2>
 * Каждый тик {@link #update(List)} вызывает {@link #updatePlayerQuest} для каждого
 * pinned и background квеста. Внутри: пересчёт этапа → загрузка/выгрузка задач →
 * тик условий → обновление прогресса → проверка завершения задач → проверка завершения квеста.
 *
 * <h2>События</h2>
 * Все мутации файрят события через {@link QuestEvents} и {@link QuestProgressEvents}.
 * Порядок событий задокументирован в {@code docs/СОБЫТИЯ.md}.
 *
 * <h2>Персистентность</h2>
 * Состояние сериализуется в NBT через {@link #saveState()} / {@link #loadState}.
 * Флаг {@link #isDirty} отслеживает наличие несохранённых изменений.
 *
 * @see QuestEvents
 * @see QuestProgressEvents
 */
public class ServerQuestManager {
    private final QuestRepository questRepository = new QuestRepository();
    private final ITaskConditionHandler<ITaskCondition> conditionDispatcher;  // TODO: extract instantiation
    private final IQuestRequirementChecker requirementChecker;
    private final Map<UUID, PlayerProgressTracker> trackedPlayers = new HashMap<>();
    private boolean isDirty = false;

    public ServerQuestManager(IQuestRequirementChecker requirementChecker) {
        var dispatcher = new TaskConditionDispatcher()
                .register(ScoreCondition.class, new ScoreConditionHandler())
                .register(PredicateCondition.class, new PredicateConditionHandler());
        dispatcher
                .register(AllCondition.class, new AllConditionHandler(dispatcher))
                .register(AnyCondition.class, new AnyConditionHandler(dispatcher))
                .register(NoneCondition.class, new NoneConditionHandler(dispatcher));
        this.conditionDispatcher = dispatcher;
        this.requirementChecker = requirementChecker;
    }

    /**
     * Создаёт новый динамический квест (не из датапака).
     *
     * <p>События: {@link QuestEvents#QUEST_CREATED}
     *
     * @param questId идентификатор квеста
     * @throws IllegalArgumentException если квест с таким идентификатором уже существует
     */
    public void createDynamicQuest(Identifier questId) {
        var entry = this.questRepository.createDynamicQuest(questId);
        this.isDirty = true;

        QuestEvents.QUEST_CREATED.invoker().onQuestCreation(entry);
    }

    public void removeDynamicQuest(Identifier questId) {
        Objects.requireNonNull(questId);

        this.questRepository.removeDynamicQuest(questId);
        this.isDirty = true;

        QuestEvents.QUEST_REMOVED.invoker().onQuestRemoval(questId);
    }

    // <editor-fold desc="Модификация квестов">

    /**
     * Модифицирует квест через {@link QuestModifier}. Если модификатор изменил
     * хотя бы одно поле, файрит событие.
     *
     * <p>События: {@link QuestEvents#QUEST_MODIFIED} (если были изменения)
     *
     * @param questId  идентификатор квеста
     * @param consumer операция модификации
     */
    public void modifyQuest(Identifier questId, Consumer<QuestModifier> consumer) {
        var modifier = this.questRepository.getQuestModifier(questId);
        consumer.accept(modifier);
        if (modifier.isDirty()) {
            QuestEvents.QUEST_MODIFIED.invoker().onQuestModification(this.questRepository.getQuestEntry(questId));
            this.isDirty = true;
        }
    }

    /** Есть ли несохранённые изменения с момента последнего {@link #saveState()}. */
    public boolean modifiedSinceLastSave() {
        return this.isDirty;
    }

    // </editor-fold>

    // <editor-fold desc="Работа с квестами">

    /**
     * Выдаёт квест игроку — начинает отслеживание прогресса.
     *
     * <p>Рассчитывает первый активный этап. Игнорируется, если квест уже выдан
     * или был завершён ранее.
     *
     * <p>События: {@link QuestProgressEvents#QUEST_GIVEN} →
     * {@link QuestProgressEvents#STAGE_CHANGED}
     *
     * @param questId идентификатор квеста
     * @param player  игрок
     * @throws IllegalArgumentException если квест не существует
     */
    public void giveQuest(Identifier questId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(player);

        var entry = this.questRepository.requireQuestEntry(questId);

        var playerTracker = this.trackedPlayers.computeIfAbsent(
                player.getUuid(), item -> new PlayerProgressTracker(this.questRepository));

        var questTracker = playerTracker.startTracking(questId);
        if (questTracker.isEmpty()) return;

        this.isDirty = true;

        QuestProgressEvents.QUEST_GIVEN.invoker().onQuestGive(entry, player);

        questTracker.get().recomputeActiveStage(newStage ->
                QuestProgressEvents.STAGE_CHANGED.invoker().onStageChange(entry, newStage, player));
    }

    /**
     * Снимает квест с игрока — прекращает отслеживание и удаляет весь прогресс.
     *
     * <p>Если квест был закреплён, сначала открепляет его.
     * Игнорируется, если квест не был выдан.
     *
     * <p>События: [{@link QuestEvents#QUEST_UNPINNED}] →
     * {@link QuestProgressEvents#QUEST_DROPPED}
     *
     * @param questId идентификатор квеста
     * @param player  игрок
     * @throws IllegalArgumentException если квест не существует
     */
    public void dropQuest(Identifier questId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(player);

        var entry = this.questRepository.requireQuestEntry(questId);

        var playerTracker = this.trackedPlayers.get(player.getUuid());
        if (playerTracker == null || !playerTracker.isTracked(questId)) return;

        this.pinRemove(questId, player);

        playerTracker.stopTracking(questId);
        this.isDirty = true;

        QuestProgressEvents.QUEST_DROPPED.invoker().onQuestDrop(entry, player);
    }

    /**
     * Завершает задачу вручную с указанным статусом.
     *
     * <p>Не проверяет условия — просто помечает задачу как завершённую.
     * Смена этапа и проверка завершения квеста произойдут на следующем тике
     * в {@link #updatePlayerQuest}.
     *
     * <p>События: {@link QuestProgressEvents#TASK_COMPLETED}
     *
     * @param questId идентификатор квеста
     * @param taskId  идентификатор задачи
     * @param player  игрок
     * @param status  статус завершения
     */
    public void completeTask(Identifier questId, String taskId, ServerPlayerEntity player, CompletionStatus status) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(player);

        var taskEntry = this.questRepository.getTaskEntry(questId, taskId);

        var tracker = this.getQuestTracker(player, questId).orElse(null);
        if (tracker == null) return;

        tracker.complete(taskId, status);

        QuestProgressEvents.TASK_COMPLETED.invoker().onTaskCompletion(taskEntry, player, status);
        this.isDirty = true;
    }

    /**
     * Закрепляет обязательную задачу активного этапа квеста.
     *
     * <p>Игнорируется, если у игрока нет этого квеста или квест уже завершён.
     *
     * <p>События: {@link QuestEvents#QUEST_PINNED} если квест не был закреплён,
     * {@link QuestEvents#TASK_PINNED} всегда.
     *
     * @param questId идентификатор квеста
     * @param player  игрок
     * @throws IllegalArgumentException если квест не существует
     */
    public void pinRequiredTask(Identifier questId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(player);

        var questEntry = this.questRepository.requireQuestEntry(questId);

        var tracker = this.getQuestTracker(player, questId).orElse(null);
        if (tracker == null) return;

        var activeStage = tracker.getActiveStage().orElse(null);
        if (activeStage == null) return;

        this.pinTaskInternal(questEntry, tracker, questEntry.quest().getRequiredTask(activeStage), player);
    }

    /**
     * Закрепляет конкретную задачу квеста.
     *
     * <p>Задача должна существовать в квесте. Игнорируется, если у игрока
     * нет этого квеста.
     *
     * <p>События: {@link QuestEvents#QUEST_PINNED} если квест не был закреплён,
     * {@link QuestEvents#TASK_PINNED} всегда.
     *
     * @param questId идентификатор квеста
     * @param taskId  идентификатор задачи
     * @param player  игрок
     * @throws IllegalArgumentException если квест не существует
     */
    public void pinTask(Identifier questId, String taskId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(player);

        var questEntry = this.questRepository.requireQuestEntry(questId);

        var tracker = this.getQuestTracker(player, questId).orElse(null);
        if (tracker == null) return;

        this.pinTaskInternal(questEntry, tracker, taskId, player);
    }

    /**
     * Общая логика закрепления задачи. Всегда файрит {@link QuestEvents#TASK_PINNED}.
     * Если квест не был закреплён — дополнительно файрит {@link QuestEvents#QUEST_PINNED}.
     */
    private void pinTaskInternal(QuestEntry questEntry, QuestProgressTracker tracker, String taskId, ServerPlayerEntity player) {
        boolean wasPinned = tracker.isPinned();

        tracker.setTaskPin(taskId);
        this.isDirty = true;

        if (!wasPinned) {
            QuestEvents.QUEST_PINNED.invoker().onQuestPin(questEntry, player);
        }
        QuestEvents.TASK_PINNED.invoker().onTaskPin(questEntry.questId(), taskId, player);
    }

    /**
     * Открепляет квест — убирает его из HUD игрока.
     *
     * <p>Игнорируется, если квест не закреплён или не выдан.
     *
     * <p>События: {@link QuestEvents#QUEST_UNPINNED}
     *
     * @param questId идентификатор квеста
     * @param player  игрок
     */
    public void pinRemove(Identifier questId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(player);

        var tracker = this.getQuestTracker(player, questId).orElse(null);
        if (tracker == null || !tracker.isPinned()) return;

        tracker.resetTaskPin();

        QuestEvents.QUEST_UNPINNED.invoker().onQuestUnpin(questId, player);
        this.isDirty = true;
    }

    // </editor-fold>

    // <editor-fold desc="Получение информации о квестах">

    /** Зарегистрирован ли квест (static или dynamic). */
    public boolean isQuestExists(Identifier questId) {
        Objects.requireNonNull(questId);

        return this.questRepository.getQuest(questId) != null;
    }

    /** Загружен ли квест из датапака (а не создан динамически). */
    public boolean isQuestStatic(Identifier questId) {
        Objects.requireNonNull(questId);

        return this.questRepository.isQuestStatic(questId);
    }

    /** Все зарегистрированные квесты (static + dynamic). */
    public List<QuestEntry> getRegisteredQuests() {
        return this.questRepository.getQuestIds().stream()
                .map(this.questRepository::getQuestEntry)
                .toList();
    }

    /** Квесты, загруженные из датапаков. */
    public List<QuestEntry> getStaticQuests() {
        return this.questRepository.getStaticQuestIds().stream()
                .map(this.questRepository::getQuestEntry)
                .toList();
    }

    /** Квесты, созданные динамически через {@link #createDynamicQuest}. */
    public List<QuestEntry> getDynamicQuests() {
        return this.questRepository.getDynamicQuestIds().stream()
                .map(this.questRepository::getQuestEntry)
                .toList();
    }

    /** Все квесты игрока — активные и завершённые. */
    public List<QuestEntry> getTrackedQuests(ServerPlayerEntity player) {
        Objects.requireNonNull(player);
        return this.getPlayerTracker(player)
                .map(tracker -> tracker.getTrackedQuests().stream()
                        .map(this.questRepository::getQuestEntry)
                        .toList())
                .orElseGet(ArrayList::new);
    }

    /** Идентификаторы всех квестов игрока — активных и завершённых. */
    public List<Identifier> getTrackedQuestIds(ServerPlayerEntity player) {
        Objects.requireNonNull(player);
        return this.getPlayerTracker(player)
                .map(PlayerProgressTracker::getTrackedQuests)
                .orElseGet(ArrayList::new);
    }

    /** Резолвер для чтения квестов и задач по идентификатору. */
    public QuestResolver getQuestResolver() {
        return this.questRepository;
    }

    /** Активные (ещё не завершённые) квесты игрока. */
    public List<Quest> getActiveQuests(ServerPlayerEntity player) {
        Objects.requireNonNull(player);

        var playerProgressTracker = this.trackedPlayers.get(player.getUuid());
        if (playerProgressTracker == null) return new ArrayList<>();

        return playerProgressTracker.getActiveQuests().stream()
                .map(this.questRepository::getQuest)
                .filter(Objects::nonNull)
                .toList();
    }

    /** Все завершённые квесты игрока (любой статус). */
    public List<QuestEntry> getCompleteQuests(ServerPlayerEntity player) {
        Objects.requireNonNull(player);

        var playerProgressTracker = this.trackedPlayers.get(player.getUuid());
        if (playerProgressTracker == null) return new ArrayList<>();

        return playerProgressTracker.getCompleteQuests().stream()
                .map(this.questRepository::getQuestEntry)
                .filter(Objects::nonNull)
                .toList();
    }

    /** Завершённые квесты игрока с указанным статусом. */
    public List<Quest> getCompleteQuests(ServerPlayerEntity player, CompletionStatus status) {
        Objects.requireNonNull(player);
        Objects.requireNonNull(status);

        var playerTracker = this.trackedPlayers.get(player.getUuid());
        if (playerTracker == null) return new ArrayList<>();

        return playerTracker.getCompleteQuests().stream()
                .filter(questId -> playerTracker.getCompletionStatus(questId).map(questStatus -> questStatus == status).orElse(false))
                .map(this.questRepository::getQuest)
                .filter(Objects::nonNull)
                .toList();
    }

    /** Выдан ли квест хотя бы одному игроку (активный или завершённый). */
    public boolean isQuestTrackedByAnyone(Identifier questId) {
        return this.trackedPlayers.values().stream()
                .anyMatch(tracker -> tracker.isTracked(questId));
    }

    /**
     * Silently drops quest from all player trackers without firing any events.
     * Use this for bulk offline cleanup — proper online drops should be done
     * beforehand via {@link #dropQuest}.
     *
     * @return number of trackers cleaned up
     */
    public int dropQuestFromAllTrackers(Identifier questId) {
        Objects.requireNonNull(questId);

        var count = 0;
        for (var tracker : this.trackedPlayers.values()) {
            if (!tracker.isTracked(questId)) continue;
            tracker.stopTracking(questId);
            this.isDirty = true;
            count++;
        }
        return count;
    }

    /** Выдан ли квест игроку (активный или завершённый). */
    public boolean isQuestTracked(Identifier questId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(player);

        return this.getPlayerTracker(player)
                .map(tracker -> tracker.isTracked(questId))
                .orElse(false);
    }

    /** Активен ли квест (выдан и ещё не завершён). */
    public boolean isQuestActive(Identifier questId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(player);

        return this.getPlayerTracker(player)
                .map(tracker -> tracker.isActive(questId))
                .orElse(false);
    }

    /** Завершён ли квест (любой статус). */
    public boolean isQuestComplete(Identifier questId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(player);

        return this.getPlayerTracker(player)
                .map(tracker -> tracker.isComplete(questId))
                .orElse(false);
    }

    /** Завершён ли квест с указанным статусом. */
    public boolean isQuestComplete(Identifier questId, ServerPlayerEntity player, CompletionStatus status) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(player);
        Objects.requireNonNull(status);

        return this.getPlayerTracker(player)
                .flatMap(tracker -> tracker.getCompletionStatus(questId))
                .map(questStatus -> questStatus == status)
                .orElse(false);
    }

    /** Статус завершения квеста для игрока; {@link Optional#empty()} — если квест активен или не выдан. */
    public Optional<CompletionStatus> getQuestCompletionStatus(Identifier questId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(player);

        return this.getPlayerTracker(player)
                .flatMap(tracker -> tracker.getCompletionStatus(questId));
    }

    public Optional<CompletionStatus> getTaskCompletionStatus(Identifier questId, String taskId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(player);

        return this.getQuestTracker(player, questId)
                .flatMap(tracker -> tracker.getCompletionStatus(taskId));
    }

    /** Завершён ли квест успешно. */
    public boolean isQuestSucceeded(Identifier questId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(player);

        return this.isQuestComplete(questId, player, CompletionStatus.SUCCESS);
    }

    /** Завершён ли квест провалом. */
    public boolean isQuestFailed(Identifier questId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(player);

        return this.isQuestComplete(questId, player, CompletionStatus.FAILURE);
    }

    /** Завершён ли квест пропуском. */
    public boolean isQuestSkipped(Identifier questId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(player);

        return this.isQuestComplete(questId, player, CompletionStatus.SKIPPED);
    }

    /** Закреплён ли квест (имеет pinned задачу). */
    public boolean isQuestPinned(Identifier questId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(player);

        return this.getQuestTracker(player, questId)
                .map(QuestProgressTracker::isPinned)
                .orElse(false);
    }

    /** Активна ли задача (находится в активном этапе и не завершена). */
    public boolean isTaskActive(Identifier questId, String taskId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(player);

        return this.getQuestTracker(player, questId)
                .map(questTracker -> questTracker.isActive(taskId))
                .orElse(false);
    }

    /** Завершена ли задача (любой статус). */
    public boolean isTaskComplete(Identifier questId, String taskId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(player);

        return this.getQuestTracker(player, questId)
                .map(questTracker -> questTracker.isComplete(taskId))
                .orElse(false);
    }

    /** Завершена ли задача с указанным статусом. */
    public boolean isTaskComplete(Identifier questId, String taskId, ServerPlayerEntity player, CompletionStatus status) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(player);
        Objects.requireNonNull(status);

        return this.getQuestTracker(player, questId)
                .flatMap(questTracker -> questTracker.getCompletionStatus(taskId))
                .map(taskStatus -> taskStatus == status)
                .orElse(false);
    }

    /** Завершена ли задача успешно. */
    public boolean isTaskSucceeded(Identifier questId, String taskId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(player);

        return this.isTaskComplete(questId, taskId, player, CompletionStatus.SUCCESS);
    }

    /** Завершена ли задача провалом. */
    public boolean isTaskFailed(Identifier questId, String taskId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(player);

        return this.isTaskComplete(questId, taskId, player, CompletionStatus.FAILURE);
    }

    /** Завершена ли задача пропуском. */
    public boolean isTaskSkipped(Identifier questId, String taskId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(player);

        return this.isTaskComplete(questId, taskId, player, CompletionStatus.SKIPPED);
    }

    /** Возвращает идентификатор закреплённой задачи квеста для игрока. */
    public Optional<String> getPinnedTaskId(Identifier questId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(player);

        return this.getQuestTracker(player, questId)
                .flatMap(QuestProgressTracker::getTaskPin);
    }

    /** Закреплена ли конкретная задача. */
    public boolean isTaskPinned(Identifier questId, String taskId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(player);

        return this.getQuestTracker(player, questId)
                .map(tracker -> tracker.isPinned(taskId))
                .orElse(false);
    }

    /** Номер активного этапа (по сути — число пройдённых этапов). 0 если квест не выдан. */
    public int getStagesComplete(Identifier questId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(player);

        return this.getQuestTracker(player, questId)
                .flatMap(QuestProgressTracker::getActiveStage)
                .orElse(0);
    }

    /** Активный этап квеста. Пуст если квест не выдан или завершён. */
    public Optional<Integer> getActiveStage(Identifier questId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(player);

        return this.getQuestTracker(player, questId)
                .flatMap(QuestProgressTracker::getActiveStage);
    }

    /** Число завершённых задач в указанном этапе. */
    public int getTasksComplete(Identifier questId, int stage, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(player);

        var questTracker = this.getQuestTracker(player, questId).orElse(null);
        if (questTracker == null) return 0;

        var quest = this.questRepository.getQuest(questId);
        if (quest == null) return 0;

        return (int) quest.getStage(stage).stream()
                .map(questTracker::isComplete)
                .filter(result -> result)
                .count();
    }

    /** Число завершённых задач в активном этапе. 0 если квест не выдан или завершён. */
    public int getTasksComplete(Identifier questId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(player);

        var questTracker = this.getQuestTracker(player, questId).orElse(null);
        if (questTracker == null) return 0;

        var quest = this.questRepository.getQuest(questId);
        if (quest == null) return 0;

        var activeStage = questTracker.getActiveStage().orElse(null);
        if (activeStage == null) return 0;

        return (int) quest.getStage(activeStage).stream()
                .map(questTracker::isComplete)
                .filter(result -> result)
                .count();
    }

    /** Текущее значение прогресса условия успеха задачи. 0 если задача не найдена. */
    public int getTaskSuccessCompletion(Identifier questId, String taskId, ServerPlayerEntity player) {
        var task = this.questRepository.getTask(questId, taskId);
        if (task == null) return 0;
        return this.conditionDispatcher.getCurrentValue(task.successCondition(), player);
    }

    /** Целевое значение условия успеха задачи. 1 если задача или условие не найдены. */
    public int getTaskSuccessTarget(Identifier questId, String taskId) {
        var task = this.questRepository.getTask(questId, taskId);
        if (task == null) return 1;
        var condition = task.successCondition();
        if (condition == null) return 1;
        return condition.getTargetValue();
    }

    /** Текущее значение прогресса условия провала задачи. 0 если задача не найдена. */
    public int getTaskFailureCompletion(Identifier questId, String taskId, ServerPlayerEntity player) {
        var task = this.questRepository.getTask(questId, taskId);
        if (task == null) return 0;
        return this.conditionDispatcher.getCurrentValue(task.failureCondition(), player);
    }

    /** Целевое значение условия провала задачи. 1 если задача или условие не найдены. */
    public int getTaskFailureTarget(Identifier questId, String taskId) {
        var task = this.questRepository.getTask(questId, taskId);
        if (task == null) return 1;
        var condition = task.failureCondition();
        if (condition == null) return 1;
        return condition.getTargetValue();
    }

    // </editor-fold>

    /**
     * Тик менеджера — обновляет прогресс всех pinned и background квестов для каждого игрока.
     * Должен вызываться каждый серверный тик.
     */
    public void update(List<ServerPlayerEntity> players) {
        for (var player : players) {
            this.updatePlayer(player);
        }
    }

    private void updatePlayer(ServerPlayerEntity player) {
        var playerTracker = this.trackedPlayers.get(player.getUuid());
        if (playerTracker == null) return;

        for (var questId : playerTracker.getActiveQuests()) {
            var entry = this.questRepository.getQuestEntry(questId);
            if (entry == null) continue;

            var questTracker = playerTracker.getQuestTracker(questId).orElse(null);
            if (questTracker == null) continue;

            if (!entry.quest().background() && !questTracker.isPinned()) continue;

            this.updatePlayerQuest(player, playerTracker, questTracker, entry);
        }
    }

    /**
     * Обновляет один квест одного игрока за тик.
     *
     * <p>Полный цикл: пересчёт этапа → загрузка/выгрузка задач → тик условий →
     * обновление прогресса → завершение задач → проверка завершения квеста.
     * Порядок событий описан в {@code docs/СОБЫТИЯ.md}.
     *
     * <p>Вызывается только для активных (незавершённых) квестов.
     */
    private void updatePlayerQuest(
            ServerPlayerEntity player,
            PlayerProgressTracker playerTracker,
            QuestProgressTracker questTracker,
            QuestEntry entry
    ) {
        var questId = entry.questId();

        var stageChanged = questTracker.recomputeActiveStage(newStage ->
                QuestProgressEvents.STAGE_CHANGED.invoker().onStageChange(entry, newStage, player));

        if (stageChanged && questTracker.isPinned())
            questTracker.getActiveStage()
                    .map(entry.quest()::getRequiredTask)
                    .ifPresent(questTracker::setTaskPin);

        this.ensureActiveStageLoaded(player, questTracker, stageChanged);
        if (questTracker.getActiveStage().isPresent()) {
            this.processActiveTasks(player, questId, questTracker);
        }

        boolean wasPinned = questTracker.isPinned();

        playerTracker.checkCompletion(questId, status -> {
            if (wasPinned) {
                QuestEvents.QUEST_UNPINNED.invoker().onQuestUnpin(questId, player);
            }
            QuestProgressEvents.QUEST_COMPLETED.invoker().onQuestCompletion(entry, status, player);
            this.unlockDependentQuests(player, playerTracker, questId, wasPinned);
            this.isDirty = true;
        });
    }

    /**
     * Тикает условия, обновляет прогресс и проверяет завершение каждой активной задачи квеста.
     *
     * <p>Вызывается только когда у квеста есть активный этап. Не вызывается если активного
     * этапа нет — например после ручного завершения последней задачи командой между тиками.
     * В этом случае завершение квеста обнаружится через {@link PlayerProgressTracker#checkCompletion}
     * на том же тике.
     */
    private void processActiveTasks(ServerPlayerEntity player, Identifier questId, QuestProgressTracker questTracker) {
        for (var taskId : questTracker.getActiveTasks()) {
            var taskEntry = this.questRepository.getTaskEntry(questId, taskId);
            if (taskEntry == null) continue;

            var task = taskEntry.task();

            this.conditionDispatcher.tick(task.successCondition(), player);
            this.conditionDispatcher.tick(task.failureCondition(), player);
            QuestProgressEvents.TASK_TICKED.invoker().onTaskTick(taskEntry, player);

            var successValue = this.conditionDispatcher.getCurrentValue(task.successCondition(), player);
            var failureValue = this.conditionDispatcher.getCurrentValue(task.failureCondition(), player);

            var successChanged = questTracker.updateSuccessValue(taskId, successValue);

            if (successChanged) {
                QuestProgressEvents.TASK_SUCCESS_PROGRESS_CHANGED.invoker()
                        .onTaskProgressChange(taskEntry, player, successValue);
                this.isDirty = true;
            }

            var failureChanged = questTracker.updateFailureValue(taskId, failureValue);

            if (failureChanged) {
                QuestProgressEvents.TASK_FAILURE_PROGRESS_CHANGED.invoker()
                        .onTaskProgressChange(taskEntry, player, failureValue);
                this.isDirty = true;
            }

            if (successChanged && this.conditionDispatcher.test(task.successCondition(), player)) {
                this.completeTask(questId, taskId, player, CompletionStatus.SUCCESS);
            } else if (failureChanged && this.conditionDispatcher.test(task.failureCondition(), player)) {
                this.completeTask(questId, taskId, player, CompletionStatus.FAILURE);
            }
        }
    }

    /**
     * Удостоверяется, что все задачи активного этапа загружены. Загружает то что не загружено и выгружает то,
     * что уже не нужно. Отправляет соответствующие события. Вызывается безусловно — в том числе когда activeStage
     * равен null, чтобы выгрузить задачи последнего этапа при завершении квеста.
     * @param player Игрок
     * @param questTracker Трекер квеста
     * @param stageChanged Сменился ли активный этап в этом тике
     */
    private void ensureActiveStageLoaded(ServerPlayerEntity player, QuestProgressTracker questTracker, boolean stageChanged) {
        var loadedTasks = new HashSet<>(questTracker.getLoadedTasks());
        questTracker.loadActiveStage(
                task -> this.conditionDispatcher.getCurrentValue(task.successCondition(), player),
                task -> this.conditionDispatcher.getCurrentValue(task.failureCondition(), player)
        );

        var shouldBeLoaded = questTracker.getActiveTasks();

        // Unload tasks that are loaded but not present in the current stage
        loadedTasks.stream().filter(taskId -> !shouldBeLoaded.contains(taskId)).forEach(taskId -> {
            var taskEntry = this.questRepository.getTaskEntry(questTracker.getQuestId(), taskId);
            if (taskEntry == null) return;

            QuestProgressEvents.TASK_UNLOADED.invoker().onTaskUnload(taskEntry, player, stageChanged);
            questTracker.markUnloaded(taskId);
        });

        // Load tasks in current stage that are not loaded
        shouldBeLoaded.stream().filter(taskId -> !loadedTasks.contains(taskId)).forEach(taskId -> {
            var taskEntry = this.questRepository.getTaskEntry(questTracker.getQuestId(), taskId);
            if (taskEntry == null) return;

            this.conditionDispatcher.load(taskEntry.task().successCondition(), player);
            this.conditionDispatcher.load(taskEntry.task().failureCondition(), player);

            QuestProgressEvents.TASK_LOADED.invoker().onTaskLoad(taskEntry, player, stageChanged);
            questTracker.markLoaded(taskId);
        });
    }

    /**
     * Проверяет и выдаёт квесты, зависящие от завершённого квеста.
     * Квест выдаётся, если все квесты хотя бы в одной группе зависимостей завершены успешно.
     * Закрепление выданного квеста определяется его {@link QuestPinMode}:
     * {@code FORCE} — всегда закрепить, {@code AUTO} — закрепить если родительский был закреплён,
     * {@code OFF} — не закреплять.
     */
    private void unlockDependentQuests(ServerPlayerEntity player, PlayerProgressTracker playerTracker, Identifier questId, boolean wasParentPinned) {
        for (var dependentQuestId : this.questRepository.getDependentQuests(questId)) {
            var dependentQuest = this.questRepository.getQuest(dependentQuestId);
            if (dependentQuest == null) continue;

            for (var groupIndex = 0; groupIndex < dependentQuest.getDependencyGroupsCount(); groupIndex++) {
                var isGroupComplete = dependentQuest.getDependencyGroup(groupIndex).stream()
                        .allMatch(depId -> playerTracker.isComplete(depId, CompletionStatus.SUCCESS));

                if (!isGroupComplete)
                    continue;

                var require = dependentQuest.getRequire();
                if (require != null && !this.requirementChecker.check(require, player)) break;

                this.giveQuest(dependentQuestId, player);

                boolean shouldPin = switch (dependentQuest.getPinMode()) {
                    case FORCE -> true;
                    case AUTO -> wasParentPinned;
                    case OFF -> false;
                };
                if (shouldPin) this.pinRequiredTask(dependentQuestId, player);

                break;
            }
        }
    }

    /** Трекер прогресса игрока. Пуст если игрок не отслеживается. */
    private Optional<PlayerProgressTracker> getPlayerTracker(ServerPlayerEntity player) {
        return Optional.ofNullable(this.trackedPlayers.get(player.getUuid()));
    }

    /** Трекер прогресса квеста для игрока. Пуст если квест не выдан или уже завершён. */
    private Optional<QuestProgressTracker> getQuestTracker(ServerPlayerEntity player, Identifier questId) {
        return this.getPlayerTracker(player)
                .flatMap(tracker -> tracker.getQuestTracker(questId));
    }

    /**
     * Восстанавливает состояние менеджера из NBT (при загрузке мира).
     * Полностью заменяет динамические квесты и трекеры игроков.
     */
    public void loadState(ServerQuestManagerState state) {
        this.trackedPlayers.clear();

        this.questRepository.replaceDynamicQuests(state.dynamicQuests());

        for (var playerTrackerStateEntry : state.playerTrackers().entrySet()) {
            var playerTracker = PlayerProgressTracker.create(this.questRepository, playerTrackerStateEntry.getValue());
            this.trackedPlayers.put(playerTrackerStateEntry.getKey(), playerTracker);
        }
    }

    /** Сериализует текущее состояние для сохранения в NBT. Сбрасывает {@link #isDirty}. */
    public ServerQuestManagerState saveState() {
        this.isDirty = false;

        return new ServerQuestManagerState(
                Collections.unmodifiableMap(this.trackedPlayers.entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().saveState()
                ))),
                this.questRepository.getDynamicQuestIds().stream().collect(Collectors.toUnmodifiableMap(
                        Function.identity(),
                        id -> (MutableQuest) this.questRepository.requireQuest(id)
                ))
        );
    }

    /**
     * Загружает квесты из датапаков. Полностью заменяет все статические квесты.
     *
     * <p>События: {@link QuestEvents#QUESTS_RELOADED}
     */
    public void loadQuests(Map<Identifier, Quest> quests) {
        this.questRepository.replaceStaticQuests(quests);
        QuestEvents.QUESTS_RELOADED.invoker().onReload();
    }
}

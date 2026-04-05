package k4k.travelcorequesting.questing.services;

import k4k.travelcorequesting.domain.abstractions.ITaskCondition;
import k4k.travelcorequesting.domain.abstractions.Quest;
import k4k.travelcorequesting.domain.enums.CompletionStatus;
import k4k.travelcorequesting.domain.models.taskConditions.AllCondition;
import k4k.travelcorequesting.domain.models.taskConditions.PredicateCondition;
import k4k.travelcorequesting.domain.models.taskConditions.ScoreCondition;
import k4k.travelcorequesting.questing.abstractions.ITaskConditionHandler;
import k4k.travelcorequesting.questing.abstractions.QuestModifier;
import k4k.travelcorequesting.questing.abstractions.QuestResolver;
import k4k.travelcorequesting.questing.events.QuestEvents;
import k4k.travelcorequesting.questing.events.QuestProgressEvents;
import k4k.travelcorequesting.questing.models.QuestEntry;
import k4k.travelcorequesting.questing.services.taskConditionTesters.AllConditionHandler;
import k4k.travelcorequesting.questing.services.taskConditionTesters.PredicateConditionHandler;
import k4k.travelcorequesting.questing.services.taskConditionTesters.ScoreConditionHandler;
import k4k.travelcorequesting.questing.states.ServerQuestManagerState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

// Трекеры должны хранить идентификаторы и ресолвить их только при необходимости из общего реестра
// Иначе трекер сможет работать только с загруженными квестами и из-за этого весь прогресс по не загруженным
// квестам будет потерян при сохранении

// Так и так придётся держать общий реестр загруженных задач (если не придумать чего-то лучше), тк. квесты
// могут быть загружены из датапака, в уже выданном квесте в активном этапе может появиться задача, которая не
// будет загружена.
// РЕШЕНИЕ: Добавить в QuestProgress активный этап. Если активный этап не соответствует рассчитанному - разгрузить
//   все загруженные задачи и загрузить задачи рассчитанного этапа, установить этап на рассчитанный.
//   При обновлении проверять появились ли в активном этапе квеста загруженные задачи, если появились - загрузить.
//   Для оптимизации можно как-то трекать изменения квестов
// ОТМЕНА: Загрузка из датапака будет работать напрямую с ServerQuestManager-ом и он сможет отследить изменения

// Я готов смириться с задержкой в 1 тик между изменением квеста и загрузкой задачи. Борьба с этой проблемой всё сильно
//   усложняет, плюс, если задача появилась в активном этапе квеста для игрока, которого пока нет на сервере,
//   загрузку для него нужно будет совершить как он зайдёт. Ре

public class ServerQuestManager {
    private final QuestRepository questRepository = new QuestRepository();
    private final ITaskConditionHandler<ITaskCondition> conditionDispatcher;  // TODO: extract instantiation
    private final Map<UUID, PlayerProgressTracker> trackedPlayers = new HashMap<>();
    private boolean isDirty = false;

    public ServerQuestManager() {
        var dispatcher = new TaskConditionDispatcher()
                .register(ScoreCondition.class, new ScoreConditionHandler())
                .register(PredicateCondition.class, new PredicateConditionHandler());
        dispatcher
                .register(AllCondition.class, new AllConditionHandler(dispatcher));
        this.conditionDispatcher = dispatcher;
    }

    /**
     * Создаёт новый квест. Ошибка, если квест уже существует
     * @param questId Идентификатор
     */
    public void createDynamicQuest(Identifier questId) {
        var entry = this.questRepository.createDynamicQuest(questId);
        this.isDirty = true;

        QuestEvents.QUEST_CREATED.invoker().onQuestCreation(entry);
    }

    // <editor-fold desc="Модификация квестов">

    public void modifyQuest(Identifier questId, Consumer<QuestModifier> consumer) {
        var modifier = this.questRepository.getQuestModifier(questId);
        consumer.accept(modifier);
        if (modifier.isDirty()) {
            QuestEvents.QUEST_MODIFIED.invoker().onQuestModification(this.questRepository.getQuestEntry(questId));
            this.isDirty = true;
        }
    }

    public boolean modifiedSinceLastSave() {
        return this.isDirty;
    }

    // </editor-fold>

    // <editor-fold desc="Работа с квестами">

    /**
     * Начинает отслеживание прогресса по квесту для игрока. Ошибка, если квеста не существует
     * @param questId Идентификатор квеста
     * @param player Игрок
     */
    public void giveQuest(Identifier questId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(player);

        var entry = this.questRepository.requireQuestEntry(questId);  // Квест трекер может отслеживать выполнение и не существующих квестов, но при
            // выдаче квеста такая возможность не особо имеет смысл

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
     * Прекращает отслеживание квеста для игрока. Ошибка, если квеста не существует
     * @param questId Идентификатор квеста
     * @param player Игрок
     */
    public void dropQuest(Identifier questId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(player);

        var entry = this.questRepository.requireQuestEntry(questId);

        var playerTracker = this.trackedPlayers.get(player.getUuid());
        if (playerTracker == null) return;
        if (!playerTracker.has(questId)) return;

        this.unpinIfPinned(questId, player, playerTracker);

        playerTracker.stopTracking(questId);
        this.isDirty = true;

        QuestProgressEvents.QUEST_DROPPED.invoker().onQuestDrop(entry, player);
    }

    /**
     * Выполняет указанную задачу. Ошибка, если квеста или задачи не существует
     * @param questId Идентификатор квеста
     * @param taskId Идентификатор задачи
     * @param player Игрок
     * @param status Статус выполнения
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
     * Закрепляет обязательную задачу активного этапа квеста. Ошибка, если квеста не существует или выполнен
     * @param questId Идентификатор квеста
     * @param player Игрок
     */
    public void pinRequiredTask(Identifier questId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(player);

        var questEntry = this.questRepository.requireQuestEntry(questId);

        var tracker = this.getQuestTracker(player, questId).orElse(null);
        if (tracker == null) return;

        var activeStage = tracker.getActiveStage().orElse(null);
        if (activeStage == null) return;

        tracker.setTaskPin(questEntry.quest().getRequiredTask(activeStage));

        QuestEvents.QUEST_PINNED.invoker().onQuestPin(questEntry, player);
        this.isDirty = true;
    }

    /**
     * Закрепляет задачу активного этапа квеста. Ошибка, если квеста не существует, выполнен или не существует,
     * выполнена или не является частью активного этапа задача
     * @param questId Идентификатор квеста
     * @param taskId Идентификатор задачи
     * @param player Игрок
     */
    public void pinTask(Identifier questId, String taskId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(player);

        var questEntry = this.questRepository.requireQuestEntry(questId);

        var tracker = this.getQuestTracker(player, questId).orElse(null);
        if (tracker == null) return;

        tracker.setTaskPin(taskId);

        QuestEvents.QUEST_PINNED.invoker().onQuestPin(questEntry, player);
        this.isDirty = true;
    }

    /**
     * Открепить активный пин
     * @param player Игрок
     */
    public void pinRemove(Identifier questId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(player);

        var tracker = this.getQuestTracker(player, questId).orElse(null);
        if (tracker == null) return;

        tracker.resetTaskPin();

        QuestEvents.QUEST_PIN_REMOVED.invoker().onQuestPinRemove(questId, player);
        this.isDirty = true;
    }

    // </editor-fold>

    // <editor-fold desc="Получение информации о квестах">

    /**
     * Возвращает true если квест с таким идентификатором зарегистрирован и загружен
     */
    public boolean isQuestExists(Identifier questId) {
        Objects.requireNonNull(questId);

        return this.questRepository.getQuest(questId) != null;
    }

    /**
     * Возвращает true если квест статический (загружен из датапака)
     */
    public boolean isQuestStatic(Identifier questId) {
        Objects.requireNonNull(questId);

        return this.questRepository.isQuestStatic(questId);
    }

    /**
     * Получить список всех зарегистрированных квестов
     */
    public List<QuestEntry> getRegisteredQuests() {
        return this.questRepository.getQuestIds().stream()
                .map(this.questRepository::getQuestEntry)
                .toList();
    }

    /**
     * Получить список всех квестов загруженных из датапаков
     */
    public List<QuestEntry> getStaticQuests() {
        return this.questRepository.getStaticQuestIds().stream()
                .map(this.questRepository::getQuestEntry)
                .toList();
    }

    /**
     * Получить список всех динамически созданных квестов
     */
    public List<QuestEntry> getDynamicQuests() {
        return this.questRepository.getDynamicQuestIds().stream()
                .map(this.questRepository::getQuestEntry)
                .toList();
    }

    public List<QuestEntry> getTrackedQuests(ServerPlayerEntity player) {
        Objects.requireNonNull(player);
        return this.getPlayerTracker(player)
                .map(tracker -> tracker.getTrackedQuests().stream()
                        .map(this.questRepository::getQuestEntry)
                        .toList())
                .orElseGet(ArrayList::new);
    }

    public List<Identifier> getTrackedQuestIds(ServerPlayerEntity player) {
        Objects.requireNonNull(player);
        return this.getPlayerTracker(player)
                .map(PlayerProgressTracker::getTrackedQuests)
                .orElseGet(ArrayList::new);
    }

    public QuestResolver getQuestResolver() {
        return this.questRepository;
    }

    /**
     * Получить список активных квестов игрока
     * @param player Игрок
     * @return Список квестов
     */
    public List<Quest> getActiveQuests(ServerPlayerEntity player) {
        Objects.requireNonNull(player);

        var playerProgressTracker = this.trackedPlayers.get(player.getUuid());
        if (playerProgressTracker == null) return new ArrayList<>();

        return playerProgressTracker.getActiveQuests().stream()
                .map(this.questRepository::getQuest)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Получить список всех выполненных квестов игрока
     * @param player Игрок
     * @return Список квестов
     */
    public List<QuestEntry> getCompleteQuests(ServerPlayerEntity player) {
        Objects.requireNonNull(player);

        var playerProgressTracker = this.trackedPlayers.get(player.getUuid());
        if (playerProgressTracker == null) return new ArrayList<>();

        return playerProgressTracker.getCompleteQuests().stream()
                .map(this.questRepository::getQuestEntry)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Получить список успешно или не успешно выполненных квестов игрока
     * @param player Игрок
     * @param status Статус
     * @return Список квестов
     */
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

    /**
     * Проверить, отслеживается ли квест для игрока
     * @param questId Идентификатор квеста
     * @param player Игрок
     * @return Отслеживается ли квест
     */
    public boolean isQuestTracked(Identifier questId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(player);

        return this.getPlayerTracker(player)
                .map(tracker -> tracker.isTracked(questId))
                .orElse(false);
    }

    /**
     * Проверить, активен ли квест для игрока
     * @param questId Идентификатор квеста
     * @param player Игрок
     * @return Отслеживается ли квест
     */
    public boolean isQuestActive(Identifier questId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(player);

        return this.getPlayerTracker(player)
                .map(tracker -> tracker.isActive(questId))
                .orElse(false);
    }

    /**
     * Проверить, выполнен ли квест игроком
     * @param questId Идентификатор квеста
     * @param player Игрок
     * @return Выполнен ли квест
     */
    public boolean isQuestComplete(Identifier questId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(player);

        return this.getPlayerTracker(player)
                .map(tracker -> tracker.isComplete(questId))
                .orElse(false);
    }

    /**
     * Проверить, выполнен ли квест с определённым результатом игроком
     * @param questId Идентификатор квеста
     * @param player Игрок
     * @param status Статус
     * @return Выполнен ли квест
     */
    public boolean isQuestComplete(Identifier questId, ServerPlayerEntity player, CompletionStatus status) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(player);
        Objects.requireNonNull(status);

        return this.getPlayerTracker(player)
                .flatMap(tracker -> tracker.getCompletionStatus(questId))
                .map(questStatus -> questStatus == status)
                .orElse(false);
    }

    /**
     * Проверить выполнен ли квест успешно
     */
    public boolean isQuestSucceeded(Identifier questId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(player);

        return this.isQuestComplete(questId, player, CompletionStatus.SUCCESS);
    }

    /**
     * Проверить выполнен ли квест неудачно
     */
    public boolean isQuestFailed(Identifier questId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(player);

        return this.isQuestComplete(questId, player, CompletionStatus.FAILURE);
    }

    /**
     * Проверить выполнен ли квест неудачно
     */
    public boolean isQuestSkipped(Identifier questId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(player);

        return this.isQuestComplete(questId, player, CompletionStatus.SKIPPED);
    }

    /**
     * Проверить, закреплён ли квест игроком
     * @param questId Идентификатор квеста
     * @param player Игрок
     * @return Закреплён ли квест
     */
    public boolean isQuestPinned(Identifier questId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(player);

        return this.getQuestTracker(player, questId)
                .map(QuestProgressTracker::isPinned)
                .orElse(false);
    }

    public boolean hasQuest(ServerPlayerEntity player, Identifier questId) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(player);

        var playerTracker = this.trackedPlayers.get(player.getUuid());
        if (playerTracker == null) return false;

        return playerTracker.has(questId);
    }

    /**
     * Проверить, отслеживается ли задача для игрока
     * @param questId Идентификатор квеста
     * @param taskId Идентификатор задачи
     * @param player Игрок
     * @return Отслеживается ли квест
     */
    public boolean isTaskActive(Identifier questId, String taskId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(player);

        return this.getQuestTracker(player, questId)
                .map(questTracker -> questTracker.isActive(taskId))
                .orElse(false);
    }

    /**
     * Проверить, выполнена ли задача игроком
     * @param questId Идентификатор квеста
     * @param taskId Идентификатор задачи
     * @param player Игрок
     * @return Выполнена ли задача
     */
    public boolean isTaskComplete(Identifier questId, String taskId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(player);

        return this.getQuestTracker(player, questId)
                .map(questTracker -> questTracker.isComplete(taskId))
                .orElse(false);
    }

    /**
     * Проверить, выполнена ли задача с определённым статусом игроком
     * @param questId Идентификатор квеста
     * @param taskId Идентификатор задачи
     * @param player Игрок
     * @param status Статус
     * @return Выполнена ли задача
     */
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

    public boolean isTaskSucceeded(Identifier questId, String taskId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(player);

        return this.isTaskComplete(questId, taskId, player, CompletionStatus.SUCCESS);
    }

    public boolean isTaskFailed(Identifier questId, String taskId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(player);

        return this.isTaskComplete(questId, taskId, player, CompletionStatus.FAILURE);
    }

    public boolean isTaskSkipped(Identifier questId, String taskId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(player);

        return this.isTaskComplete(questId, taskId, player, CompletionStatus.SKIPPED);
    }

    /**
     * Проверить, закреплена ли задача игроком
     * @param questId Идентификатор квеста
     * @param taskId Идентификатор задачи
     * @param player Игрок
     * @return Выполнена ли задача
     */
    public boolean isTaskPinned(Identifier questId, String taskId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(player);

        return this.getQuestTracker(player, questId)
                .map(tracker -> tracker.isPinned(taskId))
                .orElse(false);
    }

    /**
     * Получить число выполненных этапов игроком. Возвращает -1 если не найден
     * @param questId Идентификатор квеста
     * @param player Игрок
     * @return Число этапов
     */
    public int getStagesComplete(Identifier questId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(player);

        return this.getQuestTracker(player, questId)
                .flatMap(QuestProgressTracker::getActiveStage)
                .orElse(0);
    }

    public Optional<Integer> getActiveStage(Identifier questId, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(player);

        return this.getQuestTracker(player, questId)
                .flatMap(QuestProgressTracker::getActiveStage);
    }

    /**
     * Получить число выполненных задач в определённом этапе
     * @param questId Идентификатор квеста
     * @param stage Этап
     * @param player Игрок
     * @return Число задач
     */
    public int getTasksComplete(Identifier questId, int stage, ServerPlayerEntity player) {
        Objects.requireNonNull(questId);
        Objects.requireNonNull(player);

        var questTracker = this.getQuestTracker(player, questId).orElse(null);
        if (questTracker == null) return -1;

        var quest = this.questRepository.getQuest(questId);
        if (quest == null) return -1;

        return (int) quest.getStage(stage).stream()
                .map(questTracker::isComplete)
                .filter(result -> result)
                .count();
    }

    /**
     * Получить число выполненных задач в активном этапе
     * @param questId Идентификатор квеста
     * @param player Игрок
     * @return Число задач
     */
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

    /**
     * Получить уровень успешного выполнения задачи
     * @param questId Идентификатор квеста
     * @param player Игрок
     * @return Уровень выполнения
     */
    public int getTaskSuccessCompletion(Identifier questId, String taskId, ServerPlayerEntity player) {
        var task = this.questRepository.getTask(questId, taskId);
        if (task == null) return 0;
        return this.conditionDispatcher.getCurrentValue(task.successCondition(), player);
    }

    /**
     * Получить целевое значение для успешного выполнения задачи
     * @param questId Идентификатор квеста
     * @return Целевое значение выполнения
     */
    public int getTaskSuccessTarget(Identifier questId, String taskId) {
        var task = this.questRepository.getTask(questId, taskId);
        if (task == null) return 1;
        var condition = task.successCondition();
        if (condition == null) return 1;
        return condition.getTargetValue();
    }

    /**
     * Получить уровень провала задачи
     * @param questId Идентификатор квеста
     * @param player Игрок
     * @return Уровень провала
     */
    public int getTaskFailureCompletion(Identifier questId, String taskId, ServerPlayerEntity player) {
        var task = this.questRepository.getTask(questId, taskId);
        if (task == null) return 0;
        return this.conditionDispatcher.getCurrentValue(task.failureCondition(), player);
    }

    /**
     * Получить целевое значение для провала задачи
     * @param questId Идентификатор квеста
     * @return Целевое значение провала
     */
    public int getTaskFailureTarget(Identifier questId, String taskId) {
        var task = this.questRepository.getTask(questId, taskId);
        if (task == null) return 1;
        var condition = task.failureCondition();
        if (condition == null) return 1;
        return condition.getTargetValue();
    }

    // </editor-fold>

    public void update(List<ServerPlayerEntity> players) {
        for (var player : players) {
            this.updatePlayer(player);
        }
    }

    private void updatePlayer(ServerPlayerEntity player) {
        var tracker = this.trackedPlayers.get(player.getUuid());
        if (tracker == null) return;

        for (var questId : tracker.getQuestsToUpdate()) {
            this.updatePlayerQuest(player, questId);
        }
    }

    /**
     * Должен вызываться только на незавершённых квестах!
     * @param player Игрок
     * @param questId Идентификатор квеста
     */
    private void updatePlayerQuest(ServerPlayerEntity player, Identifier questId) {
        var playerTracker = this.getPlayerTracker(player).orElse(null);
        if (playerTracker == null) return;

        var questTracker = playerTracker.getQuestTracker(questId).orElse(null);
        if (questTracker == null) return;

        var entry = this.questRepository.getQuestEntry(questId);
        if (entry == null) return;

        questTracker.recomputeActiveStage(newStage ->
                QuestProgressEvents.STAGE_CHANGED.invoker().onStageChange(entry, newStage, player));

        var activeStage = questTracker.getActiveStage().orElse(null);
        if (activeStage == null) return;

        this.ensureActiveStageLoaded(player, questTracker);

        // Process each active task
        for (var taskId : questTracker.getActiveTasks()) {
            var taskEntry = this.questRepository.getTaskEntry(questId, taskId);
            if (taskEntry == null) continue;

            var task = taskEntry.task();

            // Tick each task
            this.conditionDispatcher.tick(task.successCondition(), player);
            this.conditionDispatcher.tick(task.failureCondition(), player);
            QuestProgressEvents.TASK_TICKED.invoker().onTaskTick(taskEntry, player);

            // Update progress
            var successValue = this.conditionDispatcher.getCurrentValue(task.successCondition(), player);
            var failureValue = this.conditionDispatcher.getCurrentValue(task.failureCondition(), player);

            var successChanged = questTracker.updateSuccessValue(taskId, successValue);

            if (successChanged) {
                QuestProgressEvents.TASK_SUCCESS_PROGRESS_CHANGED.invoker()
                        .onTaskProgressChange(taskEntry, player, successValue);
                this.isDirty = true;
            }

            // Update progress
            var failureChanged = questTracker.updateFailureValue(taskId, failureValue);

            if (failureChanged) {
                QuestProgressEvents.TASK_FAILURE_PROGRESS_CHANGED.invoker()
                        .onTaskProgressChange(taskEntry, player, failureValue);
                this.isDirty = true;
            }

            // Check if complete
            if (successChanged && this.conditionDispatcher.test(task.successCondition(), player)) {
                this.completeTask(questId, taskId, player, CompletionStatus.SUCCESS);
            }

            // Check if complete
            else if (failureChanged && this.conditionDispatcher.test(task.failureCondition(), player)) {
                this.completeTask(questId, taskId, player, CompletionStatus.FAILURE);
            }
        }

        boolean wasPinned = questTracker.isPinned();

        playerTracker.checkCompletion(questId, status -> {
            if (wasPinned) {
                QuestEvents.QUEST_PIN_REMOVED.invoker().onQuestPinRemove(questId, player);
            }
            QuestProgressEvents.QUEST_COMPLETED.invoker().onQuestCompletion(entry, status, player);
            this.unlockDependentQuests(player, playerTracker, questId);
            this.isDirty = true;
        });
    }

    /**
     * Удостоверяется, что все задачи активного этапа загружены. Загружает то что не загружено и выгружает то,
     * что уже не нужно. Отправляет соответствующие события
     * @param player Игрок
     * @param questTracker Трекер квеста
     */
    private void ensureActiveStageLoaded(ServerPlayerEntity player, QuestProgressTracker questTracker) {
        var prevLoadedTasks = new HashSet<>(questTracker.getActiveTasks());
        questTracker.loadActiveStage(
                task -> this.conditionDispatcher.getCurrentValue(task.successCondition(), player),
                task -> this.conditionDispatcher.getCurrentValue(task.failureCondition(), player)
        );

        var loadedTasks = questTracker.getActiveTasks();

        // Unload tasks that are loaded but not present in the current stage
        prevLoadedTasks.stream().filter(taskId -> !loadedTasks.contains(taskId)).forEach(taskId -> {
            var taskEntry = this.questRepository.getTaskEntry(questTracker.getQuestId(), taskId);
            if (taskEntry == null) return;

            QuestProgressEvents.TASK_UNLOADED.invoker().onTaskUnload(taskEntry, player);
        });

        // Load tasks in current stage that are not loaded
        loadedTasks.stream().filter(taskId -> !prevLoadedTasks.contains(taskId)).forEach(taskId -> {
            var taskEntry = this.questRepository.getTaskEntry(questTracker.getQuestId(), taskId);
            if (taskEntry == null) return;

            this.conditionDispatcher.load(taskEntry.task().successCondition(), player);
            this.conditionDispatcher.load(taskEntry.task().failureCondition(), player);

            QuestProgressEvents.TASK_LOADED.invoker().onTaskLoad(taskEntry, player);
        });

    }

    private void unlockDependentQuests(ServerPlayerEntity player, PlayerProgressTracker playerTracker, Identifier questId) {
        for (var dependentQuestId : this.questRepository.getDependentQuests(questId)) {
            var dependentQuest = this.questRepository.getQuest(dependentQuestId);
            if (dependentQuest == null) continue;

            for (var groupIndex = 0; groupIndex < dependentQuest.getDependencyGroupsCount(); groupIndex++) {
                var isGroupComplete = dependentQuest.getDependencyGroup(groupIndex).stream()
                        .allMatch(depId -> playerTracker.isComplete(depId, CompletionStatus.SUCCESS));

                if (isGroupComplete) {
                    this.giveQuest(dependentQuestId, player);
                    break;
                }
            }
        }
    }

    private void unpinIfPinned(Identifier questId, ServerPlayerEntity player, PlayerProgressTracker playerTracker) {
        var questTracker = playerTracker.getQuestTracker(questId).orElse(null);
        if (questTracker == null || !questTracker.isPinned()) return;

        questTracker.resetTaskPin();
        QuestEvents.QUEST_PIN_REMOVED.invoker().onQuestPinRemove(questId, player);
    }

    private Optional<PlayerProgressTracker> getPlayerTracker(ServerPlayerEntity player) {
        return Optional.ofNullable(this.trackedPlayers.get(player.getUuid()));
    }

    private Optional<QuestProgressTracker> getQuestTracker(ServerPlayerEntity player, Identifier questId) {
        return this.getPlayerTracker(player)
                .flatMap(tracker -> tracker.getQuestTracker(questId));
    }

    public void loadState(ServerQuestManagerState state) {
        this.trackedPlayers.clear();

        this.questRepository.replaceDynamicQuests(state.dynamicQuests());

        for (var playerTrackerStateEntry : state.playerTrackers().entrySet()) {
            var playerTracker = PlayerProgressTracker.create(this.questRepository, playerTrackerStateEntry.getValue());
            this.trackedPlayers.put(playerTrackerStateEntry.getKey(), playerTracker);
        }
    }

    public ServerQuestManagerState saveState() {
        this.isDirty = false;

        return new ServerQuestManagerState(
                Collections.unmodifiableMap(this.trackedPlayers.entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().saveState()
                ))),
                this.questRepository.getDynamicQuestIds().stream().collect(Collectors.toUnmodifiableMap(
                        Function.identity(),
                        this.questRepository::requireQuest
                ))
        );
    }

    public void loadQuests(Map<Identifier, Quest> quests) {
        this.questRepository.replaceStaticQuests(quests);
        QuestEvents.QUESTS_RELOADED.invoker().onReload();

        // TODO: После перезагрузки должно отправляться событие клиентам, если у кого-то из клиентов открыт
        //  список квестов, он должен запросить список снова
    }
}

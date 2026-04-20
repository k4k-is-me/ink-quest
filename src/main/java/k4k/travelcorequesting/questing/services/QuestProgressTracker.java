package k4k.travelcorequesting.questing.services;

import k4k.travelcorequesting.domain.abstractions.Task;
import k4k.travelcorequesting.domain.enums.CompletionStatus;
import k4k.travelcorequesting.questing.abstractions.QuestResolver;
import k4k.travelcorequesting.questing.states.QuestTrackerState;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Класс для хранения уровня выполнения задач квеста игроком
 */
public final class QuestProgressTracker {
    private final QuestResolver resolver;
    private final Identifier questId;

    /// Активный этап квеста
    private @Nullable Integer activeStage = null;

    /// Выполненные задачи квеста
    private final Map<String, CompletionStatus> completeTasks = new HashMap<>();

    /// Активно отслеживаемые задачи квеста
    private final Map<String, TaskProgressTracker> activeTasks = new HashMap<>();

    /// Закреплённая задача квеста
    private @Nullable String pinnedTaskId = null;


    public QuestProgressTracker(Identifier questId, QuestResolver resolver) {
        this.resolver = resolver;
        this.questId = questId;
    }

    public static QuestProgressTracker create(Identifier questId, QuestResolver resolver, QuestTrackerState state) {
        var tracker = new QuestProgressTracker(questId, resolver);

        for (var taskTrackerStateEntry : state.activeTasksTrackers().entrySet()) {
            var taskTracker = TaskProgressTracker.create(taskTrackerStateEntry.getValue());
            tracker.activeTasks.put(taskTrackerStateEntry.getKey(), taskTracker);
        }

        tracker.completeTasks.putAll(state.completeTasks());
        tracker.activeStage = state.activeStage();
        tracker.pinnedTaskId = state.pinnedTaskId();

        return tracker;
    }

    public QuestTrackerState saveState() {
        return new QuestTrackerState(
                this.activeStage,
                this.pinnedTaskId,
                Collections.unmodifiableMap(this.completeTasks),
                Collections.unmodifiableMap(this.activeTasks.entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().saveState()
                )))
        );
    }

    public Identifier getQuestId() {
        return this.questId;
    }

    /**
     * Возвращает записанный активный этап квеста. Активный этап может быть
     * записан или рассчитан. Записывается фактический этап, а рассчитывается
     * тот, который должен быть. Если рассчитанный и фактический отличаются -
     * это повод сменить этап.
     * @return Фактический активный этап
     */
    public Optional<Integer> getActiveStage() {
        return Optional.ofNullable(this.activeStage);
    }

    /**
     * Рассчитывает и внутренне сохраняет активный этап квеста. Если этап
     * сменился - вызывает хендлер
     * @param handler Обработчик смены этапа
     */
    public boolean recomputeActiveStage(StageChangeHandler handler) {
        var stage = this.computeActiveStageIndex();

        if (Objects.equals(stage, this.activeStage))
            return false;

        this.activeStage = stage;

        handler.onStageChange(stage);
        return true;
    }

    /**
     * Помечает задачу как выполненную
     * @param taskId Идентификатор задачи
     * @param status Статус выполнения
     */
    public void complete(String taskId, CompletionStatus status) {
        var quest = this.resolver.getQuest(this.questId);
        if (quest == null) return;

        if (!quest.containsTask(taskId))
            return;

        this.activeTasks.remove(taskId);  // NOTE: В загруженных задачах не должно появляться завершённых - может привести к проблемам
        this.completeTasks.put(taskId, status);
    }

    /**
     * Возвращает статус выполненной задачи квеста (null если задача не отслеживаются или не выполнена)
     * @return Статус задачи
     */
    public Optional<CompletionStatus> getCompletionStatus(String taskId) {
        return Optional.ofNullable(this.completeTasks.get(taskId));
    }

    /**
     * Возвращает true если задача активна
     * @return true, если задача активна
     */
    public boolean isActive(String taskId) {
        return this.activeTasks.containsKey(taskId);
    }

    /**
     * Возвращает true если задача завершена
     * @return true, если задача завершена
     */
    public boolean isComplete(String taskId) {
        return this.completeTasks.containsKey(taskId);
    }

    /**
     * Возвращает true если задача завершена
     * @return true, если задача завершена
     */
    public boolean isComplete(String taskId, CompletionStatus status) {
        return this.completeTasks.containsKey(taskId) && this.completeTasks.get(taskId) == status;
    }

    /**
     * Возвращает true если квест закреплён (закреплена одна из задач квеста)
     */
    public boolean isPinned() {
        return this.pinnedTaskId != null;
    }

    /**
     * Возвращает true если задача закреплена
     * @param taskId Идентификатор задачи
     */
    public boolean isPinned(String taskId) {
        return Objects.equals(this.pinnedTaskId, taskId);
    }

    public Optional<String> getTaskPin() {
        return Optional.ofNullable(this.pinnedTaskId);
    }

    public void setTaskPin(String taskId) {
        Objects.requireNonNull(taskId);

        this.resolver.requireTask(this.questId, taskId);
        this.pinnedTaskId = taskId;
    }

    public void resetTaskPin() {
        this.pinnedTaskId = null;
    }

    /**
     * Возвращает список отслеживаемых задач (обычно список задач в активном этапе за исключением выполненных).
     * Чтобы актуализировать список, необходимо вызвать loadActiveStage.
     * @return Список отслеживаемых задач
     */
    public Set<String> getActiveTasks() {
        return Collections.unmodifiableSet(this.activeTasks.keySet());  // NOTE: returned set is backed by a map so it may change
    }

    /**
     * Загружает не выполненные задачи активного этапа, которые ещё не были загружены и удаляет из списка активных
     * задачи не относящиеся к этапу. Желательно вызывать каждый тик, чтобы повторно не обрабатывать уже выполненные
     * задачи
     * @param successProvider Провайдер начальных значений условия успеха задачи
     * @param failureProvider Провайдер начальных значений условия неудачи задачи
     */
    public void loadActiveStage(InitialProgressProvider successProvider, InitialProgressProvider failureProvider) {
        var quest = this.resolver.getQuest(this.questId);

        if (quest == null) return;
        if (this.activeStage == null) {
            this.activeTasks.clear();
            return;
        }

        var stageTasks = quest.getStage(this.activeStage);

        this.activeTasks.keySet().retainAll(stageTasks);

        for (var taskId : stageTasks) {
            if (this.isComplete(taskId)) {  // Complete tasks can't be active
                this.activeTasks.remove(taskId);
                continue;
            }

            var task = quest.getTask(taskId);
            if (task == null) continue;

            this.activeTasks.computeIfAbsent(taskId, key -> new TaskProgressTracker(
                    successProvider.getInitialProgress(task),
                    failureProvider.getInitialProgress(task)
            ));
        }
    }

    /**
     * Устанавливает новое значение прогресса для условия успеха задачи. Возвращает true если значение отличается
     * от предыдущего. Если новое значение больше или равно целевому - задача помечается выполненной.
     * @param newValue Новое значение
     * @return true если значение отличается от предыдущего
     */
    public boolean updateSuccessValue(String taskId, int newValue) {
        if (this.isComplete(taskId)) return false;

        var taskTracker = this.activeTasks.get(taskId);
        if (taskTracker == null) return false;

        var oldValue = taskTracker.successProgress;
        taskTracker.successProgress = newValue;

        return oldValue != newValue;
    }

    /**
     * Устанавливает новое значение прогресса для условия неудачи задачи. Возвращает true если значение отличается
     * от предыдущего. Если новое значение больше или равно целевому - задача помечается выполненной.
     * @param newValue Новое значение
     * @return true если значение отличается от предыдущего
     */
    public boolean updateFailureValue(String taskId, int newValue) {
        if (this.isComplete(taskId)) return false;

        var taskTracker = this.activeTasks.get(taskId);
        if (taskTracker == null) return false;

        var oldValue = taskTracker.failureProgress;
        taskTracker.failureProgress = newValue;

        return oldValue != newValue;
    }


    @FunctionalInterface
    public interface StageChangeHandler {
        void onStageChange(@Nullable Integer newStage);
    }

    @FunctionalInterface
    public interface InitialProgressProvider {
        Integer getInitialProgress(Task task);
    }


    /**
     * Рассчитывает номер активного этапа квеста, null если все этапы пройдены
     * (в том числе если квест не содержит этапов).
     * Если квест содержит хотя бы один этап - начинаться отсчёт будет с 0.
     * Не может использоваться для определения завершённости квеста.
     * Используется для определения когда игрок должен перейти на другой этап
     */
    private @Nullable Integer computeActiveStageIndex() {
        var quest = this.resolver.getQuest(this.questId);
        if (quest == null) return null;

        for (var stageIndex = 0; stageIndex < quest.getStageCount(); stageIndex++) {
            var requiredTaskId = quest.getRequiredTask(stageIndex);

            // Обязательная задача этапа ещё не завершена - значит этап активный
            if (!this.completeTasks.containsKey(requiredTaskId))
                return stageIndex;

            // Обязательная задача завершена провалом - значит квест провален и активного этапа быть не должно
            if (this.completeTasks.get(requiredTaskId) == CompletionStatus.FAILURE)
                return null;
        }

        // Все обязательные задачи завершены, значит квест завершён и активного этапа быть не должно
        return null;
    }
}

package k4k.travelcorequesting.questing.services;

import k4k.travelcorequesting.domain.abstractions.Quest;
import k4k.travelcorequesting.domain.abstractions.Task;
import k4k.travelcorequesting.domain.models.MutableQuest;
import k4k.travelcorequesting.domain.models.MutableTask;
import k4k.travelcorequesting.domain.models.taskConditions.PredicateCondition;
import k4k.travelcorequesting.questing.abstractions.QuestModifier;
import k4k.travelcorequesting.questing.abstractions.QuestResolver;
import k4k.travelcorequesting.questing.enums.TaskType;
import k4k.travelcorequesting.questing.models.QuestEntry;
import k4k.travelcorequesting.questing.enums.QuestSourceType;
import k4k.travelcorequesting.questing.models.TaskEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class QuestRepository implements QuestResolver {
    private final Map<Identifier, Quest> staticQuests = new HashMap<>();
    private final Map<Identifier, MutableQuest> dynamicQuests = new HashMap<>();

    /// Отношение квеста к списку квестов зависящих от него
    /// Оптимизация. Позволяет быстро определить какие квесты нужно проверить и потенциально выдать
    private final Map<Identifier, List<Identifier>> dependants = new HashMap<>();

    /**
     * Метод для загрузки статических квестов.
     * Статические квесты - это квесты, которые не могут быть изменены в ходе игры,
     * например квесты из датапаков
     */
    public void replaceStaticQuests(Map<Identifier, Quest> newStaticQuests) {
        this.staticQuests.clear();
        this.staticQuests.putAll(newStaticQuests);
        this.recomputeDependants();
    }

    public void replaceDynamicQuests(Map<Identifier, Quest> newDynamicQuests) {
        this.dynamicQuests.clear();
        this.dynamicQuests.putAll(newDynamicQuests.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> (MutableQuest) entry.getValue()
                )));
    }

    /**
     * Пересчитывает отношение квестов к квестам, которые от них зависят
     */
    private void recomputeDependants() {
        this.dependants.clear();

        for (var questId : this.getQuestIds()) {
            var quest = this.requireQuest(questId);

            for (var groupIndex = 0; groupIndex < quest.getDependencyGroupsCount(); groupIndex++) {
                for (var depId : quest.getDependencyGroup(groupIndex)) {
                    this.dependants.computeIfAbsent(depId, ignored -> new ArrayList<>())
                            .add(questId);
                }
            }
        }
    }

    /**
     * Создаёт новый квест. Ошибка, если квест уже существует
     * @param questId Идентификатор
     */
    public QuestEntry createDynamicQuest(Identifier questId) {
        Objects.requireNonNull(questId);

        if (this.dynamicQuests.containsKey(questId))
            throw new IllegalArgumentException(String.format("Quest with id %s already exists", questId));

        var quest = this.dynamicQuests.put(questId, MutableQuest.create(
                Text.literal(questId.toString())
        ));

        return new QuestEntry(questId, quest, QuestSourceType.DYNAMIC);
    }

    public QuestModifier getQuestModifier(Identifier questId) {
        Objects.requireNonNull(questId);

        var quest = this.requireQuestMutable(questId);
        return new QuestModifierImpl(questId, quest);
    }

    public boolean isQuestStatic(Identifier questId) {
        Objects.requireNonNull(questId);
        return this.staticQuests.containsKey(questId);
    }

    @Override
    public Quest requireQuest(Identifier questId) {
        var quest = this.getQuest(questId);
        if (quest == null)
            throw new IllegalArgumentException(String.format("Quest %s is required but not found", questId));
        return quest;
    }

    @Override
    public Task requireTask(Identifier questId, String taskId) {
        var quest = this.requireQuest(questId);
        var task = quest.getTask(taskId);
        if (task == null)
            throw new IllegalArgumentException(String.format("Task %s of quest %s is required but not found", taskId, questId));
        return task;
    }

    /**
     * Получить квест по идентификатору, null если не существует
     * @param questId Идентификатор квеста
     * @return Квест
     */
    @Override
    public @Nullable Quest getQuest(Identifier questId) {  // TODO: return optional
        var quest = this.dynamicQuests.get(questId);
        if (quest != null) return quest;
        return this.staticQuests.get(questId);
    }

    /**
     * Получить задачу по идентификатору, null если не существует
     * @param questId Идентификатор квеста
     * @param taskId Идентификатор задачи
     * @return Задача
     */
    @Override
    public @Nullable Task getTask(Identifier questId, String taskId) {
        var quest = this.getQuest(questId);
        if (quest == null) return null;
        return quest.getTask(taskId);
    }

    @Override
    public @Nullable QuestSourceType getQuestSourceType(Identifier questId) {
        if (this.dynamicQuests.containsKey(questId))
            return QuestSourceType.DYNAMIC;
        if (this.staticQuests.containsKey(questId))
            return QuestSourceType.STATIC;
        return null;
    }

    @Override
    public @Nullable QuestEntry getQuestEntry(Identifier questId) {
        var quest = this.getQuest(questId);
        if (quest == null) return null;
        return new QuestEntry(questId, quest, this.getQuestSourceType(questId));
    }

    @Override
    public QuestEntry requireQuestEntry(Identifier questId) {
        var quest = this.requireQuest(questId);
        return new QuestEntry(questId, quest, this.getQuestSourceType(questId));
    }

    @Override
    public @Nullable TaskEntry getTaskEntry(Identifier questId, String taskId) {
        var questEntry = this.getQuestEntry(questId);
        if (questEntry == null) return null;

        var task = this.getTask(questId, taskId);
        if (task == null) return null;

        return TaskEntry.fromQuestEntry(questEntry, taskId, task);
    }

    @Override
    public TaskEntry requireTaskEntry(Identifier questId, String taskId) {
        var questEntry = this.requireQuestEntry(questId);
        var task = this.requireTask(questId, taskId);

        return TaskEntry.fromQuestEntry(questEntry, taskId, task);
    }

    /**
     * Получить список всех квестов
     * @return Список квестов
     */
    public List<Identifier> getQuestIds() {
        return Stream.concat(
                        this.dynamicQuests.keySet().stream(),
                        this.staticQuests.keySet().stream()
                )
                .distinct()
                .toList();
    }

    public Set<Identifier> getStaticQuestIds() {
        return this.staticQuests.keySet();
    }

    public Set<Identifier> getDynamicQuestIds() {
        return this.dynamicQuests.keySet();
    }

    /**
     * Возвращает список квестов, зависящих от переданного
     * @param questId Идентификатор квеста
     * @return Список зависимых квестов
     */
    public List<Identifier> getDependentQuests(Identifier questId) {
        return Collections.unmodifiableList(this.dependants.computeIfAbsent(questId, ignored -> new ArrayList<>()));
    }

    /**
     * Возвращает список всех этапов, в которых участвует задача
     */
    @Override
    public List<Integer> getTaskStages(Identifier questId, String taskId) {
        var taskEntry = this.requireTaskEntry(questId, taskId);
        var quest = taskEntry.quest();
        return IntStream.range(0, quest.getStageCount())
                .filter(stage -> quest.getStage(stage).stream()
                        .anyMatch(t -> Objects.equals(t, taskId)))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Возвращает обязательность задачи по номеру этапа
     */
    @Override
    public @Nullable TaskType getTaskType(Identifier questId, String taskId, int stageIndex) {
        var taskEntry = this.requireTaskEntry(questId, taskId);
        var quest = taskEntry.quest();

        if (stageIndex < 0 || stageIndex > quest.getStageCount()) return null;

        var stage = quest.getStage(stageIndex);
        if (stage.isEmpty() || !stage.contains(taskId)) return null;

        return TaskType.from(Objects.equals(stage.get(0), taskId));
    }


    private MutableQuest requireQuestMutable(Identifier questId) {
        var quest = this.dynamicQuests.get(questId);
        if (quest == null)
            throw new IllegalArgumentException(String.format("Quest %s is required but not found", questId));
        return quest;
    }

    private MutableTask requireTaskMutable(Identifier questId, String taskId) {
        var quest = this.requireQuestMutable(questId);
        var task = quest.getTaskMutable(taskId);
        if (task == null)
            throw new IllegalArgumentException(String.format("Task %s of quest %s is required but not found", taskId, questId));
        return task;
    }


    public final class QuestModifierImpl implements QuestModifier {
        private final Identifier questId;
        private final MutableQuest quest;
        private boolean isDirty = false;

        QuestModifierImpl(Identifier questId, MutableQuest quest) {
            this.questId = questId;
            this.quest = quest;
        }

        @Override
        public boolean isDirty() {
            return this.isDirty;
        }

        /**
         * Устанавливает заголовок квеста. Ошибка, если квеста не существует
         * @param title Заголовок
         */
        @Override
        public Quest setTitle(Text title) {
            Objects.requireNonNull(title);

            quest.setTitle(title);
            this.isDirty = true;
            return quest;
        }

        /**
         * Устанавливает описание квеста. Ошибка, если квеста не существует
         * @param description Описание
         */
        @Override
        public Quest setDescription(@Nullable Text description) {
            quest.setDescription(description);
            this.isDirty = true;
            return quest;
        }

        /**
         * Устанавливает иконку квеста. Ошибка, если квеста не существует
         * @param icon Иконка
         */
        @Override
        public Quest setIcon(Identifier icon) {
            Objects.requireNonNull(icon);

            quest.setIcon(icon);
            this.isDirty = true;
            return quest;
        }

        /**
         * Устанавливает индекс сортировки квеста. Ошибка, если квеста не существует
         * @param index Индекс сортировки
         */
        @Override
        public Quest setIndex(int index) {
            quest.setIndex(index);
            this.isDirty = true;
            return quest;
        }

        /**
         * Устанавливает признак фонового квеста. Ошибка, если квеста не существует
         * @param isBackground Признак
         */
        @Override
        public Quest setBackground(boolean isBackground) {
            quest.setBackground(isBackground);
            this.isDirty = true;
            return quest;
        }

        /**
         * Устанавливает режим закрепления квеста. Ошибка, если квеста не существует
         * @param pin Режим
         */
        @Override
        public Quest setPin(boolean pin) {
            quest.setPin(pin);
            this.isDirty = true;
            return quest;
        }

        /**
         * Добавляет зависимость в последний блок зависимостей квеста. Ошибка, если квеста не существует
         * @param dependency Зависимость
         */
        @Override
        public Quest addAndDependency(Identifier dependency) {
            Objects.requireNonNull(dependency);

            var lastGroupIndex = quest.getDependencyGroupsCount() - 1;
            quest.addDependency(lastGroupIndex, dependency);

            dependants.computeIfAbsent(dependency, depId -> new ArrayList<>())
                    .add(questId);
            this.isDirty = true;
            return quest;
        }

        /**
         * Добавляет зависимость в новый блок зависимостей квеста. Ошибка, если квеста не существует
         * @param dependency Зависимость
         */
        @Override
        public Quest addOrDependency(Identifier dependency) {
            Objects.requireNonNull(dependency);

            quest.addDependency(dependency);

            dependants.computeIfAbsent(dependency, depId -> new ArrayList<>())
                    .add(questId);
            this.isDirty = true;
            return quest;
        }

        /**
         * Удаляет все зависимости квеста. Ошибка, если квеста не существует
         */
        @Override
        public Quest removeDependencies() {
            for (var depGroupIndex = 0; depGroupIndex < quest.getDependencyGroupsCount(); depGroupIndex++) {
                for (var depId : quest.getDependencyGroup(depGroupIndex)) {
                    if (!dependants.containsKey(depId)) continue;
                    dependants.get(depId).remove(questId);
                }
            }

            quest.removeDependencies();
            this.isDirty = true;
            return quest;
        }

        /**
         * Добавляет новый этап в квест и создаёт в нём задачу. Ошибка, если квеста не существует или задача с таким
         * идентификатором уже существует в квесте
         * @param taskId Идентификатор задачи
         */
        @Override
        public Quest addTaskRequired(String taskId) {
            Objects.requireNonNull(taskId);

            if (quest.containsTask(taskId))
                throw new IllegalArgumentException(String.format("Quest with id %s already contains task %s", questId, taskId));

            quest.setTask(taskId, MutableTask.create(Text.literal(taskId)));
            quest.addTaskToStage(taskId);
            this.isDirty = true;
            return quest;
        }

        /**
         * Добавляет задачу в последний этап квеста. Ошибка, если квеста не существует, задача с таким идентификатором
         * уже существует в квесте или квест не имеет этапов
         * @param taskId Идентификатор задачи
         */
        @Override
        public Quest addTaskOptional(String taskId) {
            Objects.requireNonNull(taskId);

            if (quest.containsTask(taskId))
                throw new IllegalArgumentException(String.format("Quest with id %s already contains task %s", questId, taskId));

            quest.setTask(taskId, MutableTask.create(Text.literal(taskId)));
            var lastStageIndex = quest.getStageCount() - 1;
            quest.addTaskToStage(lastStageIndex, taskId);
            this.isDirty = true;
            return quest;
        }

        /**
         * Устанавливает заголовок задачи квеста. Ошибка, если квеста не существует или задача с таким идентификатором уже
         * существует в квесте
         * @param taskId Идентификатор задачи
         * @param title Заголовок
         */
        @Override
        public Quest setTaskTitle(String taskId, Text title) {
            Objects.requireNonNull(taskId);
            Objects.requireNonNull(title);

            var task = requireTaskMutable(questId, taskId);
            task.setTitle(title);
            this.isDirty = true;
            return requireQuest(questId);
        }

        /**
         * Устанавливает описание задачи квеста. Ошибка, если квеста не существует или задача с таким идентификатором уже
         * существует в квесте
         * @param taskId Идентификатор задачи
         * @param description Заголовок
         */
        @Override
        public Quest setTaskDescription(String taskId, @Nullable Text description) {
            Objects.requireNonNull(taskId);

            var task = requireTaskMutable(questId, taskId);
            task.setDescription(description);
            this.isDirty = true;
            return requireQuest(questId);
        }

        /**
         * Устанавливает функцию load задачи квеста. Ошибка, если квеста не существует или задача с таким идентификатором
         * уже существует в квесте
         * @param taskId Идентификатор задачи
         * @param function Идентификатор функции
         */
        @Override
        public Quest setTaskLoadFunction(String taskId, @Nullable Identifier function) {
            Objects.requireNonNull(taskId);

            var task = requireTaskMutable(questId, taskId);
            task.setLoadFunction(function);
            this.isDirty = true;
            return requireQuest(questId);
        }

        /**
         * Устанавливает функцию tick задачи квеста. Ошибка, если квеста не существует или задача с таким идентификатором
         * уже существует в квесте
         * @param taskId Идентификатор задачи
         * @param function Идентификатор функции
         */
        @Override
        public Quest setTaskTickFunction(String taskId, @Nullable Identifier function) {
            Objects.requireNonNull(taskId);

            var task = requireTaskMutable(questId, taskId);
            task.setTickFunction(function);
            this.isDirty = true;
            return requireQuest(questId);
        }

        /**
         * Устанавливает функцию fail задачи квеста. Ошибка, если квеста не существует или задача с таким идентификатором
         * уже существует в квесте
         * @param taskId Идентификатор задачи
         * @param function Идентификатор функции
         */
        @Override
        public Quest setTaskFailedFunction(String taskId, @Nullable Identifier function) {
            Objects.requireNonNull(taskId);

            var task = requireTaskMutable(questId, taskId);
            task.setFailureFunction(function);
            this.isDirty = true;
            return requireQuest(questId);
        }

        /**
         * Устанавливает функцию success задачи квеста. Ошибка, если квеста не существует или задача с таким идентификатором
         * уже существует в квесте
         * @param taskId Идентификатор задачи
         * @param function Идентификатор функции
         */
        @Override
        public Quest setTaskSucceededFunction(String taskId, @Nullable Identifier function) {
            Objects.requireNonNull(taskId);

            var task = requireTaskMutable(questId, taskId);
            task.setSuccessFunction(function);
            this.isDirty = true;
            return requireQuest(questId);
        }

        /**
         * Устанавливает функцию done задачи квеста. Ошибка, если квеста не существует или задача с таким идентификатором
         * уже существует в квесте
         * @param taskId Идентификатор задачи
         * @param function Идентификатор функции
         */
        @Override
        public Quest setTaskUnloadFunction(String taskId, @Nullable Identifier function) {
            Objects.requireNonNull(taskId);

            var task = requireTaskMutable(questId, taskId);
            task.setUnloadFunction(function);
            this.isDirty = true;
            return requireQuest(questId);
        }

        /**
         * Устанавливает условие выполнения задачи. Предикат - параметризованная функция, возвращающая bool. Квест будет
         * считаться выполненным, если при проверке предиката тот вернёт true. Ошибка, если квеста или задачи не существует
         * @param taskId Идентификатор задачи
         * @param predicateId Предикат
         */
        @Override
        public Quest setTaskSuccessConditionPredicate(String taskId, Identifier predicateId) {
            Objects.requireNonNull(taskId);
            Objects.requireNonNull(predicateId);

            var task = requireTaskMutable(questId, taskId);
            task.setSuccessCondition(new PredicateCondition(predicateId));
            this.isDirty = true;
            return requireQuest(questId);
        }

    //    /**
    //     * Устанавливает условие выполнения задачи. Задача будет считаться выполненной, если игроком достигнут указанный
    //     * или больший счёт. Ошибка, если квеста или задачи не существует
    //     * @param questId Идентификатор квеста
    //     * @param taskId Идентификатор задачи
    //     * @param objective Задача scoreboard-а
    //     * @param targetScore Целевое значение
    //     */
    //    public Quest setQuestTaskSuccessConditionScore(Identifier questId, String taskId, String objective, ScoreboardCriterion criterion, int targetScore, @Nullable String player) {
    //        Objects.requireNonNull(questId);
    //        Objects.requireNonNull(taskId);
    //        Objects.requireNonNull(objective);
    //
    //        var task = this.requireTaskMutable(questId, taskId);
    //        task.setSuccessCondition(new ScoreCondition(objective, criterion, targetScore, player));
    //        return this.requireQuest(questId);
    //    }

        /**
         * Устанавливает условие провала задачи. Предикат - параметризованная функция, возвращающая bool. Квест будет
         * считаться проваленным, если при проверке предиката тот вернёт true. Ошибка, если квеста или задачи не существует
         * @param taskId Идентификатор задачи
         * @param predicateId Предикат
         */
        @Override
        public Quest setTaskFailureConditionPredicate(String taskId, Identifier predicateId) {
            Objects.requireNonNull(taskId);
            Objects.requireNonNull(predicateId);

            var task = requireTaskMutable(questId, taskId);
            task.setFailureCondition(new PredicateCondition(predicateId));
            this.isDirty = true;
            return requireQuest(questId);
        }

    //    /**
    //     * Устанавливает условие провала задачи. Задача будет считаться проваленной, если игроком достигнут указанный
    //     * или больший счёт. Ошибка, если квеста или задачи не существует
    //     * @param questId Идентификатор квеста
    //     * @param taskId Идентификатор задачи
    //     * @param objective Задача scoreboard-а
    //     * @param targetScore Целевое значение
    //     */
    //    public Quest setQuestTaskFailureConditionScore(Identifier questId, String taskId, String objective, ScoreboardCriterion criterion, int targetScore, @Nullable String player) {
    //        Objects.requireNonNull(questId);
    //        Objects.requireNonNull(taskId);
    //        Objects.requireNonNull(objective);
    //
    //        var task = this.requireTaskMutable(questId, taskId);
    //        task.setFailureCondition(new ScoreCondition(objective, criterion, targetScore, player));
    //        return this.requireQuest(questId);
    //    }
    }
}

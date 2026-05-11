package k4k.travelcorequesting.domain.models;

import com.google.common.collect.Lists;
import k4k.travelcorequesting.TravelcoreQuesting;
import k4k.travelcorequesting.domain.abstractions.Quest;
import k4k.travelcorequesting.domain.abstractions.Task;
import k4k.travelcorequesting.domain.enums.QuestPinMode;
import k4k.travelcorequesting.domain.models.QuestRequirement;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;


public final class MutableQuest implements Quest {
    private static final Identifier DEFAULT_QUEST_ICON = Identifier.of(TravelcoreQuesting.MOD_ID, "default");

    private Text title;
    private @Nullable Text description;
    private Identifier icon;
    private int index;
    private boolean repeatable;
    private QuestPinMode pinMode = QuestPinMode.AUTO;
    private @Nullable QuestRequirement require;
    private final List<List<Identifier>> dependencies;
    private final Map<String, MutableTask> tasks;
    private final List<List<String>> stages;  // NOTE: Should not have duplicate tasks in any one stage

    private final Set<String> unusedTasks;

    private MutableQuest(
            Text title,
            @Nullable Text description,
            Identifier icon,
            int index,
            boolean repeatable,
            List<List<Identifier>> dependencies,
            Map<String, MutableTask> tasks,
            List<List<String>> stages
    ) {
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.index = index;
        this.repeatable = repeatable;
        this.dependencies = dependencies;
        this.tasks = tasks;
        this.stages = stages;

        this.unusedTasks = new HashSet<>(this.tasks.keySet());
        this.stages.forEach(stage -> stage.forEach(this.unusedTasks::remove));
    }

    public static MutableQuest create(Text title) {
        return new MutableQuest(
                title,
                null,
                DEFAULT_QUEST_ICON,
                0,
                false,
                new ArrayList<>(),
                new HashMap<>(),
                new ArrayList<>()
        );
    }

    @Override
    public Text title() {
        return this.title;
    }

    public void setTitle(Text title) {
        this.title = title;
    }

    @Override
    public @Nullable Text description() {
        return this.description;
    }

    public void setDescription(@Nullable Text description) {
        this.description = description;
    }

    @Override
    public Identifier icon() {
        return this.icon;
    }

    public void setIcon(Identifier icon) {
        this.icon = icon;
    }

    @Override
    public int index() {
        return this.index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public boolean repeatable() {
        return this.repeatable;
    }

    public void setRepeatable(boolean repeatable) {
        this.repeatable = repeatable;
    }

    @Override
    public QuestPinMode getPinMode() {
        return this.pinMode;
    }

    public void setPinMode(QuestPinMode pinMode) {
        this.pinMode = pinMode;
    }

    @Override
    public @Nullable QuestRequirement getRequire() {
        return this.require;
    }

    public void setRequire(@Nullable QuestRequirement require) {
        this.require = require;
    }

    @Override
    public int getDependencyGroupsCount() {
        return this.dependencies.size();
    }

    @Override
    public List<Identifier> getDependencyGroup(int group) {
        return Collections.unmodifiableList(
                this.dependencies.get(group)
        );
    }

    public void addDependency(int groupIndex, Identifier dependency) {
        this.dependencies.get(groupIndex).add(dependency);
    }

    public void addDependency(Identifier dependency) {
        this.dependencies.add(Lists.newArrayList(dependency));
    }

    public void setDependencies(List<List<Identifier>> dependencies) {
        this.dependencies.clear();
        this.dependencies.addAll(
                dependencies.stream()
                        .map(ArrayList::new)
                        .toList()
        );
    }

    public void removeDependencies() {
        this.dependencies.clear();
    }

    @Override
    public Set<String> getTasks() {
        return Collections.unmodifiableSet(this.tasks.keySet());
    }

    @Override
    public @Nullable Task getTask(String taskId) {
        return tasks.get(taskId);
    }

    public MutableTask getTaskMutable(String taskId) {
        Objects.requireNonNull(taskId);

        return tasks.get(taskId);
    }

    public void setTask(String taskId, MutableTask task) {
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(task);

        // NOTE: Если задача заменяется, то она уже могла быть использована,
        //  не паримся добавлять её в неиспользованные
        if (!this.tasks.containsKey(taskId))
            this.unusedTasks.add(taskId);

        this.tasks.put(taskId, task);
    }

    /**
     * Удаляет задачу из квеста и из всех этапов. Если задача является
     * обязательной в этапе, весь этап удаляется
     * @param taskId Идентификатор задачи
     */
    public void removeTask(String taskId) {
        Objects.requireNonNull(taskId);

        this.tasks.remove(taskId);
        this.unusedTasks.remove(taskId);
        this.removeTaskFromAllStages(taskId);
    }

    @Override
    public boolean containsTask(String taskId) {
        return this.tasks.containsKey(taskId);
    }

    @Override
    public boolean containsTask(String taskId, int stage) {
        return this.containsTask(taskId)
                && this.stages.get(stage).contains(taskId);
    }

    @Override
    public int getTaskCount() {
        return this.tasks.size();
    }

    @Override
    public int getStageCount() {
        return this.stages.size();
    }

    @Override
    public List<String> getStage(int stage) {
        return Collections.unmodifiableList(
                this.stages.get(stage)
        );
    }

    @Override
    public String getRequiredTask(int stage) {  // TODO: Return optional and fix all the places where absence of required tasks not handled
        return this.stages.get(stage).get(0);
    }

    public void addTaskToStage(int stage, String taskId) {
        this.stages.get(stage).add(this.requireExistingTask(taskId));
        this.unusedTasks.remove(taskId);
    }

    public void addTaskToStage(String taskId) {
        this.stages.add(Lists.newArrayList(this.requireExistingTask(taskId)));
        this.unusedTasks.remove(taskId);
    }

    public void setStages(List<List<String>> stages) {
        this.stages.clear();
        this.stages.addAll(
                stages.stream()
                        .map(stage -> stage.stream()
                                .map(this::requireExistingTask)
                                .collect(Collectors.toList())
                        )
                        .toList()
        );
        this.stages.forEach(
                tasks -> tasks.forEach(this.unusedTasks::remove)
        );
    }

    /**
     * Удаляет задачу из всех этапов квеста. Если задача является
     * обязательной в этапе, весь этап удаляется
     * @param taskId Идентификатор задачи
     */
    public void removeTaskFromAllStages(String taskId) {
        // Remove stages where task is required
        this.stages.removeIf(group ->
                group.isEmpty() || Objects.equals(group.get(0), taskId)
        );

        // Remove task from stages where it is not required
        this.stages.forEach(group ->
                group.removeIf(item -> Objects.equals(item, taskId))
        );

        // Remove empty stages
        this.stages.removeIf(List::isEmpty);

        if (this.tasks.containsKey(taskId))
            this.unusedTasks.add(taskId);
    }

    @Override
    public Set<String> getUnusedTasks() {
        return Collections.unmodifiableSet(this.unusedTasks);
    }

    private String requireExistingTask(String taskId) {
        if (!this.tasks.containsKey(taskId))
            throw new IllegalArgumentException("Task is not in the quest");
        return taskId;
    }
}

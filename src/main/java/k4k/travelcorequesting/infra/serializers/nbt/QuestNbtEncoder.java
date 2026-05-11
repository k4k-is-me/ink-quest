package k4k.travelcorequesting.infra.serializers.nbt;

import k4k.travelcorequesting.common.NbtEncoder;
import k4k.travelcorequesting.domain.abstractions.ITaskCondition;
import k4k.travelcorequesting.domain.abstractions.Quest;
import k4k.travelcorequesting.domain.abstractions.Task;
import k4k.travelcorequesting.domain.enums.QuestPinMode;
import k4k.travelcorequesting.domain.models.MutableQuest;
import k4k.travelcorequesting.domain.models.MutableTask;
import k4k.travelcorequesting.domain.models.QuestRequirement;
import k4k.travelcorequesting.domain.models.TaskEventActions;
import k4k.travelcorequesting.domain.enums.CompletionStatus;
import k4k.travelcorequesting.domain.models.taskConditions.AllCondition;
import k4k.travelcorequesting.domain.models.taskConditions.AnyCondition;
import k4k.travelcorequesting.domain.models.taskConditions.NoneCondition;
import k4k.travelcorequesting.domain.models.taskConditions.PredicateCondition;
import k4k.travelcorequesting.domain.models.taskConditions.ScoreCondition;
import k4k.travelcorequesting.domain.models.taskConditions.TasksCondition;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class QuestNbtEncoder implements NbtEncoder<Quest, NbtCompound> {
    public NbtCompound encode(Quest quest) {
        var nbt = new NbtCompound();

        var dependenciesNbt = new NbtList();
        for (var depGroupId = 0; depGroupId < quest.getDependencyGroupsCount(); depGroupId++) {
            var dependencyGroupNbt = new NbtList();
            quest.getDependencyGroup(depGroupId).stream()
                    .map(Identifier::toString)
                    .map(NbtString::of)
                    .forEach(dependencyGroupNbt::add);
            dependenciesNbt.add(dependencyGroupNbt);
        }

        var stagesNbt = new NbtList();
        for (var stageId = 0; stageId < quest.getStageCount(); stageId++) {
            var stageNbt = new NbtList();
            quest.getStage(stageId).stream()
                    .map(NbtString::of)
                    .forEach(stageNbt::add);
            stagesNbt.add(stageNbt);
        }

        var tasksNbt = new NbtCompound();
        for (var taskId : quest.getTasks()) {
            tasksNbt.put(taskId, encodeDynamicTask(Objects.requireNonNull(quest.getTask(taskId))));
        }

        nbt.putString("title", Text.Serializer.toJson(quest.title()));
        if (quest.description() != null) nbt.putString("description", Text.Serializer.toJson(quest.description()));
        nbt.putString("icon", quest.icon().toString());
        nbt.putInt("index", quest.index());
        nbt.putBoolean("repeatable", quest.repeatable());
        nbt.putString("pin_mode", quest.getPinMode().name().toLowerCase());
        nbt.put("dependencies", dependenciesNbt);
        nbt.put("stages", stagesNbt);
        nbt.put("tasks", tasksNbt);

        var require = quest.getRequire();
        if (require != null) nbt.put("require", encodeRequire(require));

        return nbt;
    }

    private NbtCompound encodeRequire(QuestRequirement require) {
        var nbt = new NbtCompound();
        if (require.predicate() != null) nbt.putString("predicate", require.predicate().toString());
        nbt.put("tags", stringsToNbtList(require.tags()));
        return nbt;
    }

    private NbtCompound encodeDynamicTask(Task task) {
        var nbt = new NbtCompound();

        nbt.putString("title", Text.Serializer.toJson(task.title()));
        if (task.description() != null) nbt.putString("description", Text.Serializer.toJson(task.description()));

        nbt.put("onLoad", encodeEventActions(task.onLoad()));
        nbt.put("onTick", encodeEventActions(task.onTick()));
        nbt.put("onPinnedTick", encodeEventActions(task.onPinnedTick()));
        nbt.put("onUnload", encodeEventActions(task.onUnload()));
        nbt.put("onSuccess", encodeEventActions(task.onSuccess()));
        nbt.put("onFailure", encodeEventActions(task.onFailure()));

        var successCondition = task.successCondition();
        if (successCondition != null) nbt.put("successCondition", encodeDynamicCondition(successCondition));

        var failureCondition = task.failureCondition();
        if (failureCondition != null) nbt.put("failureCondition", encodeDynamicCondition(failureCondition));

        return nbt;
    }

    private NbtCompound encodeEventActions(TaskEventActions actions) {
        var nbt = new NbtCompound();
        nbt.put("functions", stringsToNbtList(
                actions.functions().stream().map(Identifier::toString).toList()
        ));
        nbt.put("tags", stringsToNbtList(actions.tags()));
        return nbt;
    }

    private NbtList stringsToNbtList(List<String> strings) {
        var list = new NbtList();
        strings.stream().map(NbtString::of).forEach(list::add);
        return list;
    }

    private NbtCompound encodeDynamicCondition(@NotNull ITaskCondition condition) {
        var nbt = new NbtCompound();

        if (condition instanceof PredicateCondition predicateCondition) {
            nbt.putString("type", "predicate");
            nbt.putString("predicate", predicateCondition.predicateId().toString());
        }
        else if (condition instanceof ScoreCondition scoreCondition) {
            nbt.putString("type", "score");
            nbt.putString("objective", scoreCondition.objective());
            nbt.putString("criterion", scoreCondition.criterion().getName());
            if (scoreCondition.player() != null) nbt.putString("player", scoreCondition.player());
            if (scoreCondition.initial() != null) nbt.putInt("initial", scoreCondition.initial());
            nbt.putInt("target", scoreCondition.target());
        }
        else if (condition instanceof AllCondition allCondition) {
            nbt.putString("type", "all");
            var subConditions = new NbtList();
            allCondition.subConditions().stream()
                    .map(this::encodeDynamicCondition)
                    .forEach(subConditions::add);
            nbt.put("conditions", subConditions);
        }
        else if (condition instanceof AnyCondition anyCondition) {
            nbt.putString("type", "any");
            var subConditions = new NbtList();
            anyCondition.subConditions().stream()
                    .map(this::encodeDynamicCondition)
                    .forEach(subConditions::add);
            nbt.put("conditions", subConditions);
        }
        else if (condition instanceof NoneCondition noneCondition) {
            nbt.putString("type", "none");
            var subConditions = new NbtList();
            noneCondition.subConditions().stream()
                    .map(this::encodeDynamicCondition)
                    .forEach(subConditions::add);
            nbt.put("conditions", subConditions);
        }
        else if (condition instanceof TasksCondition tasksCondition) {
            nbt.putString("type", "tasks");
            if (tasksCondition.status() != null)
                nbt.putString("status", tasksCondition.status().name().toLowerCase());
            if (tasksCondition.count() != null)
                nbt.putInt("count", tasksCondition.count());
            if (tasksCondition.tasks() != null && !tasksCondition.tasks().isEmpty())
                nbt.put("tasks", stringsToNbtList(tasksCondition.tasks()));
        }
        else throw new NotImplementedException("Conversion of %s to nbt is not implemented".formatted(condition.getClass().getSimpleName()));

        return nbt;
    }

    public MutableQuest decode(NbtCompound nbt) {
        var title = Text.Serializer.fromJson(nbt.getString("title"));
        var description = nbt.contains("description") ? Text.Serializer.fromJson(nbt.getString("description")) : null;
        var icon = Identifier.tryParse(nbt.getString("icon"));
        var index = nbt.getInt("index");
        var repeatable = nbt.getBoolean("repeatable");
        var pinMode = nbt.contains("pin_mode") ? switch (nbt.getString("pin_mode")) {
            case "force" -> QuestPinMode.FORCE;
            case "off" -> QuestPinMode.OFF;
            default -> QuestPinMode.AUTO;
        } : QuestPinMode.AUTO;

        var dependenciesNbt = nbt.getList("dependencies", NbtElement.LIST_TYPE);
        var dependencies = IntStream.range(0, dependenciesNbt.size())
                .mapToObj(dependenciesNbt::getList)
                .map(groupNbt -> IntStream.range(0, groupNbt.size())
                        .mapToObj(groupNbt::getString)
                        .map(Identifier::tryParse)
                        .toList())
                .toList();

        var stagesNbt = nbt.getList("stages", NbtElement.LIST_TYPE);
        var stages = IntStream.range(0, stagesNbt.size())
                .mapToObj(stagesNbt::getList)
                .map(stageNbt -> IntStream.range(0, stageNbt.size())
                        .mapToObj(stageNbt::getString)
                        .toList())
                .toList();

        var tasksNbt = nbt.getCompound("tasks");
        var tasks = tasksNbt.getKeys().stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        taskId -> decodeDynamicTask(tasksNbt.getCompound(taskId))
                ));

        var quest = MutableQuest.create(title);
        quest.setDescription(description);
        quest.setIcon(icon);
        quest.setIndex(index);
        quest.setRepeatable(repeatable);
        quest.setPinMode(pinMode);
        quest.setDependencies(dependencies);
        tasks.forEach(quest::setTask);
        quest.setStages(stages);

        if (nbt.contains("require")) quest.setRequire(decodeRequire(nbt.getCompound("require")));

        return quest;
    }

    private QuestRequirement decodeRequire(NbtCompound nbt) {
        var predicate = nbt.contains("predicate") ? Identifier.tryParse(nbt.getString("predicate")) : null;
        var tags = nbtListToStrings(nbt.getList("tags", NbtElement.STRING_TYPE));
        return new QuestRequirement(predicate, tags);
    }

    private MutableTask decodeDynamicTask(NbtCompound nbt) {
        var title = Text.Serializer.fromJson(nbt.getString("title"));
        var task = MutableTask.create(title);

        if (nbt.contains("description"))
            task.setDescription(Text.Serializer.fromJson(nbt.getString("description")));

        if (nbt.contains("onLoad")) task.setOnLoad(decodeEventActions(nbt.getCompound("onLoad")));
        if (nbt.contains("onTick")) task.setOnTick(decodeEventActions(nbt.getCompound("onTick")));
        if (nbt.contains("onPinnedTick")) task.setOnPinnedTick(decodeEventActions(nbt.getCompound("onPinnedTick")));
        if (nbt.contains("onUnload")) task.setOnUnload(decodeEventActions(nbt.getCompound("onUnload")));
        if (nbt.contains("onSuccess")) task.setOnSuccess(decodeEventActions(nbt.getCompound("onSuccess")));
        if (nbt.contains("onFailure")) task.setOnFailure(decodeEventActions(nbt.getCompound("onFailure")));

        if (nbt.contains("successCondition"))
            task.setSuccessCondition(decodeDynamicCondition(nbt.getCompound("successCondition")));
        if (nbt.contains("failureCondition"))
            task.setFailureCondition(decodeDynamicCondition(nbt.getCompound("failureCondition")));

        return task;
    }

    private TaskEventActions decodeEventActions(NbtCompound nbt) {
        var functions = nbtListToStrings(nbt.getList("functions", NbtElement.STRING_TYPE))
                .stream()
                .map(Identifier::tryParse)
                .filter(Objects::nonNull)
                .toList();
        var tags = nbtListToStrings(nbt.getList("tags", NbtElement.STRING_TYPE));
        return new TaskEventActions(functions, tags);
    }

    private List<String> nbtListToStrings(NbtList list) {
        var result = new ArrayList<String>(list.size());
        for (var i = 0; i < list.size(); i++) result.add(list.getString(i));
        return result;
    }

    private ITaskCondition decodeDynamicCondition(NbtCompound nbt) {
        var type = nbt.getString("type");

        switch (type) {
            case "predicate":
                return new PredicateCondition(
                        Identifier.tryParse(nbt.getString("predicate"))
                );
            case "score":
                var objective = nbt.getString("objective");
                var criterion = ScoreboardCriterion.getOrCreateStatCriterion(nbt.getString("criterion"))
                        .orElseThrow();
                var player = nbt.contains("player") ? nbt.getString("player") : null;
                var initial = nbt.contains("initial") ? nbt.getInt("initial") : null;
                var target = nbt.getInt("target");

                return new ScoreCondition(objective, criterion, player, initial, target);
            case "all":
                var conditionsNbt = nbt.getList("conditions", NbtElement.COMPOUND_TYPE);
                var subConditions = IntStream.range(0, conditionsNbt.size())
                        .mapToObj(conditionsNbt::getCompound)
                        .map(this::decodeDynamicCondition)
                        .collect(Collectors.toList());
                return new AllCondition(subConditions);
            case "any":
                var anyConditionsNbt = nbt.getList("conditions", NbtElement.COMPOUND_TYPE);
                var anySubConditions = IntStream.range(0, anyConditionsNbt.size())
                        .mapToObj(anyConditionsNbt::getCompound)
                        .map(this::decodeDynamicCondition)
                        .collect(Collectors.toList());
                return new AnyCondition(anySubConditions);
            case "none":
                var noneConditionsNbt = nbt.getList("conditions", NbtElement.COMPOUND_TYPE);
                var noneSubConditions = IntStream.range(0, noneConditionsNbt.size())
                        .mapToObj(noneConditionsNbt::getCompound)
                        .map(this::decodeDynamicCondition)
                        .collect(Collectors.toList());
                return new NoneCondition(noneSubConditions);
            case "tasks":
                var taskStatus = nbt.contains("status")
                        ? CompletionStatus.valueOf(nbt.getString("status").toUpperCase()) : null;
                var taskCount = nbt.contains("count") ? nbt.getInt("count") : null;
                var taskPool = nbt.contains("tasks")
                        ? nbtListToStrings(nbt.getList("tasks", NbtElement.STRING_TYPE)) : null;
                return new TasksCondition(taskStatus, taskCount, taskPool);
            default:
                throw new NotImplementedException("Decoding of condition type %s is not implemented".formatted(type));
        }
    }
}

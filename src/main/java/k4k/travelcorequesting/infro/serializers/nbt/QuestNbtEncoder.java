package k4k.travelcorequesting.infro.serializers.nbt;

import k4k.travelcorequesting.common.NbtEncoder;
import k4k.travelcorequesting.domain.abstractions.ITaskCondition;
import k4k.travelcorequesting.domain.abstractions.Quest;
import k4k.travelcorequesting.domain.abstractions.Task;
import k4k.travelcorequesting.domain.enums.QuestPinMode;
import k4k.travelcorequesting.domain.models.MutableQuest;
import k4k.travelcorequesting.domain.models.MutableTask;
import k4k.travelcorequesting.domain.models.taskConditions.AllCondition;
import k4k.travelcorequesting.domain.models.taskConditions.PredicateCondition;
import k4k.travelcorequesting.domain.models.taskConditions.ScoreCondition;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

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
        nbt.putBoolean("background", quest.background());
        nbt.putString("pin_mode", quest.getPinMode().name().toLowerCase());
        nbt.put("dependencies", dependenciesNbt);
        nbt.put("stages", stagesNbt);
        nbt.put("tasks", tasksNbt);

        return nbt;
    }

    private NbtCompound encodeDynamicTask(Task task) {
        var nbt = new NbtCompound();

        nbt.putString("title", Text.Serializer.toJson(task.title()));
        if (task.description() != null) nbt.putString("description", Text.Serializer.toJson(task.description()));
        if (task.loadFunction() != null) nbt.putString("load", Objects.requireNonNull(task.loadFunction()).toString());
        if (task.tickFunction() != null) nbt.putString("tick", Objects.requireNonNull(task.tickFunction()).toString());
        if (task.successFunction() != null) nbt.putString("successFunction", Objects.requireNonNull(task.successFunction()).toString());
        if (task.failureFunction() != null) nbt.putString("failureFunction", Objects.requireNonNull(task.failureFunction()).toString());
        if (task.unloadFunction() != null) nbt.putString("unload", Objects.requireNonNull(task.unloadFunction()).toString());
        nbt.putBoolean("successManual", task.isManualSuccess());
        nbt.putBoolean("failureManual", task.isManualFailure());

        var successCondition = task.successCondition();
        if (successCondition != null) nbt.put("successCondition", encodeDynamicCondition(successCondition));

        var failureCondition = task.failureCondition();
        if (failureCondition != null) nbt.put("failureCondition", encodeDynamicCondition(failureCondition));

        return nbt;
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
        else throw new NotImplementedException("Conversion of %s to nbt is not implemented".formatted(condition.getClass().getSimpleName()));

        return nbt;
    }

    public MutableQuest decode(NbtCompound nbt) {
        var title = Text.Serializer.fromJson(nbt.getString("title"));
        var description = nbt.contains("description") ? Text.Serializer.fromJson(nbt.getString("description")) : null;
        var icon = Identifier.tryParse(nbt.getString("icon"));
        var index = nbt.getInt("index");
        var background = nbt.getBoolean("background");
        var pinMode = nbt.contains("pin_mode") ? switch (nbt.getString("pin_mode")) {
            case "force" -> QuestPinMode.FORCE;
            case "off" -> QuestPinMode.OFF;
            default -> QuestPinMode.AUTO;
        } : QuestPinMode.AUTO;

        // Декодируем зависимости
        var dependenciesNbt = nbt.getList("dependencies", NbtElement.LIST_TYPE);
        var dependencies = IntStream.range(0, dependenciesNbt.size())
                .mapToObj(dependenciesNbt::getList)
                .map(groupNbt -> IntStream.range(0, groupNbt.size())
                        .mapToObj(groupNbt::getString)
                        .map(Identifier::tryParse)
                        .toList())
                .toList();

        // Декодируем этапы
        var stagesNbt = nbt.getList("stages", NbtElement.LIST_TYPE);
        var stages = IntStream.range(0, stagesNbt.size())
                .mapToObj(stagesNbt::getList)
                .map(stageNbt -> IntStream.range(0, stageNbt.size())
                        .mapToObj(stageNbt::getString)
                        .toList())
                .toList();

        // Декодируем задачи
        var tasksNbt = nbt.getCompound("tasks");
        var tasks = tasksNbt.getKeys().stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        taskId -> decodeDynamicTask(tasksNbt.getCompound(taskId))
                ));

        // Создаём квест через фабричный метод и затем настраиваем
        var quest = MutableQuest.create(title);
        quest.setDescription(description);
        quest.setIcon(icon);
        quest.setIndex(index);
        quest.setBackground(background);
        quest.setPinMode(pinMode);
        quest.setDependencies(dependencies);
        // Добавляем задачи
        tasks.forEach(quest::setTask);

        quest.setStages(stages);

        return quest;
    }

    private MutableTask decodeDynamicTask(NbtCompound nbt) {
        var title = Text.Serializer.fromJson(nbt.getString("title"));
        var task = MutableTask.create(title);

        if (nbt.contains("description"))
            task.setDescription(Text.Serializer.fromJson(nbt.getString("description")));
        if (nbt.contains("load"))
            task.setLoadFunction(Identifier.tryParse(nbt.getString("load")));
        if (nbt.contains("tick"))
            task.setTickFunction(Identifier.tryParse(nbt.getString("tick")));
        if (nbt.contains("successFunction"))
            task.setSuccessFunction(Identifier.tryParse(nbt.getString("successFunction")));
        if (nbt.contains("failureFunction"))
            task.setFailureFunction(Identifier.tryParse(nbt.getString("failureFunction")));
        if (nbt.contains("unload"))
            task.setUnloadFunction(Identifier.tryParse(nbt.getString("unload")));
        if (nbt.contains("successCondition"))
            task.setSuccessCondition(nbt.contains("successCondition") ? decodeDynamicCondition(nbt.getCompound("successCondition")) : null);
        if (nbt.contains("failureCondition"))
            task.setFailureCondition(nbt.contains("failureCondition") ? decodeDynamicCondition(nbt.getCompound("failureCondition")) : null);

        task.setManualSuccess(nbt.getBoolean("successManual"));
        task.setManualFailure(nbt.getBoolean("failureManual"));

        return task;
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
            default:
                throw new NotImplementedException("Decoding of condition type %s is not implemented".formatted(type));
        }
    }
}

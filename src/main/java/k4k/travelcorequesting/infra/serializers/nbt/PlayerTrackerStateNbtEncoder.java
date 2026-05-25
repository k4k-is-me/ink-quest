package k4k.travelcorequesting.infra.serializers.nbt;

import k4k.travelcorequesting.common.NbtEncoder;
import k4k.travelcorequesting.domain.enums.CompletionStatus;
import k4k.travelcorequesting.questing.states.PlayerTrackerState;
import k4k.travelcorequesting.questing.states.QuestTrackerState;
import k4k.travelcorequesting.questing.states.TaskTrackerState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.HashSet;

public class PlayerTrackerStateNbtEncoder implements NbtEncoder<PlayerTrackerState, NbtCompound> {
    public NbtCompound encode(PlayerTrackerState state) {
        var nbt = new NbtCompound();
        var activeNbt = new NbtCompound();
        var completeNbt = new NbtCompound();

        for (var questTrackerEntry : state.activeQuestsTrackers().entrySet()) {
            activeNbt.put(questTrackerEntry.getKey().toString(), encodeQuestState(questTrackerEntry.getValue()));
        }

        for (var completeQuestEntry : state.completedQuests().entrySet()) {
            completeNbt.putString(completeQuestEntry.getKey().toString(), completeQuestEntry.getValue().toString());
        }

        nbt.put("ActiveQuests", activeNbt);
        nbt.put("CompleteQuests", completeNbt);

        return nbt;
    }

    private static NbtCompound encodeQuestState(QuestTrackerState state) {
        var nbt = new NbtCompound();
        var activeNbt = new NbtCompound();
        var completeNbt = new NbtCompound();

        if (state.activeStage() != null)
            nbt.putInt("ActiveStage", state.activeStage());

        if (state.pinnedTaskId() != null)
            nbt.putString("PinnedTaskId", state.pinnedTaskId());

        for (var taskTrackerEntry : state.activeTasksTrackers().entrySet()) {
            var taskNbt = new NbtCompound();
            taskNbt.putInt("SuccessValue", taskTrackerEntry.getValue().successProgress());
            taskNbt.putInt("FailureValue", taskTrackerEntry.getValue().failureProgress());

            activeNbt.put(taskTrackerEntry.getKey(), taskNbt);
        }

        for (var completeTaskEntry : state.completeTasks().entrySet()) {
            completeNbt.putString(completeTaskEntry.getKey(), completeTaskEntry.getValue().toString());
        }

        var loadedTasksNbt = new NbtList();
        state.loadedTasks().forEach(id -> loadedTasksNbt.add(NbtString.of(id)));

        nbt.put("ActiveTasks", activeNbt);
        nbt.put("CompleteTasks", completeNbt);
        nbt.put("LoadedTasks", loadedTasksNbt);
        nbt.putBoolean("Viewed", state.viewed());

        return nbt;
    }

    public PlayerTrackerState decode(NbtCompound nbt) {
        var activeNbt = nbt.getCompound("ActiveQuests");
        var completeNbt = nbt.getCompound("CompleteQuests");

        var active = new HashMap<Identifier, QuestTrackerState>(activeNbt.getSize());
        for (var questKey : activeNbt.getKeys()) {
            active.put(Identifier.tryParse(questKey), decodeQuestState(activeNbt.getCompound(questKey)));
        }

        var complete = new HashMap<Identifier, CompletionStatus>(completeNbt.getSize());
        for (var questKey : completeNbt.getKeys()) {
            complete.put(Identifier.tryParse(questKey), CompletionStatus.valueOf(completeNbt.getString(questKey)));
        }

        return new PlayerTrackerState(active, complete);
    }

    private static QuestTrackerState decodeQuestState(NbtCompound nbt) {
        var activeStage = nbt.contains("ActiveStage") ? nbt.getInt("ActiveStage") : null;
        var pinnedTaskId = nbt.contains("PinnedTaskId") ? nbt.getString("PinnedTaskId") : null;
        var activeNbt = nbt.getCompound("ActiveTasks");
        var completeNbt = nbt.getCompound("CompleteTasks");

        var activeTasksTrackers = new HashMap<String, TaskTrackerState>(activeNbt.getSize());
        for (var key : activeNbt.getKeys()) {
            var taskNbt = activeNbt.getCompound(key);
            var success = taskNbt.getInt("SuccessValue");
            var failure = taskNbt.getInt("FailureValue");
            activeTasksTrackers.put(key, new TaskTrackerState(success, failure));
        }

        var completeTasks = new HashMap<String, CompletionStatus>(completeNbt.getSize());
        for (var key : completeNbt.getKeys()) {
            completeTasks.put(key, CompletionStatus.valueOf(completeNbt.getString(key)));
        }

        var loadedTasks = new HashSet<String>();
        if (nbt.contains("LoadedTasks")) {
            var loadedNbt = nbt.getList("LoadedTasks", NbtElement.STRING_TYPE);
            for (var i = 0; i < loadedNbt.size(); i++) loadedTasks.add(loadedNbt.getString(i));
        }

        var viewed = nbt.contains("Viewed") && nbt.getBoolean("Viewed");

        return new QuestTrackerState(activeStage, pinnedTaskId, completeTasks, activeTasksTrackers, loadedTasks, viewed);
    }
}

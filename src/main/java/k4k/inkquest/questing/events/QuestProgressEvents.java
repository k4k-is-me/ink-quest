package k4k.inkquest.questing.events;

import k4k.inkquest.domain.enums.CompletionStatus;
import k4k.inkquest.questing.models.QuestEntry;
import k4k.inkquest.questing.models.TaskEntry;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

public class QuestProgressEvents {
    /// Квест был выдан игроку
    public static final Event<QuestGiven> QUEST_GIVEN = EventFactory.createArrayBacked(QuestGiven.class, (callbacks) -> (questEntry, player) -> {
        for (var event : callbacks) {
            event.onQuestGive(questEntry, player);
        }
    });

    /// Квест был снят с игрока командой
    public static final Event<QuestDropped> QUEST_DROPPED = EventFactory.createArrayBacked(QuestDropped.class, (callbacks) -> (questEntry, player) -> {
        for (var event : callbacks) {
            event.onQuestDrop(questEntry, player);
        }
    });

    public static final Event<QuestComplete> QUEST_COMPLETED = EventFactory.createArrayBacked(QuestComplete.class, (callbacks) -> (questEntry, status, player) -> {
        for (var event : callbacks) {
            event.onQuestCompletion(questEntry, status, player);
        }
    });

    public static final Event<StageChanged> STAGE_CHANGED = EventFactory.createArrayBacked(StageChanged.class, (callbacks) -> (questEntry, newStage, player) -> {
        for (var event : callbacks) {
            event.onStageChange(questEntry, newStage, player);
        }
    });

    public static final Event<TaskLoaded> TASK_LOADED = EventFactory.createArrayBacked(TaskLoaded.class, (callbacks) -> (taskEntry, player, stageChanged) -> {
        for (var event : callbacks) {
            event.onTaskLoad(taskEntry, player, stageChanged);
        }
    });

    public static final Event<TaskTick> TASK_TICKED = EventFactory.createArrayBacked(TaskTick.class, (callbacks) -> (taskEntry, player) -> {
        for (var event : callbacks) {
            event.onTaskTick(taskEntry, player);
        }
    });

    public static final Event<TaskTick> PINNED_TASK_TICKED = EventFactory.createArrayBacked(TaskTick.class, (callbacks) -> (taskEntry, player) -> {
        for (var event : callbacks) {
            event.onTaskTick(taskEntry, player);
        }
    });

    // С unload могут быть технические трудности. Дело в том, что для выполнения unload нужен игрок, а его
    // может не быть на момент выполнения remove задачи (когда unload бы вызывался), а на момент автоматического
    // вызова unload (как это происходит с load) задачи уже не будет.
    public static final Event<TaskUnloaded> TASK_UNLOADED = EventFactory.createArrayBacked(TaskUnloaded.class, (callbacks) -> (taskEntry, player, stageChanged) -> {
        for (var event : callbacks) {
            event.onTaskUnload(taskEntry, player, stageChanged);
        }
    });

    public static final Event<TaskCompleted> TASK_COMPLETED = EventFactory.createArrayBacked(TaskCompleted.class, (callbacks) -> (taskEntry, player, status) -> {
        for (var event : callbacks) {
            event.onTaskCompletion(taskEntry, player, status);
        }
    });

    public static final Event<TaskProgressChanged> TASK_SUCCESS_PROGRESS_CHANGED = EventFactory.createArrayBacked(TaskProgressChanged.class, (callbacks) -> (taskEntry, player, newValue) -> {
        for (var event : callbacks) {
            event.onTaskProgressChange(taskEntry, player, newValue);
        }
    });

    public static final Event<TaskProgressChanged> TASK_FAILURE_PROGRESS_CHANGED = EventFactory.createArrayBacked(TaskProgressChanged.class, (callbacks) -> (taskEntry, player, newValue) -> {
        for (var event : callbacks) {
            event.onTaskProgressChange(taskEntry, player, newValue);
        }
    });


    @FunctionalInterface
    public interface QuestGiven {
        void onQuestGive(QuestEntry questEntry, ServerPlayerEntity player);
    }

    @FunctionalInterface
    public interface QuestDropped {
        void onQuestDrop(QuestEntry questEntry, ServerPlayerEntity player);
    }

    @FunctionalInterface
    public interface QuestComplete {
        void onQuestCompletion(QuestEntry questEntry, CompletionStatus status, ServerPlayerEntity player);
    }

    @FunctionalInterface
    public interface StageChanged {
        void onStageChange(QuestEntry questEntry, @Nullable Integer newStage, ServerPlayerEntity player);
    }

    @FunctionalInterface
    public interface TaskLoaded {
        void onTaskLoad(TaskEntry taskEntry, ServerPlayerEntity player, boolean stageChanged);
    }

    @FunctionalInterface
    public interface TaskTick {
        void onTaskTick(TaskEntry taskEntry, ServerPlayerEntity player);
    }

    @FunctionalInterface
    public interface TaskUnloaded {
        void onTaskUnload(TaskEntry taskEntry, ServerPlayerEntity player, boolean stageChanged);
    }

    @FunctionalInterface
    public interface TaskCompleted {
        void onTaskCompletion(TaskEntry taskEntry, ServerPlayerEntity player, CompletionStatus status);
    }

    @FunctionalInterface
    public interface TaskProgressChanged {
        void onTaskProgressChange(TaskEntry taskEntry, ServerPlayerEntity player, int newValue);
    }
}

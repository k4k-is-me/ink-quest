package k4k.travelcorequesting.questing.events;

import k4k.travelcorequesting.questing.models.QuestEntry;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class QuestEvents {
    public static final Event<QuestsReloaded> QUESTS_RELOADED = EventFactory.createArrayBacked(QuestsReloaded.class, (callbacks) -> () -> {
        for (var event : callbacks) {
            event.onReload();
        }
    });

    // NOTE: Может быть не нужно
    public static final Event<QuestCreated> QUEST_CREATED = EventFactory.createArrayBacked(QuestCreated.class, (callbacks) -> (questEntry) -> {
        for (var event : callbacks) {
            event.onQuestCreation(questEntry);
        }
    });

    /// Квест был изменён
    public static final Event<QuestModified> QUEST_MODIFIED = EventFactory.createArrayBacked(QuestModified.class, (callbacks) -> (questEntry) -> {
        for (var event : callbacks) {
            event.onQuestModification(questEntry);
        }
    });

    public static final Event<QuestPinned> QUEST_PINNED = EventFactory.createArrayBacked(QuestPinned.class, callbacks -> (questEntry, player) -> {
        for (var event : callbacks) {
            event.onQuestPin(questEntry, player);
        }
    });

    public static final Event<QuestUnpinned> QUEST_UNPINNED = EventFactory.createArrayBacked(QuestUnpinned.class, callbacks -> (questEntry, player) -> {
        for (var event : callbacks) {
            event.onQuestUnpin(questEntry, player);
        }
    });

    public static final Event<QuestRemoved> QUEST_REMOVED = EventFactory.createArrayBacked(QuestRemoved.class, (callbacks) -> (questId) -> {
        for (var event : callbacks) {
            event.onQuestRemoval(questId);
        }
    });

    /// Закреплённая задача квеста установлена — при первом закреплении квеста и при смене задачи
    public static final Event<TaskPinned> TASK_PINNED = EventFactory.createArrayBacked(TaskPinned.class, callbacks -> (questId, taskId, player) -> {
        for (var event : callbacks) {
            event.onTaskPin(questId, taskId, player);
        }
    });


    @FunctionalInterface
    public interface QuestsReloaded {
        void onReload();
    }

    @FunctionalInterface
    public interface QuestCreated {
        void onQuestCreation(QuestEntry questEntry);
    }

    @FunctionalInterface
    public interface QuestModified {
        void onQuestModification(QuestEntry questEntry);
    }

    @FunctionalInterface
    public interface QuestPinned {
        void onQuestPin(QuestEntry questEntry, ServerPlayerEntity player);
    }

    @FunctionalInterface
    public interface QuestRemoved {
        void onQuestRemoval(Identifier questId);
    }

    @FunctionalInterface
    public interface QuestUnpinned {
        void onQuestUnpin(Identifier questId, ServerPlayerEntity player);
    }

    @FunctionalInterface
    public interface TaskPinned {
        void onTaskPin(Identifier questId, String taskId, ServerPlayerEntity player);
    }
}

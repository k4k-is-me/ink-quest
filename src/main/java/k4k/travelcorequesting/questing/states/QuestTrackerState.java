package k4k.travelcorequesting.questing.states;

import k4k.travelcorequesting.domain.enums.CompletionStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

public record QuestTrackerState (
        @Nullable Integer activeStage,
        @Nullable String pinnedTaskId,
        Map<String, CompletionStatus> completeTasks,
        Map<String, TaskTrackerState> activeTasksTrackers,
        Set<String> loadedTasks,
        boolean viewed
) {}


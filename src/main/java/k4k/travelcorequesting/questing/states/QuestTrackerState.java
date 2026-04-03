package k4k.travelcorequesting.questing.states;

import k4k.travelcorequesting.domain.enums.CompletionStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public record QuestTrackerState (
        @Nullable Integer activeStage,
        @Nullable String pinnedTaskId,
        Map<String, CompletionStatus> completeTasks,
        Map<String, TaskTrackerState> activeTasksTrackers
) {}


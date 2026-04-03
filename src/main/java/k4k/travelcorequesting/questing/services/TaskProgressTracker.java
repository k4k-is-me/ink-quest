package k4k.travelcorequesting.questing.services;

import k4k.travelcorequesting.questing.states.TaskTrackerState;

public final class TaskProgressTracker {
    public int successProgress;
    public int failureProgress;

    public TaskProgressTracker(int successProgress, int failureProgress) {
        this.successProgress = successProgress;
        this.failureProgress = failureProgress;
    }

    public static TaskProgressTracker create(TaskTrackerState state) {
        return new TaskProgressTracker(state.successProgress(), state.failureProgress());
    }

    public TaskTrackerState saveState() {
        return new TaskTrackerState(
                this.successProgress,
                this.failureProgress
        );
    }
}

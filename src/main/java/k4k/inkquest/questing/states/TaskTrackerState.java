package k4k.inkquest.questing.states;

public record TaskTrackerState (
        int successProgress,
        int failureProgress
) {}

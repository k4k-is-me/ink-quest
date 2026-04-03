package k4k.travelcorequesting.questing.states;

public record TaskTrackerState (
        int successProgress,
        int failureProgress
) {}

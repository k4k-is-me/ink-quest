package k4k.travelcorequesting.questing.states;

import k4k.travelcorequesting.domain.enums.CompletionStatus;
import net.minecraft.util.Identifier;

import java.util.Map;

public record PlayerTrackerState (
        Map<Identifier, QuestTrackerState> activeQuestsTrackers,
        Map<Identifier, CompletionStatus> completedQuests
) {}

package k4k.inkquest.questing.states;

import k4k.inkquest.domain.enums.CompletionStatus;
import net.minecraft.util.Identifier;

import java.util.Map;

public record PlayerTrackerState (
        Map<Identifier, QuestTrackerState> activeQuestsTrackers,
        Map<Identifier, CompletionStatus> completedQuests
) {}

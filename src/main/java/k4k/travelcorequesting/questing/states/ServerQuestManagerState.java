package k4k.travelcorequesting.questing.states;

import k4k.travelcorequesting.domain.models.MutableQuest;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.UUID;

public record ServerQuestManagerState (
        Map<UUID, PlayerTrackerState> playerTrackers,
        Map<Identifier, MutableQuest> dynamicQuests
) {}

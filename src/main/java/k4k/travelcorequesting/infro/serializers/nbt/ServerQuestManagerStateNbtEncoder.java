package k4k.travelcorequesting.infro.serializers.nbt;

import k4k.travelcorequesting.common.NbtEncoder;
import k4k.travelcorequesting.domain.models.MutableQuest;
import k4k.travelcorequesting.questing.states.PlayerTrackerState;
import k4k.travelcorequesting.questing.states.ServerQuestManagerState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

import java.util.*;


public class ServerQuestManagerStateNbtEncoder implements NbtEncoder<ServerQuestManagerState, NbtCompound> {
    private static final PlayerTrackerStateNbtEncoder playerTrackerStateEncoder = new PlayerTrackerStateNbtEncoder();
    private static final QuestNbtEncoder questEncoder = new QuestNbtEncoder();

    public NbtCompound encode(ServerQuestManagerState state) {
        var nbt = new NbtCompound();
        var playersNbt = new NbtCompound();
        var questsNbt = new NbtCompound();

        for (var playerTrackerEntry : state.playerTrackers().entrySet()) {
            playersNbt.put(
                    playerTrackerEntry.getKey().toString(),
                    playerTrackerStateEncoder.encode(playerTrackerEntry.getValue())
            );
        }

        for (var questEntry : state.dynamicQuests().entrySet()) {
            questsNbt.put(
                    questEntry.getKey().toString(),
                    questEncoder.encode(questEntry.getValue())
            );
        }

        nbt.put("TrackedPlayers", playersNbt);
        nbt.put("DynamicQuests", questsNbt);

        return nbt;
    }

    public ServerQuestManagerState decode(NbtCompound nbt) {
        var playerNbt = nbt.getCompound("TrackedPlayers");
        var questsNbt = nbt.getCompound("DynamicQuests");

        var players = new HashMap<UUID, PlayerTrackerState>(playerNbt.getSize());  // TODO: Check if getSize actually returns size
        for (var playerKey : playerNbt.getKeys()) {
            players.put(UUID.fromString(playerKey), playerTrackerStateEncoder.decode(playerNbt.getCompound(playerKey)));
        }

        var dynamicQuests = new HashMap<Identifier, MutableQuest>(questsNbt.getSize());
        for (var questKey : questsNbt.getKeys()) {
            dynamicQuests.put(
                    Identifier.tryParse(questKey),
                    questEncoder.decode(questsNbt.getCompound(questKey))
            );
        }

        return new ServerQuestManagerState(players, dynamicQuests);
    }
}

package k4k.travelcorequesting.infro.loaders;

import k4k.travelcorequesting.TravelcoreQuesting;
import k4k.travelcorequesting.infro.serializers.nbt.ServerQuestManagerStateNbtEncoder;
import k4k.travelcorequesting.questing.abstractions.ServerQuestManagerContainer;
import k4k.travelcorequesting.questing.services.ServerQuestManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.Objects;

public class QuestingPersistentStateAdapter extends PersistentState {
    private static final String FILENAME = TravelcoreQuesting.MOD_ID;
    private static final ServerQuestManagerStateNbtEncoder encoder = new ServerQuestManagerStateNbtEncoder();
    private final ServerQuestManager questManager;

    public QuestingPersistentStateAdapter(ServerQuestManager questManager) {
        this.questManager = questManager;
    }

    @Override
    public boolean isDirty() {
        return this.questManager.modifiedSinceLastSave();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        var state = this.questManager.saveState();
        nbt.put("State", encoder.encode(state));
        return nbt;
    }

    private static QuestingPersistentStateAdapter readNbt(NbtCompound nbt, ServerQuestManager questManager) {
        questManager.loadState(encoder.decode(nbt.getCompound("State")));
        return new QuestingPersistentStateAdapter(questManager);
    }

    public static void register(MinecraftServer server) {
        var questManager = ((ServerQuestManagerContainer) server).travelcorequesting$getQuestManager();

        var persistentStateManager = Objects.requireNonNull(server.getWorld(World.OVERWORLD))
                .getPersistentStateManager();

        // NOTE: Автоматически создаст state и загрузит его, поставит адаптер на учёт и будет вызывать writeNbt
        //  каждый раз при необходимости сохранения
        persistentStateManager.getOrCreate(
                nbt -> QuestingPersistentStateAdapter.readNbt(nbt, questManager),
                () -> new QuestingPersistentStateAdapter(questManager),
                FILENAME
        );
    }
}

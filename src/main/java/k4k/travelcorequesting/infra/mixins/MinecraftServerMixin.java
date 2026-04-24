package k4k.travelcorequesting.infra.mixins;

import k4k.travelcorequesting.infra.checkers.QuestRequirementChecker;
import k4k.travelcorequesting.questing.services.ServerQuestManager;
import k4k.travelcorequesting.questing.abstractions.ServerQuestManagerContainer;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = MinecraftServer.class)
public abstract class MinecraftServerMixin implements ServerQuestManagerContainer {
    @Unique
    private final ServerQuestManager questManager = new ServerQuestManager(new QuestRequirementChecker());

    @Override
    public ServerQuestManager travelcorequesting$getQuestManager() {
        return this.questManager;
    }
}

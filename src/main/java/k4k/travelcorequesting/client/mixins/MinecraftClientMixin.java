package k4k.travelcorequesting.client.mixins;

import k4k.travelcorequesting.client.ClientQuestManager;
import k4k.travelcorequesting.client.interfaces.ClientQuestManagerContainer;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = MinecraftClient.class)
public abstract class MinecraftClientMixin implements ClientQuestManagerContainer {
    @Unique
    private final ClientQuestManager questManager = new ClientQuestManager();

    public ClientQuestManager travelcorequesting$getQuestManager() {
        return this.questManager;
    }
}

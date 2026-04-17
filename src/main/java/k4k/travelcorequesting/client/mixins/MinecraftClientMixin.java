package k4k.travelcorequesting.client.mixins;

import k4k.travelcorequesting.client.ClientQuestBookManager;
import k4k.travelcorequesting.client.interfaces.ClientQuestBookManagerContainer;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = MinecraftClient.class)
public abstract class MinecraftClientMixin implements ClientQuestBookManagerContainer {
    @Unique
    private final ClientQuestBookManager questManager = new ClientQuestBookManager();

    public ClientQuestBookManager travelcorequesting$getQuestManager() {
        return this.questManager;
    }
}

package k4k.inkquest.client.mixins;

import k4k.inkquest.client.ClientQuestBookManager;
import k4k.inkquest.client.interfaces.ClientQuestBookManagerContainer;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = MinecraftClient.class)
public abstract class MinecraftClientMixin implements ClientQuestBookManagerContainer {
    @Unique
    private final ClientQuestBookManager questManager = new ClientQuestBookManager();

    public ClientQuestBookManager inkquest$getQuestManager() {
        return this.questManager;
    }
}

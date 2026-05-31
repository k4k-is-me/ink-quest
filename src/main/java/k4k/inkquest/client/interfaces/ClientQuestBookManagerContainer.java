package k4k.inkquest.client.interfaces;

import k4k.inkquest.client.ClientQuestBookManager;
import net.minecraft.client.MinecraftClient;

public interface ClientQuestBookManagerContainer {
    ClientQuestBookManager inkquest$getQuestManager();

    static ClientQuestBookManager getQuestManager(MinecraftClient client) {
        var container = (ClientQuestBookManagerContainer) client;
        return container.inkquest$getQuestManager();
    }
}

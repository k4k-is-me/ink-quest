package k4k.travelcorequesting.client.interfaces;

import k4k.travelcorequesting.client.ClientQuestBookManager;
import net.minecraft.client.MinecraftClient;

public interface ClientQuestBookManagerContainer {
    ClientQuestBookManager travelcorequesting$getQuestManager();

    static ClientQuestBookManager getQuestManager(MinecraftClient client) {
        var container = (ClientQuestBookManagerContainer) client;
        return container.travelcorequesting$getQuestManager();
    }
}

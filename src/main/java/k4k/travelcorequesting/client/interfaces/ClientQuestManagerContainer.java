package k4k.travelcorequesting.client.interfaces;

import k4k.travelcorequesting.client.ClientQuestManager;
import net.minecraft.client.MinecraftClient;

public interface ClientQuestManagerContainer {
    ClientQuestManager travelcorequesting$getQuestManager();

    static ClientQuestManager getQuestManager(MinecraftClient client) {
        var container = (ClientQuestManagerContainer) client;
        return container.travelcorequesting$getQuestManager();
    }
}

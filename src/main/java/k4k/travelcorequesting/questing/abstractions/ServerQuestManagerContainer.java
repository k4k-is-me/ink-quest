package k4k.travelcorequesting.questing.abstractions;

import k4k.travelcorequesting.questing.services.ServerQuestManager;
import net.minecraft.server.MinecraftServer;

public interface ServerQuestManagerContainer {
    ServerQuestManager travelcorequesting$getQuestManager();

    static ServerQuestManager getQuestManager(MinecraftServer server) {
        var container = (ServerQuestManagerContainer) server;
        return container.travelcorequesting$getQuestManager();
    }
}

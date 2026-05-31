package k4k.inkquest.questing.abstractions;

import k4k.inkquest.questing.services.ServerQuestManager;
import net.minecraft.server.MinecraftServer;

public interface ServerQuestManagerContainer {
    ServerQuestManager inkquest$getQuestManager();

    static ServerQuestManager getQuestManager(MinecraftServer server) {
        var container = (ServerQuestManagerContainer) server;
        return container.inkquest$getQuestManager();
    }
}

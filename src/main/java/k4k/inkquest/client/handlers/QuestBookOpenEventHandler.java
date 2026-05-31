package k4k.inkquest.client.handlers;

import k4k.inkquest.client.TravelcoreQuestingKeybinds;
import k4k.inkquest.infra.networking.QuestBookOpenRequestC2SPacket;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

public class QuestBookOpenEventHandler {
    public static void register() {
        ClientTickEvents.START_CLIENT_TICK.register(QuestBookOpenEventHandler::handle);
    }

    private static void handle(MinecraftClient client) {
        while (TravelcoreQuestingKeybinds.OPEN_QUESTS.wasPressed()) {
            if (client.player == null) continue;
            ClientPlayNetworking.send(new QuestBookOpenRequestC2SPacket());
        }
    }
}

package k4k.travelcorequesting.client.handlers;

import k4k.travelcorequesting.client.TravelcoreQuestingKeybinds;
import k4k.travelcorequesting.client.screens.QuestBookQuestsScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class QuestBookOpenEventHandler {
    public static void register() {
        ClientTickEvents.START_CLIENT_TICK.register(QuestBookOpenEventHandler::handle);
    }

    private static void handle(MinecraftClient client) {
        while (TravelcoreQuestingKeybinds.OPEN_QUESTS.wasPressed()) {
            client.setScreen(new QuestBookQuestsScreen());
        }
    }
}

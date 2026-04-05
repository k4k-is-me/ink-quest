package k4k.travelcorequesting;

import k4k.travelcorequesting.client.TravelcoreQuestingKeybinds;
import k4k.travelcorequesting.client.handlers.QuestBookOpenEventHandler;
import k4k.travelcorequesting.client.huds.QuestHudOverlay;
import k4k.travelcorequesting.client.interfaces.ClientQuestManagerContainer;
import k4k.travelcorequesting.infro.networking.HudChangeTaskProgressS2CPacket;
import k4k.travelcorequesting.infro.networking.HudRemoveQuestS2CPacket;
import k4k.travelcorequesting.infro.networking.HudSetQuestStageS2CPacket;
import k4k.travelcorequesting.infro.networking.HudTaskCompleteS2CPacket;
import k4k.travelcorequesting.infro.networking.HudTaskPinS2CPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;

public class TravelcoreQuestingClient implements ClientModInitializer {
    public static final QuestHudOverlay QUEST_HUD_OVERLAY = new QuestHudOverlay();

    @Override
    public void onInitializeClient() {
        registerKeybindings();
        registerEventListeners();

        HudRenderCallback.EVENT.register(QUEST_HUD_OVERLAY);

        ClientPlayNetworking.registerGlobalReceiver(HudSetQuestStageS2CPacket.TYPE, (packet, player, sender) -> {
            var client = MinecraftClient.getInstance();
            client.execute(() -> QUEST_HUD_OVERLAY.addQuest(packet.questId(), packet.quest(), packet.tasks()));
        });

//        ClientPlayNetworking.registerGlobalReceiver(TaskShowS2CPacket.TYPE, (packet, player, sender) -> {
//            var client = MinecraftClient.getInstance();
//            client.execute(() -> QUEST_HUD_OVERLAY.addTask(packet.questId(), packet.taskId(), packet.task()));
//        });

        ClientPlayNetworking.registerGlobalReceiver(HudTaskCompleteS2CPacket.TYPE, (packet, player, sender) -> {
            var client = MinecraftClient.getInstance();
            client.execute(() -> QUEST_HUD_OVERLAY.completeTask(packet.questId(), packet.taskId(), packet.status()));
        });

        ClientPlayNetworking.registerGlobalReceiver(HudRemoveQuestS2CPacket.TYPE, (packet, player, sender) -> {
            var client = MinecraftClient.getInstance();
            client.execute(() -> QUEST_HUD_OVERLAY.removeQuest(packet.questId()));
        });

        ClientPlayNetworking.registerGlobalReceiver(HudChangeTaskProgressS2CPacket.TYPE, (packet, player, sender) -> {
            var client = MinecraftClient.getInstance();
            client.execute(() -> QUEST_HUD_OVERLAY.setTaskProgress(packet.questId(), packet.taskId(), packet.value(), packet.isSuccessProgress()));
        });

        ClientPlayNetworking.registerGlobalReceiver(HudTaskPinS2CPacket.TYPE, (packet, player, sender) -> {
            var client = MinecraftClient.getInstance();
            client.execute(() -> QUEST_HUD_OVERLAY.setTaskPin(packet.questId(), packet.taskId()));
        });
    }

    private void registerKeybindings() {
        TravelcoreQuestingKeybinds.register();
    }

    private void registerEventListeners() {
        QuestBookOpenEventHandler.register();

        ClientPlayConnectionEvents.JOIN.register((network, sender, client) -> {
            TravelcoreQuesting.LOGGER.info("Player joined the game. Sending sync request to the server");

            ClientQuestManagerContainer.getQuestManager(client)
                    .syncWithServer();
        });
    }

}

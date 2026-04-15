package k4k.travelcorequesting;

import k4k.travelcorequesting.client.TravelcoreQuestingKeybinds;
import k4k.travelcorequesting.infra.networking.*;
import k4k.travelcorequesting.infra.requests.GetQuestDetailsClientRequest;
import k4k.travelcorequesting.client.handlers.QuestBookOpenEventHandler;
import k4k.travelcorequesting.client.huds.QuestHudOverlay;
import k4k.travelcorequesting.client.interfaces.ClientQuestManagerContainer;
import k4k.travelcorequesting.infra.networking.QuestBookQuestCompletedS2CPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;

public class TravelcoreQuestingClient implements ClientModInitializer {
    public static final QuestHudOverlay QUEST_HUD_OVERLAY = new QuestHudOverlay();

    @Override
    public void onInitializeClient() {
        GetQuestDetailsClientRequest.register();
        GetQuestDetailsClientRequest.INSTANCE.registerClient();

        registerKeybindings();
        registerEventListeners();

        HudRenderCallback.EVENT.register(QUEST_HUD_OVERLAY);

        ClientPlayNetworking.registerGlobalReceiver(HudSetQuestStageS2CPacket.TYPE, (packet, player, sender) -> {
            var client = MinecraftClient.getInstance();
            client.execute(() -> QUEST_HUD_OVERLAY.addQuest(packet.questId(), packet.quest(), packet.tasks()));
        });

        ClientPlayNetworking.registerGlobalReceiver(HudTaskAddS2CPacket.TYPE, (packet, player, sender) -> {
            var client = MinecraftClient.getInstance();
            client.execute(() -> QUEST_HUD_OVERLAY.addTask(packet.questId(), packet.taskId(), packet.task()));
        });

        ClientPlayNetworking.registerGlobalReceiver(HudTaskRemoveS2CPacket.TYPE, (packet, player, sender) -> {
            var client = MinecraftClient.getInstance();
            client.execute(() -> QUEST_HUD_OVERLAY.removeTask(packet.questId(), packet.taskId()));
        });

        ClientPlayNetworking.registerGlobalReceiver(HudTaskCompleteS2CPacket.TYPE, (packet, player, sender) -> {
            var client = MinecraftClient.getInstance();
            client.execute(() -> QUEST_HUD_OVERLAY.completeTask(packet.questId(), packet.taskId(), packet.status()));
        });

        ClientPlayNetworking.registerGlobalReceiver(HudQuestRemoveS2CPacket.TYPE, (packet, player, sender) -> {
            var client = MinecraftClient.getInstance();
            client.execute(() -> QUEST_HUD_OVERLAY.removeQuest(packet.questId()));
        });

        ClientPlayNetworking.registerGlobalReceiver(HudTaskSetProgressS2CPacket.TYPE, (packet, player, sender) -> {
            var client = MinecraftClient.getInstance();
            client.execute(() -> QUEST_HUD_OVERLAY.setTaskProgress(packet.questId(), packet.taskId(), packet.value(), packet.isSuccessProgress()));
        });

        ClientPlayNetworking.registerGlobalReceiver(HudTaskPinS2CPacket.TYPE, (packet, player, sender) -> {
            var client = MinecraftClient.getInstance();
            client.execute(() -> QUEST_HUD_OVERLAY.setTaskPin(packet.questId(), packet.taskId()));
        });

        ClientPlayNetworking.registerGlobalReceiver(QuestBookSyncS2CPacket.TYPE, (packet, player, sender) -> {
            var client = MinecraftClient.getInstance();
            client.execute(() -> ClientQuestManagerContainer.getQuestManager(client)
                    .onListSync(packet.quests()));
        });

        ClientPlayNetworking.registerGlobalReceiver(QuestBookQuestListItemAddedS2CPacket.TYPE, (packet, player, sender) -> {
            var client = MinecraftClient.getInstance();
            client.execute(() -> ClientQuestManagerContainer.getQuestManager(client)
                    .onQuestAdded(packet.quest()));
        });

        ClientPlayNetworking.registerGlobalReceiver(QuestBookQuestRemovedS2CPacket.TYPE, (packet, player, sender) -> {
            var client = MinecraftClient.getInstance();
            client.execute(() -> ClientQuestManagerContainer.getQuestManager(client)
                    .onQuestRemoved(packet.questId()));
        });

        ClientPlayNetworking.registerGlobalReceiver(QuestBookQuestCompletedS2CPacket.TYPE, (packet, player, sender) -> {
            var client = MinecraftClient.getInstance();
            client.execute(() -> ClientQuestManagerContainer.getQuestManager(client)
                    .onQuestCompleted(packet.questId(), packet.completionStatus()));
        });
    }

    private void registerKeybindings() {
        TravelcoreQuestingKeybinds.register();
    }

    private void registerEventListeners() {
        QuestBookOpenEventHandler.register();
    }

}

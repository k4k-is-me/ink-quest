package k4k.travelcorequesting;

import k4k.travelcorequesting.client.TravelcoreQuestingKeybinds;
import k4k.travelcorequesting.infra.networking.*;
import k4k.travelcorequesting.infra.requests.GetQuestDetailsClientRequest;
import k4k.travelcorequesting.client.handlers.QuestBookOpenEventHandler;
import k4k.travelcorequesting.client.huds.QuestHudOverlay;
import k4k.travelcorequesting.client.interfaces.ClientQuestBookManagerContainer;
import k4k.travelcorequesting.client.screens.QuestBookQuestsScreen;
import k4k.travelcorequesting.infra.networking.QuestBookQuestCompletedS2CPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.Nullable;

public class TravelcoreQuestingClient implements ClientModInitializer {
    private static @Nullable QuestHudOverlay QUEST_HUD_OVERLAY = null;

    @Override
    public void onInitializeClient() {
        GetQuestDetailsClientRequest.register();
        GetQuestDetailsClientRequest.INSTANCE.registerClient();

        registerKeybindings();
        registerEventListeners();

        HudRenderCallback.EVENT.register((context, v) -> {
            if (QUEST_HUD_OVERLAY == null) return;
            QUEST_HUD_OVERLAY.onHudRender(context, v);
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                QUEST_HUD_OVERLAY = new QuestHudOverlay());

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                QUEST_HUD_OVERLAY = null);

        ClientPlayNetworking.registerGlobalReceiver(HudSetQuestStageS2CPacket.TYPE, (packet, player, sender) -> {
            ClientQuestBookManagerContainer
                    .getQuestManager(MinecraftClient.getInstance())
                    .invalidateDetail(packet.questId());

            if (QUEST_HUD_OVERLAY == null) return;
            QUEST_HUD_OVERLAY.addQuest(packet.questId(), packet.quest(), packet.tasks());
        });

        ClientPlayNetworking.registerGlobalReceiver(HudTaskAddS2CPacket.TYPE, (packet, player, sender) -> {
            ClientQuestBookManagerContainer
                    .getQuestManager(MinecraftClient.getInstance())
                    .invalidateDetail(packet.questId());

            if (QUEST_HUD_OVERLAY == null) return;
            QUEST_HUD_OVERLAY.addTask(packet.questId(), packet.taskId(), packet.task());
        });

        ClientPlayNetworking.registerGlobalReceiver(HudTaskRemoveS2CPacket.TYPE, (packet, player, sender) -> {
            ClientQuestBookManagerContainer
                    .getQuestManager(MinecraftClient.getInstance())
                    .invalidateDetail(packet.questId());

            if (QUEST_HUD_OVERLAY == null) return;
            QUEST_HUD_OVERLAY.removeTask(packet.questId(), packet.taskId());
        });

        ClientPlayNetworking.registerGlobalReceiver(HudTaskCompleteS2CPacket.TYPE, (packet, player, sender) -> {
            if (QUEST_HUD_OVERLAY == null) return;
            QUEST_HUD_OVERLAY.completeTask(packet.questId(), packet.taskId(), packet.status());
        });

        ClientPlayNetworking.registerGlobalReceiver(HudQuestRemoveS2CPacket.TYPE, (packet, player, sender) -> {
            if (QUEST_HUD_OVERLAY == null) return;
            QUEST_HUD_OVERLAY.removeQuest(packet.questId());
        });

        ClientPlayNetworking.registerGlobalReceiver(HudTaskSetProgressS2CPacket.TYPE, (packet, player, sender) -> {
            ClientQuestBookManagerContainer
                    .getQuestManager(MinecraftClient.getInstance())
                    .invalidateDetail(packet.questId());

            if (QUEST_HUD_OVERLAY == null) return;
            QUEST_HUD_OVERLAY.setTaskProgress(packet.questId(), packet.taskId(), packet.value(), packet.isSuccessProgress());
        });

        ClientPlayNetworking.registerGlobalReceiver(HudTaskPinS2CPacket.TYPE, (packet, player, sender) -> {
            ClientQuestBookManagerContainer
                    .getQuestManager(MinecraftClient.getInstance())
                    .invalidateDetail(packet.questId());

            if (QUEST_HUD_OVERLAY == null) return;
            QUEST_HUD_OVERLAY.setTaskPin(packet.questId(), packet.taskId());
        });

        ClientPlayNetworking.registerGlobalReceiver(QuestBookQuestPinS2CPacket.TYPE, (packet, player, sender) -> {
            ClientQuestBookManagerContainer
                    .getQuestManager(MinecraftClient.getInstance())
                    .onQuestPinChanged(packet.questId(), packet.isPinned());
        });

        ClientPlayNetworking.registerGlobalReceiver(QuestBookSyncS2CPacket.TYPE, (packet, player, sender) -> {
            ClientQuestBookManagerContainer
                    .getQuestManager(MinecraftClient.getInstance())
                    .onListSync(packet.quests());
        });

        ClientPlayNetworking.registerGlobalReceiver(QuestBookQuestListItemAddedS2CPacket.TYPE, (packet, player, sender) -> {
            ClientQuestBookManagerContainer
                    .getQuestManager(MinecraftClient.getInstance())
                    .onQuestAdded(packet.quest());
        });

        ClientPlayNetworking.registerGlobalReceiver(QuestBookQuestRemovedS2CPacket.TYPE, (packet, player, sender) -> {
            ClientQuestBookManagerContainer
                    .getQuestManager(MinecraftClient.getInstance())
                    .onQuestRemoved(packet.questId());
        });

        ClientPlayNetworking.registerGlobalReceiver(QuestBookQuestCompletedS2CPacket.TYPE, (packet, player, sender) -> {
            ClientQuestBookManagerContainer
                    .getQuestManager(MinecraftClient.getInstance())
                    .onQuestCompleted(packet.questId(), packet.completionStatus());
        });

        ClientPlayNetworking.registerGlobalReceiver(QuestBookQuestListItemUpdatedS2CPacket.TYPE, (packet, player, sender) -> {
            ClientQuestBookManagerContainer.getQuestManager(MinecraftClient.getInstance())
                    .onQuestUpdated(packet.quest());
        });

        ClientPlayNetworking.registerGlobalReceiver(QuestBookOpenAtQuestS2CPacket.TYPE, (packet, player, sender) ->
                MinecraftClient.getInstance().setScreen(new QuestBookQuestsScreen(packet.questId())));
    }

    private void registerKeybindings() {
        TravelcoreQuestingKeybinds.register();
    }

    private void registerEventListeners() {
        QuestBookOpenEventHandler.register();
    }

}

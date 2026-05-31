package k4k.inkquest;

import k4k.inkquest.client.TravelcoreQuestingKeybinds;
import k4k.inkquest.client.handlers.QuestBookOpenEventHandler;
import k4k.inkquest.client.huds.QuestHudOverlay;
import k4k.inkquest.client.interfaces.ClientQuestBookManagerContainer;
import k4k.inkquest.client.notifications.NewQuestNotificationManager;
import k4k.inkquest.client.screens.QuestBookQuestsScreen;
import k4k.inkquest.infra.networking.*;
import k4k.inkquest.infra.networking.QuestBookQuestCompletedS2CPacket;
import k4k.inkquest.infra.requests.GetQuestDetailsClientRequest;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.Nullable;

public class TravelcoreQuestingClient implements ClientModInitializer {
    private static @Nullable QuestHudOverlay QUEST_HUD_OVERLAY = null;
    private static @Nullable NewQuestNotificationManager NEW_QUEST_NOTIFICATION_MANAGER = null;

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

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            QUEST_HUD_OVERLAY = new QuestHudOverlay();
            NEW_QUEST_NOTIFICATION_MANAGER = new NewQuestNotificationManager(QUEST_HUD_OVERLAY);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            QUEST_HUD_OVERLAY = null;
            NEW_QUEST_NOTIFICATION_MANAGER = null;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (NEW_QUEST_NOTIFICATION_MANAGER == null) return;
            NEW_QUEST_NOTIFICATION_MANAGER.tick();
        });

        ClientPlayNetworking.registerGlobalReceiver(HudSetQuestStageS2CPacket.TYPE, (packet, player, sender) -> {
            ClientQuestBookManagerContainer
                    .getQuestManager(MinecraftClient.getInstance())
                    .invalidateDetail(packet.questId());

            if (NEW_QUEST_NOTIFICATION_MANAGER != null)
                NEW_QUEST_NOTIFICATION_MANAGER.onQuestPinned(packet.questId());

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
            if (NEW_QUEST_NOTIFICATION_MANAGER != null)
                NEW_QUEST_NOTIFICATION_MANAGER.onQuestEnteredBook(packet.quest().questId());
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

        ClientPlayNetworking.registerGlobalReceiver(QuestBookOpenAtQuestS2CPacket.TYPE, (packet, player, sender) -> {
            var questId = packet.questId();
            if (questId == null && NEW_QUEST_NOTIFICATION_MANAGER != null) {
                questId = NEW_QUEST_NOTIFICATION_MANAGER.consumeActiveTarget();
            }
            if (NEW_QUEST_NOTIFICATION_MANAGER != null) NEW_QUEST_NOTIFICATION_MANAGER.onBookOpened();
            MinecraftClient.getInstance().setScreen(
                    questId == null ? new QuestBookQuestsScreen() : new QuestBookQuestsScreen(questId));
        });
    }

    private void registerKeybindings() {
        TravelcoreQuestingKeybinds.register();
    }

    private void registerEventListeners() {
        QuestBookOpenEventHandler.register();
    }

}

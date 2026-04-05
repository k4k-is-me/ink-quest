package k4k.travelcorequesting;

import k4k.travelcorequesting.infro.command_argument_types.CompletionLevelArgumentType;
import k4k.travelcorequesting.infro.command_argument_types.CompletionStatusArgumentType;
import k4k.travelcorequesting.infro.command_argument_types.QuestGeneralStatusArgumentType;
import k4k.travelcorequesting.infro.command_argument_types.TaskGeneralStatusArgumentType;
import k4k.travelcorequesting.infro.commands.ExecuteCommandExtension;
import k4k.travelcorequesting.infro.loaders.QuestingPersistentStateAdapter;
import k4k.travelcorequesting.infro.networking.HudChangeTaskProgressS2CPacket;
import k4k.travelcorequesting.infro.networking.HudRemoveQuestS2CPacket;
import k4k.travelcorequesting.infro.networking.HudSetQuestStageS2CPacket;
import k4k.travelcorequesting.infro.networking.HudTaskCompleteS2CPacket;
import k4k.travelcorequesting.infro.networking.HudTaskPinS2CPacket;
import k4k.travelcorequesting.infro.utils.QuestDisplays;
import k4k.travelcorequesting.infro.utils.TaskDisplays;
import k4k.travelcorequesting.questing.abstractions.ServerQuestManagerContainer;
import k4k.travelcorequesting.infro.loaders.QuestResourceLoader;
import k4k.travelcorequesting.infro.commands.QuestCommand;
import k4k.travelcorequesting.questing.events.QuestEvents;
import k4k.travelcorequesting.questing.events.QuestProgressEvents;
import k4k.travelcorequesting.questing.models.TaskDisplay;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

// TODO: Реализовать периодическую синхронизацию квестов между сервером и клиентом.
//   Реализовать распределённую синхронизацию.
//   (Можно не реализовывать периодическую синхронизацию квестов, если клиент
//   будет синхронизироваться по событию)

public class TravelcoreQuesting implements ModInitializer {
	public static final String MOD_ID = "travelcorequesting";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final QuestResourceLoader QUEST_RESOURCE_LOADER = new QuestResourceLoader();

	@Override
	public void onInitialize() {
		registerQuestResourceLoader();
		registerCommands();
		registerQuestProgressUpdate();
		registerQuestingPersistence();

		ArgumentTypeRegistry.registerArgumentType(
				Identifier.of(MOD_ID, "completion_status"),
				CompletionStatusArgumentType.class,
				ConstantArgumentSerializer.of(CompletionStatusArgumentType::completionStatus)
		);

		ArgumentTypeRegistry.registerArgumentType(
				Identifier.of(MOD_ID, "completion_level"),
				CompletionLevelArgumentType.class,
				ConstantArgumentSerializer.of(CompletionLevelArgumentType::completionLevel)
		);

		ArgumentTypeRegistry.registerArgumentType(
				Identifier.of(MOD_ID, "quest_general_status"),
				QuestGeneralStatusArgumentType.class,
				ConstantArgumentSerializer.of(QuestGeneralStatusArgumentType::questGeneralStatus)
		);

		ArgumentTypeRegistry.registerArgumentType(
				Identifier.of(MOD_ID, "task_general_status"),
				TaskGeneralStatusArgumentType.class,
				ConstantArgumentSerializer.of(TaskGeneralStatusArgumentType::taskGeneralStatus)
		);

		QuestEvents.QUEST_PINNED.register((questEntry, player) -> {
			var questManager = ServerQuestManagerContainer.getQuestManager(player.getServer());

			var stage = questManager.getActiveStage(questEntry.questId(), player).orElse(null);

			Map<String, TaskDisplay> tasks = stage != null
					? questEntry.quest().getStage(stage).stream()
							.collect(Collectors.toMap(
									Function.identity(),
									taskId -> TaskDisplays.fromTask(Objects.requireNonNull(questEntry.quest().getTask(taskId)))
							))
					: Collections.emptyMap();

			ServerPlayNetworking.send(player, new HudSetQuestStageS2CPacket(
					questEntry.questId(),
					QuestDisplays.fromQuest(questEntry.quest(), stage),
					tasks
			));
		});

		QuestProgressEvents.STAGE_CHANGED.register((questEntry, stage, player) -> {
			if (stage == null) return;

			var questManager = ServerQuestManagerContainer.getQuestManager(player.getServer());
			if (!questManager.isQuestPinned(questEntry.questId(), player)) return;

			ServerPlayNetworking.send(player, new HudSetQuestStageS2CPacket(
					questEntry.questId(),
					QuestDisplays.fromQuest(questEntry.quest(), stage),
					questEntry.quest().getStage(stage).stream()
							.collect(Collectors.toMap(
									Function.identity(),
									taskId -> TaskDisplays.fromTask(Objects.requireNonNull(questEntry.quest().getTask(taskId)))
							))
			));
		});

		QuestProgressEvents.TASK_COMPLETED.register((taskEntry, player, status) -> {
			var questManager = ServerQuestManagerContainer.getQuestManager(player.getServer());
			if (!questManager.isQuestPinned(taskEntry.questId(), player)) return;

			ServerPlayNetworking.send(player, new HudTaskCompleteS2CPacket(
					taskEntry.questId(),
					taskEntry.taskId(),
					status
			));
		});

		QuestEvents.QUEST_PIN_REMOVED.register((questId, player) ->
				ServerPlayNetworking.send(player, new HudRemoveQuestS2CPacket(questId))
		);

		QuestProgressEvents.TASK_SUCCESS_PROGRESS_CHANGED.register((taskEntry, player, newValue) -> {
			var questManager = ServerQuestManagerContainer.getQuestManager(player.getServer());
			if (!questManager.isQuestPinned(taskEntry.questId(), player)) return;

			ServerPlayNetworking.send(player, new HudChangeTaskProgressS2CPacket(taskEntry.questId(), taskEntry.taskId(), newValue, true));
		});

		QuestProgressEvents.TASK_FAILURE_PROGRESS_CHANGED.register((taskEntry, player, newValue) -> {
			var questManager = ServerQuestManagerContainer.getQuestManager(player.getServer());
			if (!questManager.isQuestPinned(taskEntry.questId(), player)) return;

			ServerPlayNetworking.send(player, new HudChangeTaskProgressS2CPacket(taskEntry.questId(), taskEntry.taskId(), newValue, false));
		});

		QuestEvents.TASK_PIN_CHANGED.register((questId, taskId, player) ->
				ServerPlayNetworking.send(player, new HudTaskPinS2CPacket(questId, taskId))
		);
	}

	private void registerQuestResourceLoader() {
		ResourceManagerHelper.get(ResourceType.SERVER_DATA)
				.registerReloadListener(QUEST_RESOURCE_LOADER);

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			var questManager = ServerQuestManagerContainer.getQuestManager(server);
			questManager.loadQuests(QUEST_RESOURCE_LOADER.getLoadedQuests());
		});

		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
			var questManager = ServerQuestManagerContainer.getQuestManager(server);
			questManager.loadQuests(QUEST_RESOURCE_LOADER.getLoadedQuests());
		});
	}

	private void registerCommands() {
		CommandRegistrationCallback.EVENT.register(
				(commandDispatcher, commandRegistryAccess, registrationEnvironment) -> {
					ExecuteCommandExtension.register(commandDispatcher);
					QuestCommand.register(commandDispatcher);
				}
		);
	}

	private void registerQuestProgressUpdate() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			var questManager = ((ServerQuestManagerContainer) server).travelcorequesting$getQuestManager();
			questManager.update(server.getPlayerManager().getPlayerList());
		});
	}

	private void registerQuestingPersistence() {
		ServerLifecycleEvents.SERVER_STARTED.register(QuestingPersistentStateAdapter::register);
	}
}
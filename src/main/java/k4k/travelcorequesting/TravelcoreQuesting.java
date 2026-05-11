package k4k.travelcorequesting;

import k4k.travelcorequesting.infra.gamerules.ModGameRules;
import k4k.travelcorequesting.infra.items.ModItems;
import k4k.travelcorequesting.infra.sounds.ModSounds;
import k4k.travelcorequesting.infra.handlers.QuestBookSyncHandler;
import k4k.travelcorequesting.infra.handlers.QuestHudSyncHandler;
import k4k.travelcorequesting.infra.handlers.QuestLifecycleFunctionExecutor;
import k4k.travelcorequesting.infra.requests.GetQuestDetailsClientRequest;
import k4k.travelcorequesting.infra.command_argument_types.CompletionLevelArgumentType;
import k4k.travelcorequesting.infra.command_argument_types.CompletionStatusArgumentType;
import k4k.travelcorequesting.infra.command_argument_types.QuestGeneralStatusArgumentType;
import k4k.travelcorequesting.infra.command_argument_types.TaskGeneralStatusArgumentType;
import k4k.travelcorequesting.infra.commands.ExecuteCommandExtension;
import k4k.travelcorequesting.infra.loaders.QuestingPersistentStateAdapter;
import k4k.travelcorequesting.infra.networking.QuestBookOpenAtQuestS2CPacket;
import k4k.travelcorequesting.infra.networking.QuestBookOpenRequestC2SPacket;
import k4k.travelcorequesting.infra.networking.QuestBookTaskPinC2SPacket;
import k4k.travelcorequesting.questing.abstractions.ServerQuestManagerContainer;
import k4k.travelcorequesting.infra.loaders.QuestResourceLoader;
import k4k.travelcorequesting.infra.commands.QuestCommand;
import net.fabricmc.api.ModInitializer;
import net.minecraft.text.Text;

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


public class TravelcoreQuesting implements ModInitializer {
	public static final String MOD_ID = "travelcorequesting";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final QuestResourceLoader QUEST_RESOURCE_LOADER = new QuestResourceLoader();

	@Override
	public void onInitialize() {
		ModGameRules.register();
		ModSounds.register();
		ModItems.register();
		registerQuestResourceLoader();
		registerCommands();
		registerQuestProgressUpdate();
		registerQuestingPersistence();
		QuestBookSyncHandler.register();
		QuestHudSyncHandler.register();
		QuestLifecycleFunctionExecutor.register();
		GetQuestDetailsClientRequest.INSTANCE.registerServer();
		ServerPlayNetworking.registerGlobalReceiver(QuestBookOpenRequestC2SPacket.TYPE, (packet, player, sender) -> {
			if (ModGameRules.canPlayerOpenQuestBook(player)) {
				ServerPlayNetworking.send(player, new QuestBookOpenAtQuestS2CPacket(null));
			} else {
				player.sendMessage(
						Text.translatable("travelcorequesting.quest_book.error.no_book_in_inventory"),
						true
				);
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(QuestBookTaskPinC2SPacket.TYPE, (packet, player, sender) -> {
			if (!ModGameRules.canPlayerManuallyPinTask(player)) return;
			var questManager = ServerQuestManagerContainer.getQuestManager(player.getServer());
			try {
				// Повторный клик по уже закреплённой задаче снимает пин с квеста
				if (questManager.isTaskPinned(packet.questId(), packet.taskId(), player)) {
					questManager.pinRemove(packet.questId(), player);
				} else {
					questManager.pinTask(packet.questId(), packet.taskId(), player);
				}
			} catch (IllegalArgumentException ignored) {
				// Квест или задача не существуют — игнорируем
			}
		});

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
	}

	private void registerQuestResourceLoader() {
		ResourceManagerHelper.get(ResourceType.SERVER_DATA)
				.registerReloadListener(QUEST_RESOURCE_LOADER);

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			var questManager = ServerQuestManagerContainer.getQuestManager(server);
			questManager.loadQuests(QUEST_RESOURCE_LOADER.getLoadedQuests());
		});

		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
			if (!success) return;
			var questManager = ServerQuestManagerContainer.getQuestManager(server);
			questManager.loadQuests(QUEST_RESOURCE_LOADER.getLoadedQuests());

			// Ресинк книги квестов для всех онлайн-игроков
			QuestBookSyncHandler.resyncAll(server);

			// Ресинк HUD: переотправляем данные закреплённых квестов с обновлёнными данными
			QuestHudSyncHandler.resyncAll(server);
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

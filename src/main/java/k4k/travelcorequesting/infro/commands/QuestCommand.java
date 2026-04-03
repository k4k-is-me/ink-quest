package k4k.travelcorequesting.infro.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import k4k.travelcorequesting.domain.enums.CompletionStatus;
import k4k.travelcorequesting.infro.abstractions.QuestStatusPredicate;
import k4k.travelcorequesting.infro.enums.QuestGeneralStatus;
import k4k.travelcorequesting.infro.enums.TaskGeneralStatus;
import k4k.travelcorequesting.infro.enums.CompletionLevel;
import k4k.travelcorequesting.infro.suggestion_providers.*;
import k4k.travelcorequesting.infro.utils.QuestTexts;
import k4k.travelcorequesting.questing.abstractions.ServerQuestManagerContainer;
import k4k.travelcorequesting.questing.services.ServerQuestManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static k4k.travelcorequesting.infro.command_argument_types.CompletionLevelArgumentType.*;
import static k4k.travelcorequesting.infro.command_argument_types.CompletionStatusArgumentType.*;
import static net.minecraft.server.command.CommandManager.*;
import static net.minecraft.command.argument.TextArgumentType.*;
import static net.minecraft.command.argument.IdentifierArgumentType.*;
import static net.minecraft.command.argument.EntityArgumentType.*;

public class QuestCommand {
    // Common args
    private static final String ARG_QUEST_ID = "questId";
    private static final String ARG_TASK_ID = "taskId";
    private static final String ARG_PLAYER = "player";
    private static final String ARG_COMPLETION_STATUS = "completionStatus";
    private static final String ARG_COMPLETION_LEVEL = "completionLevel";

    // Quest building args
    private static final String ARG_TITLE = "title";
    private static final String ARG_DESCRIPTION = "description";

    // Result messages
    public static final String ERR_QUEST_MISSING = "quest.command.error.missing";
    public static final String ERR_QUEST_NO_TRACKER = "quest.command.error.no_tracker";
    public static final String ERR_QUEST_TRACKING = "quest.command.error.tracking";
    public static final String ERR_QUEST_EXISTS = "quest.command.error.exists";
    public static final String ERR_QUEST_STATIC = "quest.command.error.static";
    public static final String ERR_QUEST_PINNED = "quest.command.error.pinned";
    public static final String ERR_QUEST_NOT_PINNED = "quest.command.error.not_pinned";
    public static final String ERR_QUEST_COMPLETE = "quest.command.error.complete";
    public static final String ERR_TASK_MISSING = "quest.command.error.task.missing";
    public static final String ERR_TASK_COMPLETE = "quest.command.error.task.complete";
    public static final String ERR_ACTIVE_STAGE_MISSING = "quest.command.error.no_active_stage";

    private static final String MSG_QUEST_NEW = "quest.command.new";
    private static final String MSG_QUEST_MODIFY_TITLE = "quest.command.modify.title";
    private static final String MSG_QUEST_MODIFY_DESCRIPTION = "quest.command.modify.description";
    private static final String MSG_QUEST_GIVE = "quest.command.give";
    private static final String MSG_QUEST_DROP = "quest.command.drop";
    private static final String MSG_QUEST_PIN_ADD_QUEST = "quest.command.pin.add.quest";
    private static final String MSG_QUEST_PIN_REMOVE = "quest.command.pin.remove";
    private static final String MSG_TASK_COMPLETE = "quest.command.complete.task";
    private static final String MSG_STAGE_COMPLETE = "quest.command.complete.stage";
    private static final String MSG_QUEST_COMPLETE = "quest.command.complete.quest";

    public static final List<QuestGeneralStatus> QUEST_STATUSES = Arrays.stream(QuestGeneralStatus.values()).toList();

    public static final List<TaskGeneralStatus> TASK_STATUSES = Arrays.stream(TaskGeneralStatus.values()).toList();

    public static final Function<QuestGeneralStatus, String> QUEST_STATUS_TO_KEYWORD = status -> switch (status) {
        case ACTIVE -> "active";
        case COMPLETE -> "complete";
        case SUCCEEDED -> "succeeded";
        case FAILED -> "failed";
        case SKIPPED -> "skipped";
        case PINNED -> "pinned";
    };

    public static final Function<QuestGeneralStatus, QuestStatusPredicate> QUEST_STATUS_TO_PREDICATE = status -> switch (status) {
        case ACTIVE -> ServerQuestManager::isQuestActive;
        case COMPLETE -> ServerQuestManager::isQuestComplete;
        case SUCCEEDED -> ServerQuestManager::isQuestSucceeded;
        case FAILED -> ServerQuestManager::isQuestFailed;
        case SKIPPED -> ServerQuestManager::isQuestSkipped;
        case PINNED -> ServerQuestManager::isQuestPinned;
    };

    public static final Function<QuestGeneralStatus, String> QUEST_STATUS_TO_TRACKED_MESSAGE = status -> switch (status) {
        case ACTIVE -> "quest.command.list.tracked.active";
        case COMPLETE -> "quest.command.list.tracked.complete";
        case SUCCEEDED -> "quest.command.list.tracked.succeeded";
        case FAILED -> "quest.command.list.tracked.failed";
        case SKIPPED -> "quest.command.list.tracked.skipped";
        case PINNED -> "quest.command.list.tracked.pinned";
    };

    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("quest")
                .requires(source -> source.hasPermissionLevel(2))
                .then(addNewSubCommand())
                .then(addModifySubCommand())
                .then(addGiveSubCommand())
                .then(addDropSubCommand())
                .then(ListSubCommand.getNodeTree())
                .then(TestSubCommand.getNodeTree())
                .then(addPinSubCommand())
                .then(addUnPinSubCommand())
                .then(addCompleteSubCommand())
        );
    }

    /// quest new <questId: Identifier>[ <title: Text>[ <description: Text>]]
    private static ArgumentBuilder<ServerCommandSource, ?> addNewSubCommand() {
        return literal("new")
                .then(argument(ARG_QUEST_ID, identifier())
                        .executes(context -> newQuest(
                                context,
                                getIdentifier(context, ARG_QUEST_ID),
                                null,
                                null
                        ))

                        .then(argument(ARG_TITLE, text())
                                .executes(context -> newQuest(
                                        context,
                                        getIdentifier(context, ARG_QUEST_ID),
                                        getTextArgument(context, ARG_TITLE),
                                        null
                                ))

                                .then(argument(ARG_DESCRIPTION, text())
                                        .executes(context -> newQuest(
                                                context,
                                                getIdentifier(context, ARG_QUEST_ID),
                                                getTextArgument(context, ARG_TITLE),
                                                getTextArgument(context, ARG_DESCRIPTION)
                                        ))
                                )
                        )
                );
    }

    /// quest modify <questId: Identifier> ...
    private static ArgumentBuilder<ServerCommandSource, ?> addModifySubCommand() {
        return literal("modify")
                .then(argument(ARG_QUEST_ID, identifier())
                        .suggests(new DynamicQuestSuggestionProvider())

                        // ... title <title: Text>
                        .then(literal("title")
                                .then(argument(ARG_TITLE, text())
                                        .executes(context -> modifyQuestTitle(
                                                context,
                                                getIdentifier(context, ARG_QUEST_ID)
                                        ))
                                )
                        )

                        // ... description <description: Text>
                        .then(literal("description")
                                .then(argument(ARG_DESCRIPTION, text())
                                        .executes(context -> modifyQuestDescription(
                                                context,
                                                getIdentifier(context, ARG_QUEST_ID)
                                        ))
                                )
                        )

                        // ...
                );
    }

    /// quest give <player: Selector> <questId: Identifier>
    private static ArgumentBuilder<ServerCommandSource, ?> addGiveSubCommand() {
        return literal("give")
                .then(argument(ARG_PLAYER, player())
                        .then(argument(ARG_QUEST_ID, identifier())
                                .suggests(new PlayerQuestSuggestionProvider(ARG_PLAYER, ServerQuestManager::isQuestTracked, false))
                                .executes(context -> giveQuest(
                                        context,
                                        getIdentifier(context, ARG_QUEST_ID),
                                        getPlayer(context, ARG_PLAYER)
                                ))
                        )
                );
    }

    private static ArgumentBuilder<ServerCommandSource, ?> addDropSubCommand() {
        return literal("drop")
                .then(argument(ARG_PLAYER, player())
                        .then(argument(ARG_QUEST_ID, identifier())
                                .suggests(new PlayerQuestSuggestionProvider(ARG_PLAYER, ServerQuestManager::isQuestTracked, true))
                                .executes(context -> dropQuest(
                                        context,
                                        getIdentifier(context, ARG_QUEST_ID),
                                        getPlayer(context, ARG_PLAYER)
                                ))
                        )
                );
    }

    private static ArgumentBuilder<ServerCommandSource, ?> addCompleteSubCommand() {
        return literal("complete")
                .then(argument(ARG_PLAYER, player())
                        .then(argument(ARG_QUEST_ID, identifier())
                                .suggests(new PlayerQuestSuggestionProvider(ARG_PLAYER, ServerQuestManager::isQuestComplete, false))

                                .then(argument(ARG_COMPLETION_STATUS, completionStatus())
                                        .then(argument(ARG_COMPLETION_LEVEL, completionLevel())
                                                .executes(context -> completeQuest(
                                                        context,
                                                        getPlayer(context, ARG_PLAYER),
                                                        getIdentifier(context, ARG_QUEST_ID),
                                                        getCompletionStatus(context, ARG_COMPLETION_STATUS),
                                                        getCompletionLevel(context, ARG_COMPLETION_LEVEL)
                                                ))
                                        )
                                )

                                .then(literal("stage")
                                        .then(argument(ARG_COMPLETION_STATUS, completionStatus())
                                                .then(argument(ARG_COMPLETION_LEVEL, completionLevel())
                                                        .executes(context -> completeActiveStage(
                                                                context,
                                                                getPlayer(context, ARG_PLAYER),
                                                                getIdentifier(context, ARG_QUEST_ID),
                                                                getCompletionStatus(context, ARG_COMPLETION_STATUS),
                                                                getCompletionLevel(context, ARG_COMPLETION_LEVEL)
                                                        ))
                                                )
                                        )
                                )

                                .then(literal("task")
                                        .then(argument(ARG_TASK_ID, word())
                                                .then(argument(ARG_COMPLETION_STATUS, completionStatus())
                                                        .executes(context -> completeTask(
                                                                context,
                                                                getPlayer(context, ARG_PLAYER),
                                                                getIdentifier(context, ARG_QUEST_ID),
                                                                getString(context, ARG_TASK_ID),
                                                                getCompletionStatus(context, ARG_COMPLETION_STATUS)
                                                        ))
                                                )
                                        )
                                )
                        )
                );
    }

    private static ArgumentBuilder<ServerCommandSource, ?> addPinSubCommand() {
        return literal("pin")
                .then(argument(ARG_PLAYER, player())
                        .then(argument(ARG_QUEST_ID, identifier())
                                .suggests(new PlayerQuestSuggestionProvider(ARG_PLAYER, ServerQuestManager::isQuestPinned, false))
                                .executes(context -> pinRequiredTask(
                                        context,
                                        getIdentifier(context, ARG_QUEST_ID),
                                        getPlayer(context, ARG_PLAYER)
                                ))
                        )
                );
    }

    private static ArgumentBuilder<ServerCommandSource, ?> addUnPinSubCommand() {
        return literal("unpin")
                .then(argument(ARG_PLAYER, player())
                        .then(argument(ARG_QUEST_ID, identifier())
                                .suggests(new PlayerQuestSuggestionProvider(ARG_PLAYER, ServerQuestManager::isQuestPinned, true))
                                .executes(context -> pinRemove(
                                        context,
                                        getIdentifier(context, ARG_QUEST_ID),
                                        getPlayer(context, ARG_PLAYER)
                                ))
                        )
                );
    }

    // ---

    public static int newQuest(CommandContext<ServerCommandSource> context, Identifier questId, @Nullable Text title, @Nullable Text description) {
        var questManager = ServerQuestManagerContainer.getQuestManager(context.getSource().getServer());

        if (questManager.isQuestExists(questId)) {
            context.getSource().sendError(Text.translatable(ERR_QUEST_EXISTS));
            return 0;
        }

        questManager.createDynamicQuest(getIdentifier(context, ARG_QUEST_ID));
        questManager.modifyQuest(questId, quest -> {
            if (title != null)
                quest.setTitle(title);

            if (description != null)
                quest.setDescription(description);
        });

        context.getSource().sendFeedback(() -> Text.translatable(MSG_QUEST_NEW), true);
        return 1;
    }

    public static int modifyQuestTitle(CommandContext<ServerCommandSource> context, Identifier questId) {
        return modifyQuestInternal(context, questId, (questManager, source) -> {
            var title = getTextArgument(context, ARG_TITLE);
            questManager.modifyQuest(questId, quest -> quest.setTitle(title));
            source.sendFeedback(() -> Text.translatable(MSG_QUEST_MODIFY_TITLE), true);
            return 1;
        });
    }

    public static int modifyQuestDescription(CommandContext<ServerCommandSource> context, Identifier questId) {
        return modifyQuestInternal(context, questId, (questManager, source) -> {
            var description = getTextArgument(context, ARG_DESCRIPTION);
            questManager.modifyQuest(questId, quest -> quest.setDescription(description));
            source.sendFeedback(() -> Text.translatable(MSG_QUEST_MODIFY_DESCRIPTION), true);
            return 1;
        });
    }

    private static int modifyQuestInternal(CommandContext<ServerCommandSource> context, Identifier questId, QuestModificationStrategy modifier) {
        var questManager = ServerQuestManagerContainer.getQuestManager(context.getSource().getServer());
        var source = context.getSource();

        if (!questManager.isQuestExists(questId)) {
            source.sendError(Text.translatable(ERR_QUEST_MISSING));
            return 0;
        }

        if (questManager.isQuestStatic(questId)) {
            source.sendError(Text.translatable(ERR_QUEST_STATIC));
            return 0;
        }

        return modifier.apply(questManager, source);
    }

    @FunctionalInterface
    private interface QuestModificationStrategy {
        int apply(ServerQuestManager questManager, ServerCommandSource source);
    }

    public static int giveQuest(CommandContext<ServerCommandSource> context, Identifier questId, ServerPlayerEntity player) {
        var questManager = ServerQuestManagerContainer.getQuestManager(context.getSource().getServer());
        var questResolver = questManager.getQuestResolver();
        var source = context.getSource();

        var entry = questResolver.getQuestEntry(questId);

        if (!questManager.isQuestExists(questId) || entry == null) {
            source.sendError(Text.translatable(ERR_QUEST_MISSING));
            return 0;
        }

        if (questManager.hasQuest(player, questId)) {
            source.sendError(Text.translatable(ERR_QUEST_TRACKING, player.getName()));
            return 0;
        }

        questManager.giveQuest(questId, player);
        source.sendFeedback(() -> Text.translatable(MSG_QUEST_GIVE, QuestTexts.getQuestText(entry), player.getName()), false);
        return 1;
    }

    private static int dropQuest(CommandContext<ServerCommandSource> context, Identifier questId, ServerPlayerEntity player) {
        var questManager = ServerQuestManagerContainer.getQuestManager(context.getSource().getServer());
        var questResolver = questManager.getQuestResolver();
        var source = context.getSource();

        var entry = questResolver.getQuestEntry(questId);

        if (!questManager.isQuestExists(questId) || entry == null) {
            source.sendError(Text.translatable(ERR_QUEST_MISSING));
            return 0;
        }

        if (!questManager.hasQuest(player, questId)) {
            source.sendError(Text.translatable(ERR_QUEST_NO_TRACKER, player.getName()));
            return 0;
        }

        questManager.dropQuest(questId, player);
        source.sendFeedback(() -> Text.translatable(MSG_QUEST_DROP), false);

        return 1;
    }

    public static int completeTask(CommandContext<ServerCommandSource> context, ServerPlayerEntity player, Identifier questId, String taskId, CompletionStatus status) {
        var questManager = ServerQuestManagerContainer.getQuestManager(context.getSource().getServer());
        var questResolver = questManager.getQuestResolver();
        var source = context.getSource();

        var entry = questResolver.getQuestEntry(questId);

        if (!questManager.isQuestExists(questId) || entry == null) {
            source.sendError(Text.translatable(ERR_QUEST_MISSING));
            return 0;
        }

        if (!questManager.hasQuest(player, questId)) {
            source.sendError(Text.translatable(ERR_QUEST_NO_TRACKER, player.getName()));
            return 0;
        }

        if (questManager.isQuestComplete(questId, player)) {
            source.sendError(Text.translatable(ERR_QUEST_COMPLETE));
            return 0;
        }

        if (questManager.isTaskComplete(questId, taskId, player)) {
            source.sendError(Text.translatable(ERR_TASK_COMPLETE));
            return 0;
        }

        questManager.completeTask(questId, taskId, player, status);
        source.sendFeedback(() -> Text.translatable(MSG_TASK_COMPLETE, player.getName()), false);
        return 1;
    }

    private static int completeActiveStage(CommandContext<ServerCommandSource> context, ServerPlayerEntity player, Identifier questId, CompletionStatus status, CompletionLevel level) {
        var questManager = ServerQuestManagerContainer.getQuestManager(context.getSource().getServer());
        var questResolver = questManager.getQuestResolver();
        var source = context.getSource();

        var entry = questResolver.getQuestEntry(questId);

        if (!questManager.isQuestExists(questId) || entry == null) {
            source.sendError(Text.translatable(ERR_QUEST_MISSING));
            return 0;
        }

        if (!questManager.hasQuest(player, questId)) {
            source.sendError(Text.translatable(ERR_QUEST_NO_TRACKER, player.getName()));
            return 0;
        }

        if (questManager.isQuestComplete(questId, player)) {
            source.sendError(Text.translatable(ERR_QUEST_COMPLETE));
            return 0;
        }

        var activeStage = questManager.getActiveStage(questId, player)
                .orElse(null);

        if (activeStage == null) {
            source.sendError(Text.translatable(ERR_ACTIVE_STAGE_MISSING));
            return 0;
        }

        var taskIds = level == CompletionLevel.REQUIRED
                ? Collections.singletonList(entry.quest().getRequiredTask(activeStage))
                : entry.quest().getStage(activeStage);

        var completedTasksCount = 0;
        for (var taskId : taskIds) {
            if (questManager.isTaskComplete(questId, taskId, player))
                continue;

            questManager.completeTask(questId, taskId, player, status);
            completedTasksCount++;
        }

        int finalCompletedTasksCount = completedTasksCount;

        source.sendFeedback(() -> Text.translatable(
                MSG_STAGE_COMPLETE,
                finalCompletedTasksCount,
                taskIds.size(),
                player.getName()
        ), false);

        return completedTasksCount;
    }

    private static int completeQuest(CommandContext<ServerCommandSource> context, ServerPlayerEntity player, Identifier questId, CompletionStatus status, CompletionLevel level) {
        var questManager = ServerQuestManagerContainer.getQuestManager(context.getSource().getServer());
        var questResolver = questManager.getQuestResolver();
        var source = context.getSource();

        var entry = questResolver.getQuestEntry(questId);

        if (!questManager.isQuestExists(questId) || entry == null) {
            source.sendError(Text.translatable(ERR_QUEST_MISSING));
            return 0;
        }

        if (!questManager.hasQuest(player, questId)) {
            source.sendError(Text.translatable(ERR_QUEST_NO_TRACKER, player.getName()));
            return 0;
        }

        if (questManager.isQuestComplete(questId, player)) {
            source.sendError(Text.translatable(ERR_QUEST_COMPLETE));
            return 0;
        }

        var completedTasksCount = 0;
        var totalTasksCount = 0;

        for (var stageIndex = 0; stageIndex < entry.quest().getStageCount(); stageIndex++) {
            var taskIds = level == CompletionLevel.REQUIRED
                    ? Collections.singletonList(entry.quest().getRequiredTask(stageIndex))
                    : entry.quest().getStage(stageIndex);
            totalTasksCount += taskIds.size();

            for (var taskId : taskIds) {
                if (questManager.isTaskComplete(questId, taskId, player))
                    continue;

                questManager.completeTask(questId, taskId, player, status);
                completedTasksCount++;
            }
        }

        int finalCompletedTasksCount = completedTasksCount;
        int finalTotalTasksCount = totalTasksCount;

        source.sendFeedback(() -> Text.translatable(
                MSG_QUEST_COMPLETE,
                finalCompletedTasksCount,
                finalTotalTasksCount,
                player.getName()
        ), false);

        return completedTasksCount;
    }

    public static int pinRequiredTask(CommandContext<ServerCommandSource> context, Identifier questId, ServerPlayerEntity player) {
        var questManager = ServerQuestManagerContainer.getQuestManager(context.getSource().getServer());
        var questResolver = questManager.getQuestResolver();
        var source = context.getSource();

        var entry = questResolver.getQuestEntry(questId);

        if (!questManager.isQuestExists(questId) || entry == null) {
            source.sendError(Text.translatable(ERR_QUEST_MISSING));
            return 0;
        }

        if (!questManager.hasQuest(player, questId)) {
            source.sendError(Text.translatable(ERR_QUEST_NO_TRACKER, player.getName()));
            return 0;
        }

        if (questManager.isQuestPinned(entry.questId(), player)) {
            source.sendError(Text.translatable(ERR_QUEST_PINNED));
            return 0;
        }

        questManager.pinRequiredTask(questId, player);
        source.sendFeedback(() -> Text.translatable(MSG_QUEST_PIN_ADD_QUEST, QuestTexts.getQuestText(entry), player.getName()), false);
        return 1;
    }

    private static int pinRemove(CommandContext<ServerCommandSource> context, Identifier questId, ServerPlayerEntity player) {
        var questManager = ServerQuestManagerContainer.getQuestManager(context.getSource().getServer());
        var source = context.getSource();

        if (!questManager.isQuestExists(questId)) {
            source.sendError(Text.translatable(ERR_QUEST_MISSING));
            return 0;
        }

        if (!questManager.hasQuest(player, questId)) {
            source.sendError(Text.translatable(ERR_QUEST_NO_TRACKER, player.getName()));
            return 0;
        }

        if (!questManager.isQuestPinned(questId, player)) {
            source.sendError(Text.translatable(ERR_QUEST_NOT_PINNED));
            return 0;
        }

        questManager.pinRemove(questId, player);
        source.sendFeedback(() -> Text.translatable(MSG_QUEST_PIN_REMOVE), false);
        return 1;
    }

    // Utility components

    /**
     * Для каждого элемента коллекции вызывает функцию и ожидает получить ArgumentBuilder, цепляет полученный ArgumentBuilder
     * к переданной ноде с помощью .then
     */
    public static <T> ArgumentBuilder<ServerCommandSource, ?> addForEach(ArgumentBuilder<ServerCommandSource, ?> node, List<T> collection, Function<T, ArgumentBuilder<ServerCommandSource, ?>> builder) {
        collection.forEach(item -> node.then(builder.apply(item)));
        return node;
    }
}
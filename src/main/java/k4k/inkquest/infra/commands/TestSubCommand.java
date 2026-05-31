package k4k.inkquest.infra.commands;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import k4k.inkquest.infra.abstractions.QuestStatusPredicate;
import k4k.inkquest.infra.abstractions.TaskStatusPredicate;
import k4k.inkquest.infra.enums.QuestGeneralStatus;
import k4k.inkquest.infra.enums.TaskGeneralStatus;
import k4k.inkquest.infra.suggestion_providers.PlayerQuestSuggestionProvider;
import k4k.inkquest.infra.suggestion_providers.QuestTaskSuggestionProvider;
import k4k.inkquest.questing.abstractions.ServerQuestManagerContainer;
import k4k.inkquest.questing.services.ServerQuestManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static com.mojang.brigadier.arguments.StringArgumentType.*;
import static k4k.inkquest.infra.command_argument_types.QuestGeneralStatusArgumentType.*;
import static k4k.inkquest.infra.command_argument_types.TaskGeneralStatusArgumentType.*;
import static net.minecraft.command.argument.EntityArgumentType.getPlayer;
import static net.minecraft.command.argument.EntityArgumentType.player;
import static net.minecraft.command.argument.IdentifierArgumentType.getIdentifier;
import static net.minecraft.command.argument.IdentifierArgumentType.identifier;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class TestSubCommand {
    // Common args
    private static final String ARG_QUEST_ID = "questId";
    private static final String ARG_TASK_ID = "taskId";
    private static final String ARG_PLAYER = "player";
    private static final String ARG_QUEST_STATUS = "status";
    private static final String ARG_TASK_STATUS = "status";

    public static ArgumentBuilder<ServerCommandSource, ?> getNodeTree() {
        return literal("test")
                .then(argument(ARG_PLAYER, player())
                        .then(argument(ARG_QUEST_ID, identifier())
                                .suggests(new PlayerQuestSuggestionProvider(ARG_PLAYER, ServerQuestManager::isQuestTracked, true))

                                .then(argument(ARG_QUEST_STATUS, questGeneralStatus())
                                        .executes(context -> testQuest(
                                                context,
                                                getPlayer(context, ARG_PLAYER),
                                                getIdentifier(context, ARG_QUEST_ID),
                                                getQuestGeneralStatus(context, ARG_QUEST_STATUS)
                                        ))
                                )

                                .then(literal("task")
                                        .then(argument(ARG_TASK_ID, word())
                                                .suggests(new QuestTaskSuggestionProvider(ARG_QUEST_ID))
                                                .then(argument(ARG_TASK_STATUS, taskGeneralStatus())
                                                        .executes(context -> testTask(
                                                                context,
                                                                getPlayer(context, ARG_PLAYER),
                                                                getIdentifier(context, ARG_QUEST_ID),
                                                                getString(context, ARG_TASK_ID),
                                                                getTaskGeneralStatus(context, ARG_TASK_STATUS)
                                                        ))
                                                )
                                        )
                                )
                        )
                );
    }

    static int checkQuest(CommandContext<ServerCommandSource> context, ServerPlayerEntity player, Identifier questId, QuestGeneralStatus status) {
        var questManager = ServerQuestManagerContainer.getQuestManager(context.getSource().getServer());
        var questResolver = questManager.getQuestResolver();
        var entry = questResolver.getQuestEntry(questId);

        if (!questManager.isQuestExists(questId) || entry == null) return 0;
        if (!questManager.isQuestTracked(questId, player)) return 0;

        return getQuestStatusPredicate(status).test(questManager, entry.questId(), player) ? 1 : 0;
    }

    static int checkTask(CommandContext<ServerCommandSource> context, ServerPlayerEntity player, Identifier questId, String taskId, TaskGeneralStatus status) {
        var questManager = ServerQuestManagerContainer.getQuestManager(context.getSource().getServer());
        var questResolver = questManager.getQuestResolver();
        var entry = questResolver.getQuestEntry(questId);

        if (!questManager.isQuestExists(questId) || entry == null) return 0;
        if (!questManager.isQuestTracked(questId, player)) return 0;
        if (!entry.quest().containsTask(taskId)) return 0;

        return getTaskStatusPredicate(status).test(questManager, questId, taskId, player) ? 1 : 0;
    }

    public static int testQuest(CommandContext<ServerCommandSource> context, ServerPlayerEntity player, Identifier questId, QuestGeneralStatus status) {
        var questManager = ServerQuestManagerContainer.getQuestManager(context.getSource().getServer());
        var questResolver = questManager.getQuestResolver();
        var source = context.getSource();

        var entry = questResolver.getQuestEntry(questId);

        if (!questManager.isQuestExists(questId) || entry == null) {
            source.sendError(Text.translatable(QuestCommand.ERR_QUEST_MISSING));
            return 0;
        }

        if (!questManager.isQuestTracked(questId, player)) {
            source.sendError(Text.translatable(QuestCommand.ERR_QUEST_NO_TRACKER, player.getName()));
            return 0;
        }

        var predicate = getQuestStatusPredicate(status);

        if (predicate.test(questManager, entry.questId(), player)) {
            var successTranslationKey = getQuestSuccessTranslationKey(status);
            source.sendFeedback(() -> Text.translatable(successTranslationKey, player.getName()), false);
            return 1;
        } else {
            var failureTranslationKey = getQuestFailureTranslationKey(status);
            source.sendFeedback(() -> Text.translatable(failureTranslationKey, player.getName()), false);
            return 0;
        }
    }

    public static int testTask(CommandContext<ServerCommandSource> context, ServerPlayerEntity player, Identifier questId, String taskId, TaskGeneralStatus status) {
        var questManager = ServerQuestManagerContainer.getQuestManager(context.getSource().getServer());
        var questResolver = questManager.getQuestResolver();
        var source = context.getSource();

        var entry = questResolver.getQuestEntry(questId);

        if (!questManager.isQuestExists(questId) || entry == null) {
            source.sendError(Text.translatable(QuestCommand.ERR_QUEST_MISSING));
            return 0;
        }

        if (!questManager.isQuestTracked(questId, player)) {
            source.sendError(Text.translatable(QuestCommand.ERR_QUEST_NO_TRACKER, player.getName()));
            return 0;
        }

        if (!entry.quest().containsTask(taskId)) {
            source.sendError(Text.translatable(QuestCommand.ERR_TASK_MISSING));
            return 0;
        }

        var predicate = getTaskStatusPredicate(status);

        if (predicate.test(questManager, questId, taskId, player)) {
            var successTranslationKey = getTaskSuccessTranslationKey(status);
            source.sendFeedback(() -> Text.translatable(successTranslationKey, player.getName()), false);
            return 1;
        } else {
            var failureTranslationKey = getTaskFailureTranslationKey(status);
            source.sendFeedback(() -> Text.translatable(failureTranslationKey, player.getName()), false);
            return 0;
        }
    }

    private static QuestStatusPredicate getQuestStatusPredicate(QuestGeneralStatus status) {
        return switch (status) {
            case ACTIVE -> ServerQuestManager::isQuestActive;
            case COMPLETE -> ServerQuestManager::isQuestComplete;
            case SUCCEEDED -> ServerQuestManager::isQuestSucceeded;
            case FAILED -> ServerQuestManager::isQuestFailed;
            case SKIPPED -> ServerQuestManager::isQuestSkipped;
            case PINNED -> ServerQuestManager::isQuestPinned;
        };
    }

    private static String getQuestSuccessTranslationKey(QuestGeneralStatus status) {
        return switch (status) {
            case ACTIVE -> "quest.command.test.quest.active";
            case COMPLETE -> "quest.command.test.quest.complete";
            case SUCCEEDED -> "quest.command.test.quest.succeeded";
            case FAILED -> "quest.command.test.quest.failed";
            case SKIPPED -> "quest.command.test.quest.skipped";
            case PINNED -> "quest.command.test.quest.pinned";
        };
    }

    private static String getQuestFailureTranslationKey(QuestGeneralStatus status) {
        return switch (status) {
            case ACTIVE -> "quest.command.test.quest.not.active";
            case COMPLETE -> "quest.command.test.quest.not.complete";
            case SUCCEEDED -> "quest.command.test.quest.not.succeeded";
            case FAILED -> "quest.command.test.quest.not.failed";
            case SKIPPED -> "quest.command.test.quest.not.skipped";
            case PINNED -> "quest.command.test.quest.not.pinned";
        };
    }

    private static TaskStatusPredicate getTaskStatusPredicate(TaskGeneralStatus status) {
        return switch (status) {
            case ACTIVE -> ServerQuestManager::isTaskActive;
            case COMPLETE -> ServerQuestManager::isTaskComplete;
            case SUCCEEDED -> ServerQuestManager::isTaskSucceeded;
            case FAILED -> ServerQuestManager::isTaskFailed;
            case SKIPPED -> ServerQuestManager::isTaskSkipped;
            case PINNED -> ServerQuestManager::isTaskPinned;
        };
    }

    private static String getTaskSuccessTranslationKey(TaskGeneralStatus status) {
        return switch (status) {
            case ACTIVE -> "quest.command.test.task.active";
            case COMPLETE -> "quest.command.test.task.complete";
            case SUCCEEDED -> "quest.command.test.task.succeeded";
            case FAILED -> "quest.command.test.task.failed";
            case SKIPPED -> "quest.command.test.task.skipped";
            case PINNED -> "quest.command.test.task.pinned";
        };
    }

    private static String getTaskFailureTranslationKey(TaskGeneralStatus status) {
        return switch (status) {
            case ACTIVE -> "quest.command.test.task.not.active";
            case COMPLETE -> "quest.command.test.task.not.complete";
            case SUCCEEDED -> "quest.command.test.task.not.succeeded";
            case FAILED -> "quest.command.test.task.not.failed";
            case SKIPPED -> "quest.command.test.task.not.skipped";
            case PINNED -> "quest.command.test.task.not.pinned";
        };
    }
}

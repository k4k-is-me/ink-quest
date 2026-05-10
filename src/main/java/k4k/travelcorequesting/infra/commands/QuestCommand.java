package k4k.travelcorequesting.infra.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import k4k.travelcorequesting.domain.enums.CompletionStatus;
import k4k.travelcorequesting.domain.enums.QuestPinMode;
import k4k.travelcorequesting.infra.enums.CompletionLevel;
import k4k.travelcorequesting.infra.items.ModItems;
import k4k.travelcorequesting.infra.suggestion_providers.*;
import k4k.travelcorequesting.infra.utils.QuestTexts;
import k4k.travelcorequesting.questing.abstractions.ServerQuestManagerContainer;
import k4k.travelcorequesting.questing.models.QuestEntry;
import k4k.travelcorequesting.questing.services.ServerQuestManager;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static k4k.travelcorequesting.infra.command_argument_types.CompletionLevelArgumentType.*;
import static k4k.travelcorequesting.infra.command_argument_types.CompletionStatusArgumentType.*;
import static net.minecraft.server.command.CommandManager.*;
import static net.minecraft.command.argument.TextArgumentType.*;
import static net.minecraft.command.argument.IdentifierArgumentType.*;
import static net.minecraft.command.argument.EntityArgumentType.*;

public class QuestCommand {
    // Common args
    private static final String ARG_QUEST_ID = "questId";
    private static final String ARG_TASK_ID = "taskId";
    private static final String ARG_PLAYER = "player";
    private static final String ARG_PLAYERS = "players";
    private static final String ARG_COMPLETION_STATUS = "completionStatus";
    private static final String ARG_COMPLETION_LEVEL = "completionLevel";

    // Quest building args
    private static final String ARG_TITLE = "title";
    private static final String ARG_DESCRIPTION = "description";
    private static final String ARG_ICON = "icon";
    private static final String ARG_INDEX = "index";

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
    public static final String ERR_TASK_NOT_ACTIVE = "quest.command.error.task.not_active";
    public static final String ERR_TASK_PINNED = "quest.command.error.task.pinned";
    public static final String ERR_ACTIVE_STAGE_MISSING = "quest.command.error.no_active_stage";

    private static final String ERR_TASK_EXISTS = "quest.command.error.task.exists";
    private static final String ERR_NO_STAGES = "quest.command.error.no_stages";
    private static final String ERR_QUEST_TRACKED = "quest.command.error.tracked";

    private static final String MSG_QUEST_NEW = "quest.command.new";
    private static final String MSG_QUEST_MODIFY_TITLE = "quest.command.modify.title";
    private static final String MSG_QUEST_MODIFY_DESCRIPTION = "quest.command.modify.description";
    private static final String MSG_QUEST_MODIFY_ICON = "quest.command.modify.icon";
    private static final String MSG_QUEST_MODIFY_INDEX = "quest.command.modify.index";
    private static final String MSG_QUEST_MODIFY_BACKGROUND = "quest.command.modify.background";
    private static final String MSG_QUEST_MODIFY_PIN_MODE = "quest.command.modify.pin_mode";
    private static final String MSG_QUEST_MODIFY_TASK_ADD = "quest.command.modify.task.add";
    private static final String MSG_QUEST_MODIFY_TASK_REMOVE = "quest.command.modify.task.remove";
    private static final String MSG_QUEST_GIVE = "quest.command.give";
    private static final String MSG_QUEST_DROP = "quest.command.drop";
    private static final String MSG_QUEST_PIN_ADD_QUEST = "quest.command.pin.add.quest";
    private static final String MSG_QUEST_PIN_REMOVE = "quest.command.pin.remove";
    private static final String MSG_TASK_COMPLETE = "quest.command.complete.task";
    private static final String MSG_STAGE_COMPLETE = "quest.command.complete.stage";
    private static final String MSG_QUEST_COMPLETE = "quest.command.complete.quest";
    private static final String MSG_QUEST_REMOVE = "quest.command.remove";
    private static final String MSG_QUEST_PURGE = "quest.command.purge";

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
                .then(addRemoveSubCommand())
                .then(addPurgeSubCommand())
                .then(QuerySubCommand.getNodeTree())
                .then(addScrollSubCommand())
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

                        // ... icon <icon: Identifier>
                        .then(literal("icon")
                                .then(argument(ARG_ICON, identifier())
                                        .executes(context -> modifyQuestIcon(
                                                context,
                                                getIdentifier(context, ARG_QUEST_ID)
                                        ))
                                )
                        )

                        // ... index <index: int>
                        .then(literal("index")
                                .then(argument(ARG_INDEX, integer())
                                        .executes(context -> modifyQuestIndex(
                                                context,
                                                getIdentifier(context, ARG_QUEST_ID)
                                        ))
                                )
                        )

                        // ... background true|false
                        .then(literal("background")
                                .then(literal("true")
                                        .executes(context -> modifyQuestBackground(
                                                context,
                                                getIdentifier(context, ARG_QUEST_ID),
                                                true
                                        ))
                                )
                                .then(literal("false")
                                        .executes(context -> modifyQuestBackground(
                                                context,
                                                getIdentifier(context, ARG_QUEST_ID),
                                                false
                                        ))
                                )
                        )

                        // ... pin_mode auto|off|force
                        .then(literal("pin_mode")
                                .then(literal("auto")
                                        .executes(context -> modifyQuestPinMode(
                                                context,
                                                getIdentifier(context, ARG_QUEST_ID),
                                                QuestPinMode.AUTO
                                        ))
                                )
                                .then(literal("off")
                                        .executes(context -> modifyQuestPinMode(
                                                context,
                                                getIdentifier(context, ARG_QUEST_ID),
                                                QuestPinMode.OFF
                                        ))
                                )
                                .then(literal("force")
                                        .executes(context -> modifyQuestPinMode(
                                                context,
                                                getIdentifier(context, ARG_QUEST_ID),
                                                QuestPinMode.FORCE
                                        ))
                                )
                        )

                        // ... tasks add required|optional <taskId: word>[ <title: Text>[ <description: Text>]]
                        // ... tasks remove <taskId: word>
                        .then(literal("tasks")
                                .then(literal("add")
                                        .then(literal("required")
                                                .then(argument(ARG_TASK_ID, word())
                                                        .executes(ctx -> modifyQuestAddTask(ctx,
                                                                getIdentifier(ctx, ARG_QUEST_ID), getString(ctx, ARG_TASK_ID), true, null, null))
                                                        .then(argument(ARG_TITLE, text())
                                                                .executes(ctx -> modifyQuestAddTask(ctx,
                                                                        getIdentifier(ctx, ARG_QUEST_ID), getString(ctx, ARG_TASK_ID), true,
                                                                        getTextArgument(ctx, ARG_TITLE), null))
                                                                .then(argument(ARG_DESCRIPTION, text())
                                                                        .executes(ctx -> modifyQuestAddTask(ctx,
                                                                                getIdentifier(ctx, ARG_QUEST_ID), getString(ctx, ARG_TASK_ID), true,
                                                                                getTextArgument(ctx, ARG_TITLE), getTextArgument(ctx, ARG_DESCRIPTION)))
                                                                )
                                                        )
                                                )
                                        )
                                        .then(literal("optional")
                                                .then(argument(ARG_TASK_ID, word())
                                                        .executes(ctx -> modifyQuestAddTask(ctx,
                                                                getIdentifier(ctx, ARG_QUEST_ID), getString(ctx, ARG_TASK_ID), false, null, null))
                                                        .then(argument(ARG_TITLE, text())
                                                                .executes(ctx -> modifyQuestAddTask(ctx,
                                                                        getIdentifier(ctx, ARG_QUEST_ID), getString(ctx, ARG_TASK_ID), false,
                                                                        getTextArgument(ctx, ARG_TITLE), null))
                                                                .then(argument(ARG_DESCRIPTION, text())
                                                                        .executes(ctx -> modifyQuestAddTask(ctx,
                                                                                getIdentifier(ctx, ARG_QUEST_ID), getString(ctx, ARG_TASK_ID), false,
                                                                                getTextArgument(ctx, ARG_TITLE), getTextArgument(ctx, ARG_DESCRIPTION)))
                                                                )
                                                        )
                                                )
                                        )
                                )
                                .then(literal("remove")
                                        .then(argument(ARG_TASK_ID, word())
                                                .suggests(new QuestTaskSuggestionProvider(ARG_QUEST_ID))
                                                .executes(ctx -> modifyQuestRemoveTask(ctx,
                                                        getIdentifier(ctx, ARG_QUEST_ID), getString(ctx, ARG_TASK_ID)))
                                        )
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
                                        getPlayer(context, ARG_PLAYER),
                                        false
                                ))

                                .then(literal("pin")
                                        .executes(context -> giveQuest(
                                                context,
                                                getIdentifier(context, ARG_QUEST_ID),
                                                getPlayer(context, ARG_PLAYER),
                                                true
                                        ))
                                )
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
                                .suggests(new PlayerQuestSuggestionProvider(ARG_PLAYER, ServerQuestManager::isQuestActive, true))

                                .executes(context -> completeQuest(
                                        context,
                                        getPlayer(context, ARG_PLAYER),
                                        getIdentifier(context, ARG_QUEST_ID),
                                        CompletionStatus.SUCCESS,
                                        CompletionLevel.REQUIRED
                                ))

                                .then(argument(ARG_COMPLETION_STATUS, completionStatus())
                                        .executes(context -> completeQuest(
                                                context,
                                                getPlayer(context, ARG_PLAYER),
                                                getIdentifier(context, ARG_QUEST_ID),
                                                getCompletionStatus(context, ARG_COMPLETION_STATUS),
                                                CompletionLevel.REQUIRED
                                        ))

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
                                        .executes(context -> completeActiveStage(
                                                context,
                                                getPlayer(context, ARG_PLAYER),
                                                getIdentifier(context, ARG_QUEST_ID),
                                                CompletionStatus.SUCCESS,
                                                CompletionLevel.REQUIRED
                                        ))

                                        .then(argument(ARG_COMPLETION_STATUS, completionStatus())
                                                .executes(context -> completeActiveStage(
                                                        context,
                                                        getPlayer(context, ARG_PLAYER),
                                                        getIdentifier(context, ARG_QUEST_ID),
                                                        getCompletionStatus(context, ARG_COMPLETION_STATUS),
                                                        CompletionLevel.REQUIRED
                                                ))

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
                                                .suggests(new QuestTaskSuggestionProvider(ARG_QUEST_ID, ARG_PLAYER, (qm, qId, tId, p) -> !qm.isTaskComplete(qId, tId, p), true))

                                                .executes(context -> completeTask(
                                                        context,
                                                        getPlayer(context, ARG_PLAYER),
                                                        getIdentifier(context, ARG_QUEST_ID),
                                                        getString(context, ARG_TASK_ID),
                                                        CompletionStatus.SUCCESS
                                                ))

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
                                .suggests(new PlayerQuestSuggestionProvider(ARG_PLAYER, (qm, qId, p) -> qm.isQuestActive(qId, p) && !qm.isQuestPinned(qId, p), true))
                                .executes(context -> pinRequiredTask(
                                        context,
                                        getIdentifier(context, ARG_QUEST_ID),
                                        getPlayer(context, ARG_PLAYER)
                                ))

                                .then(argument(ARG_TASK_ID, word())
                                        .suggests(new QuestTaskSuggestionProvider(ARG_QUEST_ID, ARG_PLAYER, (qm, qId, tId, p) -> qm.isTaskActive(qId, tId, p) && !qm.isTaskPinned(qId, tId, p), true))
                                        .executes(context -> pinTask(
                                                context,
                                                getIdentifier(context, ARG_QUEST_ID),
                                                getString(context, ARG_TASK_ID),
                                                getPlayer(context, ARG_PLAYER)
                                        ))
                                )
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

    /// quest purge <questId: Identifier>
    private static ArgumentBuilder<ServerCommandSource, ?> addPurgeSubCommand() {
        return literal("purge")
                .then(argument(ARG_QUEST_ID, identifier())
                        .suggests(new RegisteredQuestSuggestionProvider())
                        .executes(context -> dropQuestForAll(
                                context,
                                getIdentifier(context, ARG_QUEST_ID)
                        ))
                );
    }

    private static ArgumentBuilder<ServerCommandSource, ?> addScrollSubCommand() {
        return literal("scroll")
                .then(argument(ARG_QUEST_ID, identifier())
                        .suggests(new RegisteredQuestSuggestionProvider())

                        .executes(context -> giveScrollItem(
                                context,
                                getIdentifier(context, ARG_QUEST_ID),
                                context.getSource().isExecutedByPlayer()
                                        ? Collections.singletonList(context.getSource().getPlayer())
                                        : Collections.emptyList()
                        ))

                        .then(argument(ARG_PLAYERS, players())
                                .executes(context -> giveScrollItem(
                                        context,
                                        getIdentifier(context, ARG_QUEST_ID),
                                        getPlayers(context, ARG_PLAYERS)
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
        return modifyQuestInternal(context, questId, (questManager, source, entry) -> {
            var title = getTextArgument(context, ARG_TITLE);
            questManager.modifyQuest(questId, quest -> quest.setTitle(title));
            source.sendFeedback(() -> Text.translatable(MSG_QUEST_MODIFY_TITLE), true);
            return 1;
        });
    }

    public static int modifyQuestDescription(CommandContext<ServerCommandSource> context, Identifier questId) {
        return modifyQuestInternal(context, questId, (questManager, source, entry) -> {
            var description = getTextArgument(context, ARG_DESCRIPTION);
            questManager.modifyQuest(questId, quest -> quest.setDescription(description));
            source.sendFeedback(() -> Text.translatable(MSG_QUEST_MODIFY_DESCRIPTION), true);
            return 1;
        });
    }

    public static int modifyQuestIcon(CommandContext<ServerCommandSource> context, Identifier questId) {
        return modifyQuestInternal(context, questId, (questManager, source, entry) -> {
            var icon = getIdentifier(context, ARG_ICON);
            questManager.modifyQuest(questId, quest -> quest.setIcon(icon));
            source.sendFeedback(() -> Text.translatable(MSG_QUEST_MODIFY_ICON), true);
            return 1;
        });
    }

    public static int modifyQuestIndex(CommandContext<ServerCommandSource> context, Identifier questId) {
        return modifyQuestInternal(context, questId, (questManager, source, entry) -> {
            var index = getInteger(context, ARG_INDEX);
            questManager.modifyQuest(questId, quest -> quest.setIndex(index));
            source.sendFeedback(() -> Text.translatable(MSG_QUEST_MODIFY_INDEX), true);
            return 1;
        });
    }

    private static int modifyQuestBackground(CommandContext<ServerCommandSource> context, Identifier questId, boolean background) {
        return modifyQuestInternal(context, questId, (questManager, source, entry) -> {
            questManager.modifyQuest(questId, quest -> quest.setBackground(background));
            source.sendFeedback(() -> Text.translatable(MSG_QUEST_MODIFY_BACKGROUND), true);
            return 1;
        });
    }

    private static int modifyQuestPinMode(CommandContext<ServerCommandSource> context, Identifier questId, QuestPinMode pinMode) {
        return modifyQuestInternal(context, questId, (questManager, source, entry) -> {
            questManager.modifyQuest(questId, quest -> quest.setPinMode(pinMode));
            source.sendFeedback(() -> Text.translatable(MSG_QUEST_MODIFY_PIN_MODE), true);
            return 1;
        });
    }

    private static int modifyQuestAddTask(CommandContext<ServerCommandSource> context,
            Identifier questId, String taskId, boolean required,
            @Nullable Text title, @Nullable Text description) {
        return modifyQuestInternal(context, questId, (questManager, source, entry) -> {
            var quest = entry.quest();

            if (quest.containsTask(taskId)) {
                source.sendError(Text.translatable(ERR_TASK_EXISTS));
                return 0;
            }

            if (!required && quest.getStageCount() == 0) {
                source.sendError(Text.translatable(ERR_NO_STAGES));
                return 0;
            }

            questManager.modifyQuest(questId, modifier -> {
                if (required) modifier.addTaskRequired(taskId);
                else modifier.addTaskOptional(taskId);

                if (title != null) modifier.setTaskTitle(taskId, title);
                if (description != null) modifier.setTaskDescription(taskId, description);
            });

            source.sendFeedback(() -> Text.translatable(MSG_QUEST_MODIFY_TASK_ADD), true);
            return 1;
        });
    }

    private static int modifyQuestRemoveTask(CommandContext<ServerCommandSource> context,
            Identifier questId, String taskId) {
        return modifyQuestInternal(context, questId, (questManager, source, entry) -> {
            var quest = entry.quest();

            if (!quest.containsTask(taskId)) {
                source.sendError(Text.translatable(ERR_TASK_MISSING));
                return 0;
            }

            if (questManager.isQuestTrackedByAnyone(questId)) {
                source.sendError(Text.translatable(ERR_QUEST_TRACKED));
                return 0;
            }

            questManager.modifyQuest(questId, modifier -> modifier.removeTask(taskId));

            source.sendFeedback(() -> Text.translatable(MSG_QUEST_MODIFY_TASK_REMOVE), true);
            return 1;
        });
    }

    private static int modifyQuestInternal(CommandContext<ServerCommandSource> context, Identifier questId, QuestModificationStrategy modifier) {
        var questManager = ServerQuestManagerContainer.getQuestManager(context.getSource().getServer());
        var source = context.getSource();

        var entry = questManager.getQuestResolver().getQuestEntry(questId);

        if (!questManager.isQuestExists(questId) || entry == null) {
            source.sendError(Text.translatable(ERR_QUEST_MISSING));
            return 0;
        }

        if (questManager.isQuestStatic(questId)) {
            source.sendError(Text.translatable(ERR_QUEST_STATIC));
            return 0;
        }

        return modifier.apply(questManager, source, entry);
    }

    @FunctionalInterface
    private interface QuestModificationStrategy {
        int apply(ServerQuestManager questManager, ServerCommandSource source, QuestEntry entry);
    }

    public static int giveQuest(CommandContext<ServerCommandSource> context, Identifier questId, ServerPlayerEntity player, boolean pin) {
        var questManager = ServerQuestManagerContainer.getQuestManager(context.getSource().getServer());
        var questResolver = questManager.getQuestResolver();
        var source = context.getSource();

        var entry = questResolver.getQuestEntry(questId);

        if (!questManager.isQuestExists(questId) || entry == null) {
            source.sendError(Text.translatable(ERR_QUEST_MISSING));
            return 0;
        }

        if (questManager.isQuestTracked(questId, player)) {
            source.sendError(Text.translatable(ERR_QUEST_TRACKING, player.getName()));
            return 0;
        }

        questManager.giveQuest(questId, player);

        if (pin) questManager.pinRequiredTask(questId, player);

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

        if (!questManager.isQuestTracked(questId, player)) {
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

        if (!questManager.isQuestTracked(questId, player)) {
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

        if (!questManager.isQuestTracked(questId, player)) {
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

        if (!questManager.isQuestTracked(questId, player)) {
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

        if (!questManager.isQuestTracked(questId, player)) {
            source.sendError(Text.translatable(ERR_QUEST_NO_TRACKER, player.getName()));
            return 0;
        }

        if (questManager.isQuestComplete(questId, player)) {
            source.sendError(Text.translatable(ERR_QUEST_COMPLETE));
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

    private static int pinTask(CommandContext<ServerCommandSource> context, Identifier questId, String taskId, ServerPlayerEntity player) {
        var questManager = ServerQuestManagerContainer.getQuestManager(context.getSource().getServer());
        var questResolver = questManager.getQuestResolver();
        var source = context.getSource();

        var entry = questResolver.getQuestEntry(questId);

        if (!questManager.isQuestExists(questId) || entry == null) {
            source.sendError(Text.translatable(ERR_QUEST_MISSING));
            return 0;
        }

        if (!questManager.isQuestTracked(questId, player)) {
            source.sendError(Text.translatable(ERR_QUEST_NO_TRACKER, player.getName()));
            return 0;
        }

        if (!questManager.isTaskActive(questId, taskId, player)) {
            source.sendError(Text.translatable(ERR_TASK_NOT_ACTIVE));
            return 0;
        }

        if (questManager.isTaskPinned(questId, taskId, player)) {
            source.sendError(Text.translatable(ERR_TASK_PINNED));
            return 0;
        }

        questManager.pinTask(questId, taskId, player);
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

        if (!questManager.isQuestTracked(questId, player)) {
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

    /// quest remove <questId: Identifier>
    private static ArgumentBuilder<ServerCommandSource, ?> addRemoveSubCommand() {
        return literal("remove")
                .then(argument(ARG_QUEST_ID, identifier())
                        .suggests(new DynamicQuestSuggestionProvider())
                        .executes(context -> removeQuest(
                                context,
                                getIdentifier(context, ARG_QUEST_ID)
                        ))
                );
    }

    private static int removeQuest(CommandContext<ServerCommandSource> context, Identifier questId) {
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

        if (questManager.isQuestTrackedByAnyone(questId)) {
            source.sendError(Text.translatable(ERR_QUEST_TRACKED));
            return 0;
        }

        questManager.removeDynamicQuest(questId);
        source.sendFeedback(() -> Text.translatable(MSG_QUEST_REMOVE), true);
        return 1;
    }

    private static int dropQuestForAll(CommandContext<ServerCommandSource> context, Identifier questId) {
        var server = context.getSource().getServer();
        var questManager = ServerQuestManagerContainer.getQuestManager(server);
        var source = context.getSource();

        if (!questManager.isQuestExists(questId)) {
            source.sendError(Text.translatable(ERR_QUEST_MISSING));
            return 0;
        }

        // Online players — proper drop with events and HUD updates
        var onlineDropped = 0;
        for (var player : server.getPlayerManager().getPlayerList()) {
            if (!questManager.isQuestTracked(questId, player)) continue;
            questManager.dropQuest(questId, player);
            onlineDropped++;
        }

        // Remaining trackers (offline players) — silent cleanup
        var offlineDropped = questManager.dropQuestFromAllTrackers(questId);

        int online = onlineDropped;
        source.sendFeedback(
                () -> Text.translatable(MSG_QUEST_PURGE, online + offlineDropped, online, offlineDropped),
                true
        );
        return online + offlineDropped;
    }

    private static int giveScrollItem(CommandContext<ServerCommandSource> context, Identifier questId, Collection<ServerPlayerEntity> players) {
        var questManager = ServerQuestManagerContainer.getQuestManager(context.getSource().getServer());
        var entry = questManager.getQuestResolver().getQuestEntry(questId);

        if (!questManager.isQuestExists(questId) || entry == null) {
            context.getSource().sendError(Text.translatable(ERR_QUEST_MISSING));
            return 0;
        }

        ItemStack stack = new ItemStack(ModItems.QUEST_SCROLL, 1);
        var title = Text.translatable("quest.command.scroll.item.name")
                .append(entry.quest().title());

        NbtCompound nbt = stack.getOrCreateNbt();
        NbtCompound displayNbt = nbt.getCompound("display");
        NbtList loreList = new NbtList();

        loreList.add(NbtString.of(Text.Serializer.toJson(entry.quest().description())));

        displayNbt.putString("Name", Text.Serializer.toJson(title));
        displayNbt.put("Lore", loreList);
        nbt.put("display", displayNbt);
        nbt.put("Quest", NbtString.of(questId.toString()));

        for (var player : players)
            player.getInventory().offerOrDrop(stack);

        return players.size();
    }
}
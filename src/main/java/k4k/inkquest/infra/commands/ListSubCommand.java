package k4k.inkquest.infra.commands;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import k4k.inkquest.infra.abstractions.QuestStatusPredicate;
import k4k.inkquest.infra.enums.QuestGeneralStatus;
import k4k.inkquest.infra.suggestion_providers.RegisteredQuestSuggestionProvider;
import k4k.inkquest.infra.utils.QuestTexts;
import k4k.inkquest.questing.abstractions.ServerQuestManagerContainer;
import k4k.inkquest.questing.enums.TaskType;
import k4k.inkquest.questing.models.QuestEntry;
import k4k.inkquest.questing.services.ServerQuestManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static k4k.inkquest.infra.command_argument_types.QuestGeneralStatusArgumentType.*;
import static net.minecraft.command.argument.EntityArgumentType.*;
import static net.minecraft.command.argument.IdentifierArgumentType.*;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ListSubCommand {
    // Common args
    private static final String ARG_QUEST_ID = "questId";
    private static final String ARG_PLAYER = "player";
    private static final String ARG_QUEST_STATUS = "status";
    private static final String ARG_TASK_STATUS = "status";

    private static final String MSG_QUEST_LIST_ALL = "quest.command.list.all";
    private static final String MSG_QUEST_LIST_STATIC = "quest.command.list.static";
    private static final String MSG_QUEST_LIST_DYNAMIC = "quest.command.list.dynamic";
    private static final String MSG_QUEST_LIST_EMPTY = "quest.command.list.empty";
    private static final String MSG_QUEST_LIST_TASKS_EMPTY = "quest.command.list.tasks.empty";
    private static final String MSG_QUEST_LIST_TASKS_ALL = "quest.command.list.tasks.all";
    private static final String MSG_QUEST_LIST_TASKS_ALL_REQUIRED = "quest.command.list.tasks.all.required";
    private static final String MSG_QUEST_LIST_TASKS_ALL_OPTIONAL = "quest.command.list.tasks.all.optional";
    private static final String MSG_QUEST_LIST_TASKS_ALL_UNUSED = "quest.command.list.tasks.all.unused";
    private static final String MSG_QUEST_LIST_TASKS_STAGE = "quest.command.list.tasks.stage";
    private static final String MSG_QUEST_LIST_TASKS_UNUSED = "quest.command.list.tasks.unused";

    public static ArgumentBuilder<ServerCommandSource, ?> getNodeTree() {
        return literal("list")
                .executes(context -> listSuppliedQuests(
                        context,
                        MSG_QUEST_LIST_ALL,
                        ServerQuestManager::getRegisteredQuests
                ))

                .then(literal("all")
                        .executes(context -> listSuppliedQuests(
                                context,
                                MSG_QUEST_LIST_ALL,
                                ServerQuestManager::getRegisteredQuests
                        ))
                )

                .then(literal("static")
                        .executes(context -> listSuppliedQuests(
                                context,
                                MSG_QUEST_LIST_STATIC,
                                ServerQuestManager::getStaticQuests
                        ))
                )

                .then(literal("dynamic")
                        .executes(context -> listSuppliedQuests(
                                context,
                                MSG_QUEST_LIST_DYNAMIC,
                                ServerQuestManager::getDynamicQuests
                        ))
                )

                .then(literal("trackedby")
                        .then(argument(ARG_PLAYER, player())
                                .executes(context -> listTrackersAll(
                                        context,
                                        getPlayer(context, ARG_PLAYER)
                                ))

                                .then(literal("all")
                                        .executes(context -> listTrackersAll(
                                                context,
                                                getPlayer(context, ARG_PLAYER)
                                        ))
                                )

                                .then(argument(ARG_QUEST_STATUS, questGeneralStatus())
                                        .executes(context -> listTrackers(
                                                context,
                                                getPlayer(context, ARG_PLAYER),
                                                getQuestGeneralStatus(context, ARG_QUEST_STATUS)
                                        ))
                                )
                        )
                )

                .then(literal("tasks")
                        .then(argument(ARG_QUEST_ID, identifier())
                                .suggests(new RegisteredQuestSuggestionProvider())
                                .executes(context -> listTasksAll(
                                        context,
                                        getIdentifier(context, ARG_QUEST_ID)
                                ))

                                .then(literal("all")
                                        .executes(context -> listTasksAll(
                                                context,
                                                getIdentifier(context, ARG_QUEST_ID)
                                        ))
                                )

                                .then(literal("required")
                                        .executes(context -> listTasksRequired(
                                                context,
                                                getIdentifier(context, ARG_QUEST_ID)
                                        ))
                                )

                                .then(literal("optional")
                                        .executes(context -> listTasksOptional(
                                                context,
                                                getIdentifier(context, ARG_QUEST_ID)
                                        ))
                                )

                                .then(literal("unused")
                                        .executes(context -> listTasksUnused(
                                                context,
                                                getIdentifier(context, ARG_QUEST_ID)
                                        ))
                                )

//                                .then(literal("trackedby")
//                                        .then(argument(ARG_PLAYER, player())
//                                                .executes(context -> listTaskTrackersAll(
//                                                        context,
//                                                        getIdentifier(context, ARG_QUEST_ID),
//                                                        getPlayer(context, ARG_PLAYER)
//                                                ))
//
//                                                .then(literal("all")
//                                                        .executes(context -> listTaskTrackersAll(
//                                                                context,
//                                                                getIdentifier(context, ARG_QUEST_ID),
//                                                                getPlayer(context, ARG_PLAYER)
//                                                        ))
//                                                )
//
//                                                .then(argument(ARG_TASK_STATUS, taskGeneralStatus())
//                                                        .executes(context -> listTaskTrackers(
//                                                                context,
//                                                                getIdentifier(context, ARG_QUEST_ID),
//                                                                getPlayer(context, ARG_PLAYER),
//                                                                getTaskGeneralStatus(context, ARG_TASK_STATUS)
//                                                        ))
//                                                )
//                                        )
//                                )
                        )
                );
    }

    public static int listSuppliedQuests(CommandContext<ServerCommandSource> context, String successText, Function<ServerQuestManager, List<QuestEntry>> questsFunction) {
        var questManager = ServerQuestManagerContainer.getQuestManager(context.getSource().getServer());
        var source = context.getSource();

        var quests = questsFunction.apply(questManager).stream()
                .sorted(Comparator.comparing(entry -> entry.quest().title().getString()))
                .map(QuestTexts::getQuestText)
                .toList();

        if (quests.isEmpty()) {
            source.sendFeedback(() -> Text.translatable(MSG_QUEST_LIST_EMPTY), false);
            return 0;
        }

        source.sendFeedback(() -> Text.translatable(successText, quests.size(), Texts.join(quests, Function.identity())), false);
        return quests.size();
    }

    public static int listTrackersAll(CommandContext<ServerCommandSource> context, ServerPlayerEntity player) {
        return listTrackersInternal(
                context,
                player,
                "quest.command.list.tracked.all",
                QuestStatusPredicate.TRUE
        );
    }

    public static int listTrackers(CommandContext<ServerCommandSource> context, ServerPlayerEntity player, QuestGeneralStatus status) {
        var successMessage = switch (status) {
            case ACTIVE -> "quest.command.list.tracked.active";
            case COMPLETE -> "quest.command.list.tracked.complete";
            case SUCCEEDED -> "quest.command.list.tracked.succeeded";
            case FAILED -> "quest.command.list.tracked.failed";
            case PINNED -> "quest.command.list.tracked.pinned";
        };

        QuestStatusPredicate predicate = switch (status) {
            case ACTIVE -> ServerQuestManager::isQuestActive;
            case COMPLETE -> ServerQuestManager::isQuestComplete;
            case SUCCEEDED -> ServerQuestManager::isQuestSucceeded;
            case FAILED -> ServerQuestManager::isQuestFailed;
            case PINNED -> ServerQuestManager::isQuestPinned;
        };

        return listTrackersInternal(
                context,
                player,
                successMessage,
                predicate
        );
    }

    private static int listTrackersInternal(CommandContext<ServerCommandSource> context, ServerPlayerEntity player, String successText, QuestStatusPredicate predicate) {
        var questManager = ServerQuestManagerContainer.getQuestManager(context.getSource().getServer());
        var source = context.getSource();

        var quests = questManager.getTrackedQuests(player).stream()
                .filter(entry -> predicate.test(questManager, entry.questId(), player))
                .sorted(Comparator.comparing(entry -> entry.quest().title().getString()))
                .map(QuestTexts::getQuestText)
                .toList();

        if (quests.isEmpty()) {
            source.sendFeedback(() -> Text.translatable(MSG_QUEST_LIST_EMPTY), false);
            return 0;
        }

        source.sendFeedback(() -> Text.translatable(successText, player.getName(), quests.size(), Texts.join(quests, Function.identity())), false);
        return quests.size();
    }

    public static int listTasksAll(CommandContext<ServerCommandSource> context, Identifier questId) {
        var questManager = ServerQuestManagerContainer.getQuestManager(context.getSource().getServer());
        var questResolver = questManager.getQuestResolver();
        var source = context.getSource();

        var entry = questResolver.getQuestEntry(questId);

        if (!questManager.isQuestExists(questId) || entry == null) {
            source.sendError(Text.translatable(QuestCommand.ERR_QUEST_MISSING));
            return 0;
        }

        var quest = entry.quest();

        if (quest.getTaskCount() == 0) {
            source.sendFeedback(() -> Text.translatable(MSG_QUEST_LIST_TASKS_EMPTY), false);
            return 0;
        }

        var lines = new ArrayList<Text>(quest.getStageCount());

        for (int stageIndex = 0; stageIndex < quest.getStageCount(); stageIndex++) {
            var stage = quest.getStage(stageIndex);
            var tasks = new ArrayList<Text>(stage.size());

            for (var taskId : stage) {
                var taskEntry = questResolver.getTaskEntry(questId, taskId);
                if (taskEntry == null) continue;

                var taskType = questResolver.getTaskType(questId, taskId, stageIndex);

                tasks.add(QuestTexts.getTaskText(taskEntry, taskType));
            }

            lines.add(Text.translatable(MSG_QUEST_LIST_TASKS_STAGE, stageIndex + 1,  Texts.join(tasks, Function.identity())));
        }

        var unusedTasks = new ArrayList<Text>(quest.getUnusedTasks().size());

        for (var taskId : quest.getUnusedTasks()) {
            var taskEntry = questResolver.getTaskEntry(questId, taskId);
            if (taskEntry == null) continue;

            unusedTasks.add(QuestTexts.getTaskText(taskEntry, TaskType.UNUSED));
        }

        if (!unusedTasks.isEmpty())
            lines.add(Text.translatable(MSG_QUEST_LIST_TASKS_UNUSED, Texts.join(unusedTasks, Function.identity())));

        var result = Text.literal("")
                .append(Text.translatable(MSG_QUEST_LIST_TASKS_ALL, quest.getTaskCount()))
                .append("\n")
                .append(Texts.join(lines, Text.literal("\n")));

        source.sendFeedback(() -> result, false);
        return quest.getTaskCount();
    }

    private static int listTasksRequired(CommandContext<ServerCommandSource> context, Identifier questId) {
        var questManager = ServerQuestManagerContainer.getQuestManager(context.getSource().getServer());
        var questResolver = questManager.getQuestResolver();
        var source = context.getSource();

        var entry = questResolver.getQuestEntry(questId);

        if (!questManager.isQuestExists(questId) || entry == null) {
            source.sendError(Text.translatable(QuestCommand.ERR_QUEST_MISSING));
            return 0;
        }

        var quest = entry.quest();
        var tasks = IntStream.range(0, quest.getStageCount())
                .mapToObj(quest::getRequiredTask)
                .flatMap(Optional::stream)
                .map(taskId -> questResolver.getTaskEntry(questId, taskId))
                .filter(Objects::nonNull)
                .map(taskEntry -> QuestTexts.getTaskText(taskEntry, TaskType.REQUIRED))
                .toList();

        if (tasks.isEmpty()) {
            source.sendFeedback(() -> Text.translatable(MSG_QUEST_LIST_TASKS_EMPTY), false);
            return 0;
        }

        source.sendFeedback(() -> Text.translatable(MSG_QUEST_LIST_TASKS_ALL_REQUIRED, tasks.size(), Texts.join(tasks, Function.identity())), false);
        return tasks.size();
    }

    private static int listTasksOptional(CommandContext<ServerCommandSource> context, Identifier questId) {
        var questManager = ServerQuestManagerContainer.getQuestManager(context.getSource().getServer());
        var questResolver = questManager.getQuestResolver();
        var source = context.getSource();

        var entry = questResolver.getQuestEntry(questId);

        if (!questManager.isQuestExists(questId) || entry == null) {
            source.sendError(Text.translatable(QuestCommand.ERR_QUEST_MISSING));
            return 0;
        }

        var quest = entry.quest();

        var requiredTasks = IntStream.range(0, quest.getStageCount())
                .mapToObj(quest::getRequiredTask)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());

        var unusedTasks = quest.getUnusedTasks();

        var tasks = quest.getTasks().stream()
                .filter(taskId -> !requiredTasks.contains(taskId) && !unusedTasks.contains(taskId))
                .map(taskId -> questResolver.getTaskEntry(questId, taskId))
                .filter(Objects::nonNull)
                .map(task -> QuestTexts.getTaskText(task, TaskType.OPTIONAL))
                .toList();

        if (tasks.isEmpty()) {
            source.sendFeedback(() -> Text.translatable(MSG_QUEST_LIST_TASKS_EMPTY), false);
            return 0;
        }

        source.sendFeedback(() -> Text.translatable(MSG_QUEST_LIST_TASKS_ALL_OPTIONAL, tasks.size(), Texts.join(tasks, Function.identity())), false);
        return tasks.size();
    }

    private static int listTasksUnused(CommandContext<ServerCommandSource> context, Identifier questId) {
        var questManager = ServerQuestManagerContainer.getQuestManager(context.getSource().getServer());
        var questResolver = questManager.getQuestResolver();
        var source = context.getSource();

        var entry = questResolver.getQuestEntry(questId);

        if (!questManager.isQuestExists(questId) || entry == null) {
            source.sendError(Text.translatable(QuestCommand.ERR_QUEST_MISSING));
            return 0;
        }

        var quest = entry.quest();
        var tasks = quest.getUnusedTasks().stream()
                .map(taskId -> questResolver.getTaskEntry(questId, taskId))
                .filter(Objects::nonNull)
                .map(taskEntry -> QuestTexts.getTaskText(taskEntry, TaskType.UNUSED))
                .toList();

        if (tasks.isEmpty()) {
            source.sendFeedback(() -> Text.translatable(MSG_QUEST_LIST_TASKS_EMPTY), false);
            return 0;
        }

        source.sendFeedback(() -> Text.translatable(MSG_QUEST_LIST_TASKS_ALL_UNUSED, tasks.size(), Texts.join(tasks, Function.identity())), false);
        return tasks.size();
    }
}

package k4k.inkquest.infra.commands;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import k4k.inkquest.domain.abstractions.Quest;
import k4k.inkquest.infra.suggestion_providers.PlayerQuestSuggestionProvider;
import k4k.inkquest.infra.suggestion_providers.QuestTaskSuggestionProvider;
import k4k.inkquest.questing.abstractions.ServerQuestManagerContainer;
import k4k.inkquest.questing.services.ServerQuestManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.minecraft.command.argument.EntityArgumentType.getPlayer;
import static net.minecraft.command.argument.EntityArgumentType.player;
import static net.minecraft.command.argument.IdentifierArgumentType.getIdentifier;
import static net.minecraft.command.argument.IdentifierArgumentType.identifier;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Подкоманда {@code /quest query} — возвращает числовые значения прогресса квеста.
 *
 * <p>Возвращаемое значение команды (Brigadier return code) соответствует
 * запрошенному числу, что делает подкоманду совместимой с
 * {@code /execute store result score}.
 *
 * <p>Запросы {@code complete} и {@code percent} для завершённых квестов возвращают
 * ошибку и 0, так как прогресс активного этапа после завершения квеста недоступен.
 * Запросы {@code total} работают для квестов в любом состоянии.
 */
public class QuerySubCommand {
    private static final String ARG_PLAYER = "player";
    private static final String ARG_QUEST_ID = "questId";
    private static final String ARG_TASK_ID = "taskId";
    private static final String ARG_STAGE = "stage";

    private static final String ERR_STAGE_INVALID = "quest.command.error.stage.invalid";

    /**
     * Строит дерево узлов подкоманды {@code query}.
     */
    public static ArgumentBuilder<ServerCommandSource, ?> getNodeTree() {
        return literal("query")
                .then(argument(ARG_PLAYER, player())
                        .then(argument(ARG_QUEST_ID, identifier())
                                .suggests(new PlayerQuestSuggestionProvider(ARG_PLAYER, ServerQuestManager::isQuestTracked, true))
                                .then(literal("stages")
                                        .then(literal("complete")
                                                .executes(context -> stagesComplete(
                                                        context,
                                                        getPlayer(context, ARG_PLAYER),
                                                        getIdentifier(context, ARG_QUEST_ID)
                                                ))
                                        )
                                        .then(literal("total")
                                                .executes(context -> stagesTotal(
                                                        context,
                                                        getPlayer(context, ARG_PLAYER),
                                                        getIdentifier(context, ARG_QUEST_ID)
                                                ))
                                        )
                                        .then(literal("percent")
                                                .executes(context -> stagesPercent(
                                                        context,
                                                        getPlayer(context, ARG_PLAYER),
                                                        getIdentifier(context, ARG_QUEST_ID)
                                                ))
                                        )
                                )
                                .then(literal("stage")
                                        .then(literal("active")
                                                .then(literal("tasks")
                                                        .then(literal("complete")
                                                                .executes(context -> stageActiveTasksComplete(
                                                                        context,
                                                                        getPlayer(context, ARG_PLAYER),
                                                                        getIdentifier(context, ARG_QUEST_ID)
                                                                ))
                                                        )
                                                        .then(literal("total")
                                                                .executes(context -> stageActiveTasksTotal(
                                                                        context,
                                                                        getPlayer(context, ARG_PLAYER),
                                                                        getIdentifier(context, ARG_QUEST_ID)
                                                                ))
                                                        )
                                                        .then(literal("percent")
                                                                .executes(context -> stageActiveTasksPercent(
                                                                        context,
                                                                        getPlayer(context, ARG_PLAYER),
                                                                        getIdentifier(context, ARG_QUEST_ID)
                                                                ))
                                                        )
                                                )
                                        )
                                        .then(argument(ARG_STAGE, integer(0))
                                                .then(literal("tasks")
                                                        .then(literal("complete")
                                                                .executes(context -> stageNTasksComplete(
                                                                        context,
                                                                        getPlayer(context, ARG_PLAYER),
                                                                        getIdentifier(context, ARG_QUEST_ID),
                                                                        getInteger(context, ARG_STAGE)
                                                                ))
                                                        )
                                                        .then(literal("total")
                                                                .executes(context -> stageNTasksTotal(
                                                                        context,
                                                                        getPlayer(context, ARG_PLAYER),
                                                                        getIdentifier(context, ARG_QUEST_ID),
                                                                        getInteger(context, ARG_STAGE)
                                                                ))
                                                        )
                                                        .then(literal("percent")
                                                                .executes(context -> stageNTasksPercent(
                                                                        context,
                                                                        getPlayer(context, ARG_PLAYER),
                                                                        getIdentifier(context, ARG_QUEST_ID),
                                                                        getInteger(context, ARG_STAGE)
                                                                ))
                                                        )
                                                )
                                        )
                                )
                                .then(literal("task")
                                        .then(argument(ARG_TASK_ID, word())
                                                .suggests(new QuestTaskSuggestionProvider(ARG_QUEST_ID))
                                                .then(literal("success")
                                                        .then(literal("value")
                                                                .executes(context -> taskConditionValue(
                                                                        context,
                                                                        getPlayer(context, ARG_PLAYER),
                                                                        getIdentifier(context, ARG_QUEST_ID),
                                                                        getString(context, ARG_TASK_ID),
                                                                        true
                                                                ))
                                                        )
                                                        .then(literal("target")
                                                                .executes(context -> taskConditionTarget(
                                                                        context,
                                                                        getPlayer(context, ARG_PLAYER),
                                                                        getIdentifier(context, ARG_QUEST_ID),
                                                                        getString(context, ARG_TASK_ID),
                                                                        true
                                                                ))
                                                        )
                                                        .then(literal("percent")
                                                                .executes(context -> taskConditionPercent(
                                                                        context,
                                                                        getPlayer(context, ARG_PLAYER),
                                                                        getIdentifier(context, ARG_QUEST_ID),
                                                                        getString(context, ARG_TASK_ID),
                                                                        true
                                                                ))
                                                        )
                                                )
                                                .then(literal("failure")
                                                        .then(literal("value")
                                                                .executes(context -> taskConditionValue(
                                                                        context,
                                                                        getPlayer(context, ARG_PLAYER),
                                                                        getIdentifier(context, ARG_QUEST_ID),
                                                                        getString(context, ARG_TASK_ID),
                                                                        false
                                                                ))
                                                        )
                                                        .then(literal("target")
                                                                .executes(context -> taskConditionTarget(
                                                                        context,
                                                                        getPlayer(context, ARG_PLAYER),
                                                                        getIdentifier(context, ARG_QUEST_ID),
                                                                        getString(context, ARG_TASK_ID),
                                                                        false
                                                                ))
                                                        )
                                                        .then(literal("percent")
                                                                .executes(context -> taskConditionPercent(
                                                                        context,
                                                                        getPlayer(context, ARG_PLAYER),
                                                                        getIdentifier(context, ARG_QUEST_ID),
                                                                        getString(context, ARG_TASK_ID),
                                                                        false
                                                                ))
                                                        )
                                                )
                                        )
                                )
                        )
                );
    }

    /**
     * Число завершённых этапов. Ошибка если квест завершён.
     */
    private static int stagesComplete(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player, Identifier questId) {
        var questManager = ServerQuestManagerContainer.getQuestManager(ctx.getSource().getServer());
        var quest = validate(ctx.getSource(), questManager, questId, player);
        if (quest == null) return 0;
        if (rejectIfComplete(ctx.getSource(), questManager, questId, player)) return 0;

        int count = questManager.getStagesComplete(questId, player);
        ctx.getSource().sendFeedback(() -> Text.translatable("quest.command.query.stages.complete",
                player.getName(), count), false);
        return count;
    }

    /**
     * Общее число этапов квеста.
     */
    private static int stagesTotal(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player, Identifier questId) {
        var questManager = ServerQuestManagerContainer.getQuestManager(ctx.getSource().getServer());
        var quest = validate(ctx.getSource(), questManager, questId, player);
        if (quest == null) return 0;

        int total = quest.getStageCount();
        ctx.getSource().sendFeedback(() -> Text.translatable("quest.command.query.stages.total",
                player.getName(), total), false);
        return total;
    }

    /**
     * Процент завершённых этапов (0–100). Ошибка если квест завершён.
     */
    private static int stagesPercent(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player, Identifier questId) {
        var questManager = ServerQuestManagerContainer.getQuestManager(ctx.getSource().getServer());
        var quest = validate(ctx.getSource(), questManager, questId, player);
        if (quest == null) return 0;
        if (rejectIfComplete(ctx.getSource(), questManager, questId, player)) return 0;

        int complete = questManager.getStagesComplete(questId, player);
        int result = percent(complete, quest.getStageCount());
        ctx.getSource().sendFeedback(() -> Text.translatable("quest.command.query.stages.percent",
                player.getName(), result), false);
        return result;
    }

    /**
     * Число завершённых задач в активном этапе.
     */
    private static int stageActiveTasksComplete(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player, Identifier questId) {
        var questManager = ServerQuestManagerContainer.getQuestManager(ctx.getSource().getServer());
        var quest = validate(ctx.getSource(), questManager, questId, player);
        if (quest == null) return 0;

        var activeStage = questManager.getActiveStage(questId, player);
        if (activeStage.isEmpty()) {
            ctx.getSource().sendError(Text.translatable(QuestCommand.ERR_ACTIVE_STAGE_MISSING));
            return 0;
        }

        int complete = questManager.getTasksComplete(questId, player);
        ctx.getSource().sendFeedback(() -> Text.translatable("quest.command.query.stage.tasks.complete",
                player.getName(), complete), false);
        return complete;
    }

    /**
     * Общее число задач в активном этапе.
     */
    private static int stageActiveTasksTotal(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player, Identifier questId) {
        var questManager = ServerQuestManagerContainer.getQuestManager(ctx.getSource().getServer());
        var quest = validate(ctx.getSource(), questManager, questId, player);
        if (quest == null) return 0;

        var activeStage = questManager.getActiveStage(questId, player);
        if (activeStage.isEmpty()) {
            ctx.getSource().sendError(Text.translatable(QuestCommand.ERR_ACTIVE_STAGE_MISSING));
            return 0;
        }

        int total = quest.getStage(activeStage.get()).size();
        ctx.getSource().sendFeedback(() -> Text.translatable("quest.command.query.stage.tasks.total",
                player.getName(), total), false);
        return total;
    }

    /**
     * Процент завершённых задач в активном этапе (0–100).
     */
    private static int stageActiveTasksPercent(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player, Identifier questId) {
        var questManager = ServerQuestManagerContainer.getQuestManager(ctx.getSource().getServer());
        var quest = validate(ctx.getSource(), questManager, questId, player);
        if (quest == null) return 0;

        var activeStage = questManager.getActiveStage(questId, player);
        if (activeStage.isEmpty()) {
            ctx.getSource().sendError(Text.translatable(QuestCommand.ERR_ACTIVE_STAGE_MISSING));
            return 0;
        }

        int complete = questManager.getTasksComplete(questId, player);
        int total = quest.getStage(activeStage.get()).size();
        int result = percent(complete, total);
        ctx.getSource().sendFeedback(() -> Text.translatable("quest.command.query.stage.tasks.percent",
                player.getName(), result), false);
        return result;
    }

    /**
     * Число завершённых задач в этапе N. Ошибка если квест завершён.
     */
    private static int stageNTasksComplete(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player, Identifier questId, int stage) {
        var questManager = ServerQuestManagerContainer.getQuestManager(ctx.getSource().getServer());
        var quest = validate(ctx.getSource(), questManager, questId, player);
        if (quest == null) return 0;
        if (!validateStage(ctx.getSource(), quest, stage)) return 0;
        if (rejectIfComplete(ctx.getSource(), questManager, questId, player)) return 0;

        int complete = questManager.getTasksComplete(questId, stage, player);
        ctx.getSource().sendFeedback(() -> Text.translatable("quest.command.query.stage.tasks.complete",
                player.getName(), complete), false);
        return complete;
    }

    /**
     * Общее число задач в этапе N.
     */
    private static int stageNTasksTotal(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player, Identifier questId, int stage) {
        var questManager = ServerQuestManagerContainer.getQuestManager(ctx.getSource().getServer());
        var quest = validate(ctx.getSource(), questManager, questId, player);
        if (quest == null) return 0;
        if (!validateStage(ctx.getSource(), quest, stage)) return 0;

        int total = quest.getStage(stage).size();
        ctx.getSource().sendFeedback(() -> Text.translatable("quest.command.query.stage.tasks.total",
                player.getName(), total), false);
        return total;
    }

    /**
     * Процент завершённых задач в этапе N (0–100). Ошибка если квест завершён.
     */
    private static int stageNTasksPercent(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player, Identifier questId, int stage) {
        var questManager = ServerQuestManagerContainer.getQuestManager(ctx.getSource().getServer());
        var quest = validate(ctx.getSource(), questManager, questId, player);
        if (quest == null) return 0;
        if (!validateStage(ctx.getSource(), quest, stage)) return 0;
        if (rejectIfComplete(ctx.getSource(), questManager, questId, player)) return 0;

        int complete = Math.max(0, questManager.getTasksComplete(questId, stage, player));
        int total = quest.getStage(stage).size();
        int result = percent(complete, total);
        ctx.getSource().sendFeedback(() -> Text.translatable("quest.command.query.stage.tasks.percent",
                player.getName(), result), false);
        return result;
    }

    /**
     * Текущее значение условия задачи (success или failure).
     */
    private static int taskConditionValue(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player, Identifier questId, String taskId, boolean success) {
        var questManager = ServerQuestManagerContainer.getQuestManager(ctx.getSource().getServer());
        var quest = validate(ctx.getSource(), questManager, questId, player);
        if (quest == null) return 0;
        if (!quest.containsTask(taskId)) {
            ctx.getSource().sendError(Text.translatable(QuestCommand.ERR_TASK_MISSING));
            return 0;
        }

        int value = success
                ? questManager.getTaskSuccessCompletion(questId, taskId, player)
                : questManager.getTaskFailureCompletion(questId, taskId, player);
        var key = success ? "quest.command.query.task.success.value" : "quest.command.query.task.failure.value";
        ctx.getSource().sendFeedback(() -> Text.translatable(key, player.getName(), value), false);
        return value;
    }

    /**
     * Целевое значение условия задачи (success или failure).
     */
    private static int taskConditionTarget(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player, Identifier questId, String taskId, boolean success) {
        var questManager = ServerQuestManagerContainer.getQuestManager(ctx.getSource().getServer());
        var quest = validate(ctx.getSource(), questManager, questId, player);
        if (quest == null) return 0;
        if (!quest.containsTask(taskId)) {
            ctx.getSource().sendError(Text.translatable(QuestCommand.ERR_TASK_MISSING));
            return 0;
        }

        int target = success
                ? questManager.getTaskSuccessTarget(questId, taskId)
                : questManager.getTaskFailureTarget(questId, taskId);
        var key = success ? "quest.command.query.task.success.target" : "quest.command.query.task.failure.target";
        ctx.getSource().sendFeedback(() -> Text.translatable(key, player.getName(), target), false);
        return target;
    }

    /**
     * Процент выполнения условия задачи (success или failure), 0–100.
     */
    private static int taskConditionPercent(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity player, Identifier questId, String taskId, boolean success) {
        var questManager = ServerQuestManagerContainer.getQuestManager(ctx.getSource().getServer());
        var quest = validate(ctx.getSource(), questManager, questId, player);
        if (quest == null) return 0;
        if (!quest.containsTask(taskId)) {
            ctx.getSource().sendError(Text.translatable(QuestCommand.ERR_TASK_MISSING));
            return 0;
        }

        int value = success
                ? questManager.getTaskSuccessCompletion(questId, taskId, player)
                : questManager.getTaskFailureCompletion(questId, taskId, player);
        int target = success
                ? questManager.getTaskSuccessTarget(questId, taskId)
                : questManager.getTaskFailureTarget(questId, taskId);
        int result = percent(value, target);
        var key = success ? "quest.command.query.task.success.percent" : "quest.command.query.task.failure.percent";
        ctx.getSource().sendFeedback(() -> Text.translatable(key, player.getName(), result), false);
        return result;
    }

    // --- helpers ---

    /**
     * Общая валидация: проверяет существование квеста и наличие трекера у игрока.
     *
     * @return Quest если валидация пройдена, null если нет (ошибка уже отправлена)
     */
    @Nullable
    private static Quest validate(ServerCommandSource source, ServerQuestManager questManager, Identifier questId, ServerPlayerEntity player) {
        if (!questManager.isQuestExists(questId)) {
            source.sendError(Text.translatable(QuestCommand.ERR_QUEST_MISSING));
            return null;
        }
        if (!questManager.isQuestTracked(questId, player)) {
            source.sendError(Text.translatable(QuestCommand.ERR_QUEST_NO_TRACKER, player.getName()));
            return null;
        }
        return questManager.getQuestResolver().getQuest(questId);
    }

    /**
     * Отправляет ошибку и возвращает true, если квест завершён.
     * Используется в запросах complete и percent, которые применимы только к активным квестам.
     */
private static boolean rejectIfComplete(ServerCommandSource source, ServerQuestManager questManager, Identifier questId, ServerPlayerEntity player) {
        if (!questManager.isQuestComplete(questId, player)) return false;
        source.sendError(Text.translatable(QuestCommand.ERR_QUEST_COMPLETE));
        return true;
    }

    /**
     * Проверяет, что номер этапа находится в диапазоне [0, stageCount).
     *
     * @return true если этап валиден
     */
    private static boolean validateStage(ServerCommandSource source, Quest quest, int stage) {
        if (stage >= quest.getStageCount()) {
            source.sendError(Text.translatable(ERR_STAGE_INVALID));
            return false;
        }
        return true;
    }

    /**
     * Вычисляет процент (0–100). Возвращает 0 при total == 0.
     */
    private static int percent(int value, int total) {
        if (total == 0) return 0;
        return (value * 100) / total;
    }
}
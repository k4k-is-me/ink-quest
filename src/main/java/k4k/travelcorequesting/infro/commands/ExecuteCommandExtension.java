package k4k.travelcorequesting.infro.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

import static com.mojang.brigadier.arguments.StringArgumentType.*;
import static k4k.travelcorequesting.infro.command_argument_types.QuestGeneralStatusArgumentType.*;
import static k4k.travelcorequesting.infro.command_argument_types.TaskGeneralStatusArgumentType.*;
import static net.minecraft.command.argument.EntityArgumentType.getPlayer;
import static net.minecraft.command.argument.EntityArgumentType.player;
import static net.minecraft.command.argument.IdentifierArgumentType.getIdentifier;
import static net.minecraft.command.argument.IdentifierArgumentType.identifier;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ExecuteCommandExtension {
    // Common args
    private static final String ARG_PLAYER = "player";
    private static final String ARG_QUEST_ID = "questId";
    private static final String ARG_TASK_ID = "taskId";
    private static final String ARG_QUEST_STATUS = "status";
    private static final String ARG_TASK_STATUS = "status";

    private static final String MSG_QUEST_TEST_NOT = "quest.command.test.quest.not";

    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        // Получаем актуальный узел "execute" из дерева команд
        CommandNode<ServerCommandSource> executeNode = dispatcher.getRoot().getChild("execute");
        if (executeNode == null) return; // на всякий случай

        // Получаем узел "if"
        CommandNode<ServerCommandSource> ifNode = executeNode.getChild("if");
        if (ifNode == null) return;

        // Добавляем узел к "if"
        ifNode.addChild(addQuestConditions(executeNode).build());
        ifNode.addChild(addTaskConditions(executeNode).build());
    }

    /**
     * Adds `/execute if quest <player: Selector> <questId: Identifier> active|complete|succeeded|failed|skipped|pinned`
     */
    private static ArgumentBuilder<ServerCommandSource, ?> addQuestConditions(CommandNode<ServerCommandSource> node) {
        return literal("quest")
                .then(argument(ARG_PLAYER, player())
                        .then(argument(ARG_QUEST_ID, identifier())
                                .then(argument(ARG_QUEST_STATUS, questGeneralStatus())
                                        .fork(node, context -> TestSubCommand.testQuest(
                                                context,
                                                getPlayer(context, ARG_PLAYER),
                                                getIdentifier(context, ARG_QUEST_ID),
                                                getQuestGeneralStatus(context, ARG_QUEST_STATUS)
                                        ) != 0 ? Collections.singleton(context.getSource()) : Collections.emptyList())

                                        .executes(context -> TestSubCommand.testQuest(
                                                context,
                                                getPlayer(context, ARG_PLAYER),
                                                getIdentifier(context, ARG_QUEST_ID),
                                                getQuestGeneralStatus(context, ARG_QUEST_STATUS)
                                        ))
                                )
                        )
                );
    }

    /**
     * Adds `/execute if task <player: Selector> <questId: Identifier> <taskId: String> active|complete|succeeded|failed|skipped|pinned`
     */
    private static ArgumentBuilder<ServerCommandSource, ?> addTaskConditions(CommandNode<ServerCommandSource> node) {
        return literal("task")
                .then(argument(ARG_PLAYER, player())
                        .then(argument(ARG_QUEST_ID, identifier())
                                .then(argument(ARG_TASK_ID, word())
                                        .then(argument(ARG_TASK_STATUS, taskGeneralStatus())
                                                .fork(node, context -> TestSubCommand.testTask(
                                                        context,
                                                        getPlayer(context, ARG_PLAYER),
                                                        getIdentifier(context, ARG_QUEST_ID),
                                                        getString(context, ARG_TASK_ID),
                                                        getTaskGeneralStatus(context, ARG_TASK_STATUS)
                                                ) != 0 ? Collections.singleton(context.getSource()) : Collections.emptyList())

                                                .executes(context -> TestSubCommand.testTask(
                                                        context,
                                                        getPlayer(context, ARG_PLAYER),
                                                        getIdentifier(context, ARG_QUEST_ID),
                                                        getString(context, ARG_TASK_ID),
                                                        getTaskGeneralStatus(context, ARG_TASK_STATUS)
                                                ))
                                        )
                                )
                        )
                );
    }
}

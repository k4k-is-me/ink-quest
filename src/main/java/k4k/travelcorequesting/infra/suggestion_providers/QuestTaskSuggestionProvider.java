package k4k.travelcorequesting.infra.suggestion_providers;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import k4k.travelcorequesting.questing.abstractions.ServerQuestManagerContainer;
import k4k.travelcorequesting.questing.services.ServerQuestManager;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static net.minecraft.command.argument.EntityArgumentType.getPlayer;
import static net.minecraft.command.argument.IdentifierArgumentType.getIdentifier;

/**
 * Предлагает task ID для заданного квеста, опционально фильтруя по состоянию игрока.
 */
public class QuestTaskSuggestionProvider implements SuggestionProvider<ServerCommandSource> {
    private final String questIdArgumentName;
    @Nullable private final String playerArgumentName;
    private final Predicate predicate;
    private final boolean positive;

    /**
     * Предлагает все задачи квеста без фильтрации по игроку.
     */
    public QuestTaskSuggestionProvider(String questIdArgumentName) {
        this.questIdArgumentName = questIdArgumentName;
        this.playerArgumentName = null;
        this.predicate = Predicate.TRUE;
        this.positive = true;
    }

    /**
     * Предлагает задачи квеста, отфильтрованные по состоянию игрока.
     */
    public QuestTaskSuggestionProvider(String questIdArgumentName, @SuppressWarnings("NullableProblems") String playerArgumentName, Predicate predicate, boolean positive) {
        this.questIdArgumentName = questIdArgumentName;
        this.playerArgumentName = playerArgumentName;
        this.predicate = predicate;
        this.positive = positive;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        var questManager = ServerQuestManagerContainer.getQuestManager(context.getSource().getServer());
        var questId = getIdentifier(context, questIdArgumentName);
        var quest = questManager.getQuestResolver().getQuest(questId);

        if (quest == null) return builder.buildFuture();

        Stream<String> tasks = quest.getTasks().stream();

        if (playerArgumentName != null) {
            var player = getPlayer(context, playerArgumentName);
            tasks = tasks.filter(taskId -> predicate.test(questManager, questId, taskId, player) == positive);
        }

        return CommandSource.suggestMatching(tasks, builder);
    }

    @FunctionalInterface
    public interface Predicate {
        Predicate TRUE = (questManager, questId, taskId, player) -> true;

        boolean test(ServerQuestManager questManager, Identifier questId, String taskId, ServerPlayerEntity player);
    }
}

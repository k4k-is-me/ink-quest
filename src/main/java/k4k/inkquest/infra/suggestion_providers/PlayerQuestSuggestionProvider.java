package k4k.inkquest.infra.suggestion_providers;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import k4k.inkquest.questing.abstractions.ServerQuestManagerContainer;
import k4k.inkquest.questing.services.ServerQuestManager;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.command.argument.EntityArgumentType.getPlayer;

public class PlayerQuestSuggestionProvider implements SuggestionProvider<ServerCommandSource> {
    private final String playerArgumentName;
    private final Predicate predicate;
    private final boolean positive;

    public PlayerQuestSuggestionProvider(String playerArgumentName, Predicate predicate, boolean positive) {
        this.playerArgumentName = playerArgumentName;
        this.predicate = predicate;
        this.positive = positive;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        var questManager = ServerQuestManagerContainer.getQuestManager(context.getSource().getServer());
        var player = getPlayer(context, playerArgumentName);

        var identifiers = questManager.getQuestResolver().getQuestIds().stream()
                .filter(questId -> predicate.test(questManager, questId, player) == positive);

        return CommandSource.suggestIdentifiers(identifiers, builder);
    }

    @FunctionalInterface
    public interface Predicate {
        boolean test(ServerQuestManager questManager, Identifier questId, ServerPlayerEntity player);
    }
}

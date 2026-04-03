package k4k.travelcorequesting.infro.suggestion_providers;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import k4k.travelcorequesting.questing.abstractions.ServerQuestManagerContainer;
import k4k.travelcorequesting.questing.models.QuestEntry;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;

import java.util.concurrent.CompletableFuture;

/**
 * Используется при выборе квеста для модификации
 */
public class DynamicQuestSuggestionProvider implements SuggestionProvider<ServerCommandSource> {
    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        var questManager = ServerQuestManagerContainer.getQuestManager(context.getSource().getServer());

        var identifiers = questManager.getDynamicQuests().stream()
                .map(QuestEntry::questId);

        return CommandSource.suggestIdentifiers(identifiers, builder);
    }
}

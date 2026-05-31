package k4k.inkquest.infra.checkers;

import k4k.inkquest.domain.models.QuestRequirement;
import k4k.inkquest.questing.abstractions.IQuestRequirementChecker;
import net.minecraft.loot.LootDataType;
import net.minecraft.loot.context.*;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Objects;

/**
 * Реализует проверку условий {@code require} через scoreboard-теги и minecraft predicate.
 */
public class QuestRequirementChecker implements IQuestRequirementChecker {

    @Override
    public boolean check(QuestRequirement require, ServerPlayerEntity player) {
        var commandTags = player.getCommandTags();
        for (var tag : require.tags()) {
            if (!commandTags.contains(tag)) return false;
        }

        if (require.predicate() != null) {
            var server = Objects.requireNonNull(player.getServer());
            var predicate = server.getLootManager().getElement(LootDataType.PREDICATES, require.predicate());
            if (predicate == null) return false;
            var parameterSet = new LootContextParameterSet.Builder(player.getServerWorld())
                    .add(LootContextParameters.THIS_ENTITY, player)
                    .add(LootContextParameters.ORIGIN, player.getPos())
                    .build(LootContextTypes.COMMAND);
            var context = new LootContext.Builder(parameterSet).build(null);
            if (!predicate.test(context)) return false;
        }

        return true;
    }
}

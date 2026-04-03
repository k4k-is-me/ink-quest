package k4k.travelcorequesting.infro.abstractions;

import k4k.travelcorequesting.infro.commands.QuestCommand;
import k4k.travelcorequesting.questing.services.ServerQuestManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

@FunctionalInterface
public interface TaskStatusPredicate {
    TaskStatusPredicate TRUE = (a, b, c, d) -> true;

    boolean test(ServerQuestManager manager, Identifier questId, String taskId, ServerPlayerEntity player);
}

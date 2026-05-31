package k4k.inkquest.infra.abstractions;

import k4k.inkquest.questing.services.ServerQuestManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

@FunctionalInterface
public interface TaskStatusPredicate {
    TaskStatusPredicate TRUE = (a, b, c, d) -> true;

    boolean test(ServerQuestManager manager, Identifier questId, String taskId, ServerPlayerEntity player);
}

package k4k.travelcorequesting.infra.abstractions;

import k4k.travelcorequesting.questing.services.ServerQuestManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

@FunctionalInterface
public interface QuestStatusPredicate {
    QuestStatusPredicate TRUE = (a, b, c) -> true;

    boolean test(ServerQuestManager manager, Identifier questId, ServerPlayerEntity player);
}

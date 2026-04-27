package k4k.travelcorequesting.infra.mixins;

import k4k.travelcorequesting.domain.models.taskConditions.*;
import k4k.travelcorequesting.infra.checkers.QuestRequirementChecker;
import k4k.travelcorequesting.infra.conditions.MinecraftConditionContext;
import k4k.travelcorequesting.questing.services.ServerQuestManager;
import k4k.travelcorequesting.questing.abstractions.ServerQuestManagerContainer;
import k4k.travelcorequesting.questing.services.TaskConditionDispatcher;
import k4k.travelcorequesting.questing.services.taskConditionTesters.*;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = MinecraftServer.class)
public abstract class MinecraftServerMixin implements ServerQuestManagerContainer {
    @Unique
    private ServerQuestManager questManager = travelcorequesting$createQuestManager();

    @Unique
    private static ServerQuestManager travelcorequesting$createQuestManager() {
        var dispatcher = new TaskConditionDispatcher();
        dispatcher
                .register(ScoreCondition.class, new ScoreConditionHandler())
                .register(PredicateCondition.class, new PredicateConditionHandler())
                .register(TasksCondition.class, new TasksConditionHandler())
                .register(AllCondition.class, new AllConditionHandler(dispatcher))
                .register(AnyCondition.class, new AnyConditionHandler(dispatcher))
                .register(NoneCondition.class, new NoneConditionHandler(dispatcher));

        return new ServerQuestManager(
                new QuestRequirementChecker(),
                MinecraftConditionContext::new,
                dispatcher
        );
    }

    @Override
    public ServerQuestManager travelcorequesting$getQuestManager() {
        return this.questManager;
    }
}

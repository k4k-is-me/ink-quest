package k4k.inkquest.infra.mixins;

import k4k.inkquest.domain.models.taskConditions.*;
import k4k.inkquest.infra.checkers.QuestRequirementChecker;
import k4k.inkquest.infra.conditions.MinecraftConditionContext;
import k4k.inkquest.questing.services.ServerQuestManager;
import k4k.inkquest.questing.abstractions.ServerQuestManagerContainer;
import k4k.inkquest.questing.services.TaskConditionDispatcher;
import k4k.inkquest.questing.services.taskConditionTesters.*;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = MinecraftServer.class)
public abstract class MinecraftServerMixin implements ServerQuestManagerContainer {
    @Unique
    private ServerQuestManager questManager = inkquest$createQuestManager();

    @Unique
    private static ServerQuestManager inkquest$createQuestManager() {
        var dispatcher = new TaskConditionDispatcher();
        dispatcher
                .register(ScoreCondition.class, new ScoreConditionHandler())
                .register(GlobalScoreCondition.class, new GlobalScoreConditionHandler())
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
    public ServerQuestManager inkquest$getQuestManager() {
        return this.questManager;
    }
}

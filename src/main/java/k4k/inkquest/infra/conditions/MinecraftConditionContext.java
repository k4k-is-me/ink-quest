package k4k.inkquest.infra.conditions;

import k4k.inkquest.domain.enums.CompletionStatus;
import k4k.inkquest.questing.abstractions.IConditionContext;
import k4k.inkquest.questing.abstractions.QuestResolver;
import k4k.inkquest.questing.services.QuestProgressTracker;
import net.minecraft.loot.LootDataType;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Реализация {@link IConditionContext}, связанная с реальными Minecraft-сервисами.
 *
 * <p>Делегирует вызовы в Scoreboard и LootManager через {@link ServerPlayerEntity}.
 * Для проверки статуса задач использует {@link QuestProgressTracker},
 * а для списка задач активного этапа — {@link QuestResolver}.
 *
 * <p>Конструктор намеренно совпадает с сигнатурой {@link k4k.inkquest.questing.abstractions.IConditionContextFactory#create},
 * что позволяет передавать {@code MinecraftConditionContext::new} как лямбду-фабрику.
 */
public class MinecraftConditionContext implements IConditionContext {
    private final ServerPlayerEntity player;
    private final @Nullable QuestProgressTracker questTracker;
    private final QuestResolver questResolver;
    private final Identifier questId;
    private final String taskId;

    public MinecraftConditionContext(
            ServerPlayerEntity player,
            @Nullable QuestProgressTracker questTracker,
            QuestResolver questResolver,
            Identifier questId,
            String taskId
    ) {
        this.player = player;
        this.questTracker = questTracker;
        this.questResolver = questResolver;
        this.questId = questId;
        this.taskId = taskId;
    }

    @Override
    public void ensureScoreboardObjective(String objectiveName, ScoreboardCriterion criterion) {
        var scoreboard = Objects.requireNonNull(player.getServer()).getScoreboard();
        if (scoreboard.containsObjective(objectiveName)) return;
        scoreboard.addObjective(objectiveName, criterion, Text.literal(objectiveName), criterion.getDefaultRenderType());
    }

    @Override
    public int getScore(String objectiveName, @Nullable String playerOverride) {
        var scoreboard = Objects.requireNonNull(player.getServer()).getScoreboard();
        var objective = scoreboard.getObjective(objectiveName);
        if (objective == null) return 0;
        var name = Objects.requireNonNullElse(playerOverride, player.getEntityName());
        return scoreboard.getPlayerScore(name, objective).getScore();
    }

    @Override
    public void setScore(String objectiveName, @Nullable String playerOverride, int value) {
        var scoreboard = Objects.requireNonNull(player.getServer()).getScoreboard();
        var objective = scoreboard.getObjective(objectiveName);
        if (objective == null) return;
        var name = Objects.requireNonNullElse(playerOverride, player.getEntityName());
        scoreboard.getPlayerScore(name, objective).setScore(value);
    }

    @Override
    public boolean testPredicate(Identifier predicateId) {
        var server = Objects.requireNonNull(player.getServer());
        var predicate = server.getLootManager().getElement(LootDataType.PREDICATES, predicateId);
        if (predicate == null) return false;
        var parameterSet = new LootContextParameterSet.Builder(player.getServerWorld())
                .add(LootContextParameters.THIS_ENTITY, player)
                .add(LootContextParameters.ORIGIN, player.getPos())
                .build(LootContextTypes.COMMAND);
        return predicate.test(new LootContext.Builder(parameterSet).build(null));
    }

    @Override
    public boolean isTaskComplete(String taskId) {
        if (questTracker == null) return false;
        return questTracker.isComplete(taskId);
    }

    @Override
    public boolean isTaskComplete(String taskId, CompletionStatus expected) {
        if (questTracker == null) return false;
        return questTracker.isComplete(taskId, expected);
    }

    @Override
    public List<String> getActiveStageTaskIds() {
        if (questTracker == null) return List.of();
        var activeStage = questTracker.getActiveStage().orElse(null);
        if (activeStage == null) return List.of();
        var quest = questResolver.getQuest(questId);
        if (quest == null) return List.of();
        return quest.getStage(activeStage).stream()
                .filter(tid -> !Objects.equals(tid, this.taskId))
                .toList();
    }
}

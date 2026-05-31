package k4k.inkquest.questing.abstractions;

import k4k.inkquest.questing.services.QuestProgressTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Фабрика {@link IConditionContext}, привязанная к конкретной тройке (игрок, questId, taskId).
 *
 * <p>Реализация живёт в инфраструктурном слое и оборачивает Minecraft-сервисы.
 * Инжектируется в {@link k4k.inkquest.questing.services.ServerQuestManager}.
 */
@FunctionalInterface
public interface IConditionContextFactory {

    /**
     * Создаёт контекст для вычисления условия задачи.
     *
     * @param player        серверная сущность игрока
     * @param questTracker  трекер прогресса квеста; {@code null} если квест не выдан
     * @param questResolver доступ к определениям квестов и задач
     * @param questId       идентификатор квеста
     * @param taskId        идентификатор задачи внутри квеста
     */
    IConditionContext create(
            ServerPlayerEntity player,
            @Nullable QuestProgressTracker questTracker,
            QuestResolver questResolver,
            Identifier questId,
            String taskId
    );
}

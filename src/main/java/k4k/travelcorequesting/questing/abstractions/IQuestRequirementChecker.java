package k4k.travelcorequesting.questing.abstractions;

import k4k.travelcorequesting.domain.models.QuestRequirement;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Проверяет выполнение дополнительных условий разблокировки квеста (поле {@code require}).
 */
public interface IQuestRequirementChecker {

    /**
     * Возвращает {@code true}, если все условия из {@code require} выполнены для данного игрока.
     *
     * @param require условия разблокировки квеста
     * @param player  игрок, для которого выполняется проверка
     */
    boolean check(QuestRequirement require, ServerPlayerEntity player);
}

package k4k.travelcorequesting.infra.networking;

import k4k.travelcorequesting.TravelcoreQuesting;
import k4k.travelcorequesting.domain.enums.CompletionStatus;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * Уведомление клиента о завершении квеста.
 * Отправляется сервером при событии {@code QUEST_COMPLETED}.
 */
public record QuestBookQuestCompletedS2CPacket(
        Identifier questId,
        CompletionStatus completionStatus
) implements FabricPacket {

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

    public static final PacketType<QuestBookQuestCompletedS2CPacket> TYPE = PacketType.create(
            Identifier.of(TravelcoreQuesting.MOD_ID, "quest-brief-completed-s2c"),
            QuestBookQuestCompletedS2CPacket::read
    );

    /** @param buf буфер пакета */
    public static QuestBookQuestCompletedS2CPacket read(PacketByteBuf buf) {
        var questId = buf.readIdentifier();
        var completionStatus = buf.readEnumConstant(CompletionStatus.class);
        return new QuestBookQuestCompletedS2CPacket(questId, completionStatus);
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeIdentifier(this.questId);
        buf.writeEnumConstant(this.completionStatus);
    }
}

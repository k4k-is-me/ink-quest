package k4k.travelcorequesting.infra.networking;

import k4k.travelcorequesting.TravelcoreQuesting;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * Уведомление клиента об удалении квеста из списка.
 * Отправляется сервером при событии {@code QUEST_DROPPED}.
 */
public record QuestBookQuestRemovedS2CPacket(
        Identifier questId
) implements FabricPacket {

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

    public static final PacketType<QuestBookQuestRemovedS2CPacket> TYPE = PacketType.create(
            Identifier.of(TravelcoreQuesting.MOD_ID, "quest-brief-removed-s2c"),
            QuestBookQuestRemovedS2CPacket::read
    );

    /** @param buf буфер пакета */
    public static QuestBookQuestRemovedS2CPacket read(PacketByteBuf buf) {
        return new QuestBookQuestRemovedS2CPacket(buf.readIdentifier());
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeIdentifier(this.questId);
    }
}

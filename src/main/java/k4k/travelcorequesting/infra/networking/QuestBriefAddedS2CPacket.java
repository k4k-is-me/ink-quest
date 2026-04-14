package k4k.travelcorequesting.infra.networking;

import k4k.travelcorequesting.TravelcoreQuesting;
import k4k.travelcorequesting.infra.utils.QuestBriefDatas;
import k4k.travelcorequesting.questing.models.QuestBriefData;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * Уведомление клиента о добавлении нового квеста в список.
 * Отправляется сервером при событии {@code QUEST_GIVEN}.
 */
public record QuestBriefAddedS2CPacket(
        QuestBriefData quest
) implements FabricPacket {

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

    public static final PacketType<QuestBriefAddedS2CPacket> TYPE = PacketType.create(
            Identifier.of(TravelcoreQuesting.MOD_ID, "quest-brief-added-s2c"),
            QuestBriefAddedS2CPacket::read
    );

    /** @param buf буфер пакета */
    public static QuestBriefAddedS2CPacket read(PacketByteBuf buf) {
        return new QuestBriefAddedS2CPacket(QuestBriefDatas.readFromPacketByteBuf(buf));
    }

    @Override
    public void write(PacketByteBuf buf) {
        QuestBriefDatas.writeToPacketByteBuf(this.quest, buf);
    }
}

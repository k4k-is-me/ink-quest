package k4k.travelcorequesting.infra.networking;

import k4k.travelcorequesting.TravelcoreQuesting;
import k4k.travelcorequesting.infra.utils.QuestBriefDatas;
import k4k.travelcorequesting.questing.models.QuestBriefData;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Полная синхронизация списка квестов игрока с клиентом.
 * Отправляется сервером при входе игрока в мир.
 */
public record QuestListSyncS2CPacket(
        List<QuestBriefData> quests
) implements FabricPacket {

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

    public static final PacketType<QuestListSyncS2CPacket> TYPE = PacketType.create(
            Identifier.of(TravelcoreQuesting.MOD_ID, "quest-list-sync-s2c"),
            QuestListSyncS2CPacket::read
    );

    /** @param buf буфер пакета */
    public static QuestListSyncS2CPacket read(PacketByteBuf buf) {
        var count = buf.readInt();
        var quests = new ArrayList<QuestBriefData>(count);
        for (var i = 0; i < count; i++) {
            quests.add(QuestBriefDatas.readFromPacketByteBuf(buf));
        }
        return new QuestListSyncS2CPacket(quests);
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeInt(this.quests.size());
        for (var quest : this.quests) {
            QuestBriefDatas.writeToPacketByteBuf(quest, buf);
        }
    }
}

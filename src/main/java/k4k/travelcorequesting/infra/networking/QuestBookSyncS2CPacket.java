package k4k.travelcorequesting.infra.networking;

import k4k.travelcorequesting.TravelcoreQuesting;
import k4k.travelcorequesting.infra.utils.QuestBookQuestListItems;
import k4k.travelcorequesting.questing.models.QuestBookQuestListItem;
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
public record QuestBookSyncS2CPacket(
        List<QuestBookQuestListItem> quests
) implements FabricPacket {

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

    public static final PacketType<QuestBookSyncS2CPacket> TYPE = PacketType.create(
            Identifier.of(TravelcoreQuesting.MOD_ID, "quest-list-sync-s2c"),
            QuestBookSyncS2CPacket::read
    );

    /** @param buf буфер пакета */
    public static QuestBookSyncS2CPacket read(PacketByteBuf buf) {
        var count = buf.readInt();
        var quests = new ArrayList<QuestBookQuestListItem>(count);
        for (var i = 0; i < count; i++) {
            quests.add(QuestBookQuestListItems.readFromPacketByteBuf(buf));
        }
        return new QuestBookSyncS2CPacket(quests);
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeInt(this.quests.size());
        for (var quest : this.quests) {
            QuestBookQuestListItems.writeToPacketByteBuf(quest, buf);
        }
    }
}

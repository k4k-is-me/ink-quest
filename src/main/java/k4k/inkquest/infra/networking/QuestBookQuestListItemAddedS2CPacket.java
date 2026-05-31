package k4k.inkquest.infra.networking;

import k4k.inkquest.TravelcoreQuesting;
import k4k.inkquest.infra.utils.QuestBookQuestListItems;
import k4k.inkquest.questing.models.QuestBookQuestListItem;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * Уведомление клиента о добавлении нового квеста в список.
 * Отправляется сервером при событии {@code QUEST_GIVEN}.
 */
public record QuestBookQuestListItemAddedS2CPacket(
        QuestBookQuestListItem quest
) implements FabricPacket {

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

    public static final PacketType<QuestBookQuestListItemAddedS2CPacket> TYPE = PacketType.create(
            Identifier.of(TravelcoreQuesting.MOD_ID, "quest-brief-added-s2c"),
            QuestBookQuestListItemAddedS2CPacket::read
    );

    /** @param buf буфер пакета */
    public static QuestBookQuestListItemAddedS2CPacket read(PacketByteBuf buf) {
        return new QuestBookQuestListItemAddedS2CPacket(QuestBookQuestListItems.readFromPacketByteBuf(buf));
    }

    @Override
    public void write(PacketByteBuf buf) {
        QuestBookQuestListItems.writeToPacketByteBuf(this.quest, buf);
    }
}

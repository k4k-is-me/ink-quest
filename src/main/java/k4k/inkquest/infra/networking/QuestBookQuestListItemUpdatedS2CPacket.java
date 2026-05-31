package k4k.inkquest.infra.networking;

import k4k.inkquest.TravelcoreQuesting;
import k4k.inkquest.infra.utils.QuestBookQuestListItems;
import k4k.inkquest.questing.models.QuestBookQuestListItem;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * Уведомление клиента об обновлении метаданных существующего квеста в списке.
 * Отправляется сервером при событии {@code QUEST_MODIFIED} для каждого игрока, которому квест выдан.
 */
public record QuestBookQuestListItemUpdatedS2CPacket(
        QuestBookQuestListItem quest
) implements FabricPacket {

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

    public static final PacketType<QuestBookQuestListItemUpdatedS2CPacket> TYPE = PacketType.create(
            Identifier.of(TravelcoreQuesting.MOD_ID, "quest-brief-updated-s2c"),
            QuestBookQuestListItemUpdatedS2CPacket::read
    );

    /** @param buf буфер пакета */
    public static QuestBookQuestListItemUpdatedS2CPacket read(PacketByteBuf buf) {
        return new QuestBookQuestListItemUpdatedS2CPacket(QuestBookQuestListItems.readFromPacketByteBuf(buf));
    }

    @Override
    public void write(PacketByteBuf buf) {
        QuestBookQuestListItems.writeToPacketByteBuf(this.quest, buf);
    }
}

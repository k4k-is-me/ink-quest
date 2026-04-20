package k4k.travelcorequesting.infra.networking;

import k4k.travelcorequesting.TravelcoreQuesting;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * S2C пакет изменения статуса закрепления квеста в книге квестов.
 * Отправляется при {@code QUEST_PINNED} и {@code QUEST_UNPINNED}.
 *
 * @param questId  идентификатор квеста
 * @param isPinned {@code true} — квест закреплён, {@code false} — снят
 */
public record QuestBookQuestPinS2CPacket(
        Identifier questId,
        boolean isPinned
) implements FabricPacket {

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

    public static final PacketType<QuestBookQuestPinS2CPacket> TYPE = PacketType.create(
            Identifier.of(TravelcoreQuesting.MOD_ID, "quest-book-quest-pin-s2c"),
            QuestBookQuestPinS2CPacket::read
    );

    /** Читает пакет из буфера. */
    public static QuestBookQuestPinS2CPacket read(PacketByteBuf buf) {
        var questId = buf.readIdentifier();
        var isPinned = buf.readBoolean();
        return new QuestBookQuestPinS2CPacket(questId, isPinned);
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeIdentifier(questId);
        buf.writeBoolean(isPinned);
    }
}
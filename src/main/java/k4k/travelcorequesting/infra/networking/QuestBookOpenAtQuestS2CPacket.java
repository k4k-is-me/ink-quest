package k4k.travelcorequesting.infra.networking;

import k4k.travelcorequesting.TravelcoreQuesting;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * S2C пакет, инструктирующий клиента открыть книгу квестов на конкретном квесте.
 * Отправляется сервером при успешном использовании {@code QuestScrollItem}.
 *
 * @param questId идентификатор квеста для предварительного выбора
 */
public record QuestBookOpenAtQuestS2CPacket(
        Identifier questId
) implements FabricPacket {

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

    public static final PacketType<QuestBookOpenAtQuestS2CPacket> TYPE = PacketType.create(
            Identifier.of(TravelcoreQuesting.MOD_ID, "quest-book-open-at-quest-s2c"),
            QuestBookOpenAtQuestS2CPacket::read
    );

    /** Читает пакет из буфера. */
    public static QuestBookOpenAtQuestS2CPacket read(PacketByteBuf buf) {
        return new QuestBookOpenAtQuestS2CPacket(buf.readIdentifier());
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeIdentifier(questId);
    }
}

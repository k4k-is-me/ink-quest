package k4k.inkquest.infra.networking;

import k4k.inkquest.TravelcoreQuesting;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * S2C пакет, инструктирующий клиента открыть книгу квестов.
 * Отправляется сервером при успешном использовании {@code QuestScrollItem} или при запросе
 * клавишей J (если gamerule и инвентарь позволяют).
 *
 * @param questId идентификатор квеста для предварительного выбора, или {@code null} — открыть без выбора
 */
public record QuestBookOpenAtQuestS2CPacket(
        @Nullable Identifier questId
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
        return new QuestBookOpenAtQuestS2CPacket(buf.readNullable(PacketByteBuf::readIdentifier));
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeNullable(questId, PacketByteBuf::writeIdentifier);
    }
}

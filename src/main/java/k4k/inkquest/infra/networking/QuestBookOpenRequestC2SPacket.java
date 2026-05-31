package k4k.inkquest.infra.networking;

import k4k.inkquest.TravelcoreQuesting;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;

/** C2S пакет запроса на открытие квестовой книги; сервер авторитетно проверяет gamerule и инвентарь. */
public record QuestBookOpenRequestC2SPacket() implements FabricPacket {

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

    public static final PacketType<QuestBookOpenRequestC2SPacket> TYPE = PacketType.create(
            net.minecraft.util.Identifier.of(TravelcoreQuesting.MOD_ID, "quest-book-open-request-c2s"),
            QuestBookOpenRequestC2SPacket::read
    );

    /** Читает пакет из буфера. */
    public static QuestBookOpenRequestC2SPacket read(PacketByteBuf buf) {
        return new QuestBookOpenRequestC2SPacket();
    }

    @Override
    public void write(PacketByteBuf buf) {}
}

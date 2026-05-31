package k4k.inkquest.infra.networking;

import k4k.inkquest.TravelcoreQuesting;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/** C2S пакет запроса на закрепление задачи квеста из квестовой книги. */
public record QuestBookTaskPinC2SPacket(Identifier questId, String taskId) implements FabricPacket {

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

    public static final PacketType<QuestBookTaskPinC2SPacket> TYPE = PacketType.create(
            Identifier.of(TravelcoreQuesting.MOD_ID, "pin-task-c2s"),
            QuestBookTaskPinC2SPacket::read
    );

    /** Читает пакет из буфера. */
    public static QuestBookTaskPinC2SPacket read(PacketByteBuf buf) {
        return new QuestBookTaskPinC2SPacket(buf.readIdentifier(), buf.readString());
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeIdentifier(questId);
        buf.writeString(taskId);
    }
}

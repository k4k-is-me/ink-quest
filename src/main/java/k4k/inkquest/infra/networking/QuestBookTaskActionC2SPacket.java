package k4k.inkquest.infra.networking;

import k4k.inkquest.TravelcoreQuesting;
import k4k.inkquest.domain.enums.TaskButton;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/** C2S пакет нажатия кнопки ручного завершения задачи из квестовой книги. */
public record QuestBookTaskActionC2SPacket(Identifier questId, String taskId, TaskButton action)
        implements FabricPacket {

    public static final PacketType<QuestBookTaskActionC2SPacket> TYPE = PacketType.create(
            Identifier.of(TravelcoreQuesting.MOD_ID, "task-action-c2s"),
            QuestBookTaskActionC2SPacket::read
    );

    /** Читает пакет из буфера. */
    public static QuestBookTaskActionC2SPacket read(PacketByteBuf buf) {
        return new QuestBookTaskActionC2SPacket(
                buf.readIdentifier(),
                buf.readString(),
                buf.readEnumConstant(TaskButton.class)
        );
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeIdentifier(questId);
        buf.writeString(taskId);
        buf.writeEnumConstant(action);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}

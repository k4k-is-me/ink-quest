package k4k.inkquest.infra.networking;

import k4k.inkquest.TravelcoreQuesting;
import k4k.inkquest.infra.utils.HudTasks;
import k4k.inkquest.questing.models.HudTask;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * Добавляет задачу в HUD закреплённого квеста.
 * Отправляется сервером при событии {@code TASK_LOADED} (без смены этапа).
 */
public record HudTaskAddS2CPacket(
        Identifier questId,
        String taskId,
        HudTask task
) implements FabricPacket {
    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

    public static final PacketType<HudTaskAddS2CPacket> TYPE = PacketType.create(
            Identifier.of(TravelcoreQuesting.MOD_ID, "task-load-s2c"),
            HudTaskAddS2CPacket::read
    );

    /** Читает пакет из буфера. */
    public static HudTaskAddS2CPacket read(PacketByteBuf buf) {
        var questId = buf.readIdentifier();
        var taskId = buf.readString();
        var task = HudTasks.readFromPacketByteBuf(buf);
        return new HudTaskAddS2CPacket(questId, taskId, task);
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeIdentifier(this.questId);
        buf.writeString(this.taskId);
        HudTasks.writeToPacketByteBuf(buf, this.task);
    }
}

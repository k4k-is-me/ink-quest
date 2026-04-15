package k4k.travelcorequesting.infra.networking;

import k4k.travelcorequesting.TravelcoreQuesting;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * Удаляет задачу из HUD закреплённого квеста.
 * Отправляется сервером при событии {@code TASK_UNLOADED} (без смены этапа).
 */
public record HudTaskRemoveS2CPacket(
        Identifier questId,
        String taskId
) implements FabricPacket {
    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

    public static final PacketType<HudTaskRemoveS2CPacket> TYPE = PacketType.create(
            Identifier.of(TravelcoreQuesting.MOD_ID, "task-remove-s2c"),
            HudTaskRemoveS2CPacket::read
    );

    /** Читает пакет из буфера. */
    public static HudTaskRemoveS2CPacket read(PacketByteBuf buf) {
        var questId = buf.readIdentifier();
        var taskId = buf.readString();
        return new HudTaskRemoveS2CPacket(questId, taskId);
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeIdentifier(this.questId);
        buf.writeString(this.taskId);
    }
}

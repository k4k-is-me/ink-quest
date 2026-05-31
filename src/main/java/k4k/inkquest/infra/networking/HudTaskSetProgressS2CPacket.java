package k4k.inkquest.infra.networking;

import k4k.inkquest.TravelcoreQuesting;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * Обновляет прогресс задачи в HUD.
 * Отправляется сервером при изменении счётчика успеха или провала
 * ({@code TASK_SUCCESS_PROGRESS_CHANGED} / {@code TASK_FAILURE_PROGRESS_CHANGED}).
 *
 * @param value             новое абсолютное значение прогресса
 * @param isSuccessProgress {@code true} — прогресс успеха; {@code false} — прогресс провала
 */
public record HudTaskSetProgressS2CPacket(
        Identifier questId,
        String taskId,
        int value,
        boolean isSuccessProgress
) implements FabricPacket {
    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

    public static final PacketType<HudTaskSetProgressS2CPacket> TYPE = PacketType.create(
            Identifier.of(TravelcoreQuesting.MOD_ID, "task-progress-change-s2c"),
            HudTaskSetProgressS2CPacket::read
    );

    /** Читает пакет из буфера. */
    public static HudTaskSetProgressS2CPacket read(PacketByteBuf buf) {
        var questId = buf.readIdentifier();
        var taskId = buf.readString();
        var value = buf.readInt();
        var isSuccessProgress = buf.readBoolean();
        return new HudTaskSetProgressS2CPacket(questId, taskId, value, isSuccessProgress);
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeIdentifier(questId);
        buf.writeString(taskId);
        buf.writeInt(value);
        buf.writeBoolean(isSuccessProgress);
    }
}

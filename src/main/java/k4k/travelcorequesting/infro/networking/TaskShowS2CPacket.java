package k4k.travelcorequesting.infro.networking;

import k4k.travelcorequesting.TravelcoreQuesting;
import k4k.travelcorequesting.infro.utils.TaskDisplays;
import k4k.travelcorequesting.questing.models.TaskDisplay;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public record TaskShowS2CPacket(
        Identifier questId,
        String taskId,
        TaskDisplay task
) implements FabricPacket {
    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

    public static final PacketType<TaskShowS2CPacket> TYPE = PacketType.create(
            Identifier.of(TravelcoreQuesting.MOD_ID, "task-load-s2c"),
            TaskShowS2CPacket::read
    );

    public static TaskShowS2CPacket read(PacketByteBuf buf) {
        var questId = buf.readIdentifier();
        var taskId = buf.readString();
        var task = TaskDisplays.readFromPacketByteBuf(buf);
        return new TaskShowS2CPacket(questId, taskId, task);
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeIdentifier(this.questId);
        buf.writeString(this.taskId);
        TaskDisplays.writeToPacketByteBuf(buf, this.task);
    }
}

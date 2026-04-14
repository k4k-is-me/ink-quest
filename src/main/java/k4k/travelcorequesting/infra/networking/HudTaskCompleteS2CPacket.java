package k4k.travelcorequesting.infra.networking;

import k4k.travelcorequesting.TravelcoreQuesting;
import k4k.travelcorequesting.domain.enums.CompletionStatus;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public record HudTaskCompleteS2CPacket(
    Identifier questId,
    String taskId,
    CompletionStatus status
) implements FabricPacket {
    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

    public static final PacketType<HudTaskCompleteS2CPacket> TYPE = PacketType.create(
            Identifier.of(TravelcoreQuesting.MOD_ID, "task-complete-s2c"),
            HudTaskCompleteS2CPacket::read
    );

    public static HudTaskCompleteS2CPacket read(PacketByteBuf buf) {
        var questId = buf.readIdentifier();
        var taskId = buf.readString();
        var status = CompletionStatus.valueOf(buf.readString());
        return new HudTaskCompleteS2CPacket(questId, taskId, status);
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeIdentifier(questId);
        buf.writeString(taskId);
        buf.writeString(status.toString());
    }
}

package k4k.travelcorequesting.infro.networking;

import k4k.travelcorequesting.TravelcoreQuesting;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public record HudTaskPinS2CPacket(
        Identifier questId,
        String taskId
) implements FabricPacket {
    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

    public static final PacketType<HudTaskPinS2CPacket> TYPE = PacketType.create(
            Identifier.of(TravelcoreQuesting.MOD_ID, "task-pin-s2c"),
            HudTaskPinS2CPacket::read
    );

    public static HudTaskPinS2CPacket read(PacketByteBuf buf) {
        var questId = buf.readIdentifier();
        var taskId = buf.readString();
        return new HudTaskPinS2CPacket(questId, taskId);
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeIdentifier(questId);
        buf.writeString(taskId);
    }
}
package k4k.travelcorequesting.infro.networking;

import k4k.travelcorequesting.TravelcoreQuesting;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public record HudRemoveQuestS2CPacket (
        Identifier questId
) implements FabricPacket {
    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

    public static final PacketType<HudRemoveQuestS2CPacket> TYPE = PacketType.create(
            Identifier.of(TravelcoreQuesting.MOD_ID, "quest-pin-remove-s2c"),
            HudRemoveQuestS2CPacket::read
    );

    public static HudRemoveQuestS2CPacket read(PacketByteBuf buf) {
        var questId = buf.readIdentifier();
        return new HudRemoveQuestS2CPacket(questId);
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeIdentifier(this.questId);
    }
}

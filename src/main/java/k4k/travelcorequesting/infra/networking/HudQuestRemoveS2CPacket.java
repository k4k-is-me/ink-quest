package k4k.travelcorequesting.infra.networking;

import k4k.travelcorequesting.TravelcoreQuesting;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * Удаляет квест из HUD.
 * Отправляется сервером при снятии пина с квеста ({@code QUEST_PIN_REMOVED}).
 */
public record HudQuestRemoveS2CPacket(
        Identifier questId
) implements FabricPacket {
    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

    public static final PacketType<HudQuestRemoveS2CPacket> TYPE = PacketType.create(
            Identifier.of(TravelcoreQuesting.MOD_ID, "quest-pin-remove-s2c"),
            HudQuestRemoveS2CPacket::read
    );

    /** Читает пакет из буфера. */
    public static HudQuestRemoveS2CPacket read(PacketByteBuf buf) {
        var questId = buf.readIdentifier();
        return new HudQuestRemoveS2CPacket(questId);
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeIdentifier(this.questId);
    }
}

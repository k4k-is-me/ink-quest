package k4k.inkquest.common.requests;

import net.minecraft.network.PacketByteBuf;

public interface IPacketEncoder<T> {
    void encode(T dto, PacketByteBuf out);
    T decode(PacketByteBuf in);
}

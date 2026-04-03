package k4k.travelcorequesting.common.requests.encoders;

import com.google.gson.Gson;
import k4k.travelcorequesting.common.requests.IPacketEncoder;
import net.minecraft.network.PacketByteBuf;

public class JsonPacketEncoder<T> implements IPacketEncoder<T> {
    private final Class<T> targetClass;
    private final Gson gsonInstance;

    public JsonPacketEncoder(Class<T> targetClass) {
        this.targetClass = targetClass;
        this.gsonInstance = new Gson();
    }

    public JsonPacketEncoder(Gson gson, Class<T> targetClass) {
        this.targetClass = targetClass;
        this.gsonInstance = gson;
    }

    @Override
    public void encode(T dto, PacketByteBuf out) {
        out.writeString(gsonInstance.toJson(dto));
    }

    @Override
    public T decode(PacketByteBuf in) {
        return gsonInstance.fromJson(in.readString(), this.targetClass);
    }
}

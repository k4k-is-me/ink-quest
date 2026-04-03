package k4k.travelcorequesting.common;

import net.minecraft.nbt.NbtElement;

public interface NbtEncoder<T, K extends NbtElement> {
    K encode(T value);
    T decode(K nbt);
}

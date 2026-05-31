package k4k.inkquest.common;

import net.minecraft.nbt.NbtElement;

public interface NbtEncoder<T, K extends NbtElement> {
    K encode(T value);
    T decode(K nbt);
}

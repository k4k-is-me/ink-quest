package k4k.travelcorequesting.questing.models;

import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public record TaskDisplay(
        Text title,
        @Nullable Text description,
        @Nullable Integer successTarget,
        @Nullable Integer failureTarget
) {
    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }
}

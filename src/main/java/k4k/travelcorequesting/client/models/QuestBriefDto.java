package k4k.travelcorequesting.client.models;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public record QuestBriefDto(
        Text title,
        @Nullable Text description,
        @Nullable Text group,
        Identifier icon
) {}

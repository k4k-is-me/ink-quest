package k4k.travelcorequesting.client.models;

import net.minecraft.text.Text;

public record TaskDto(
        Text title,
        Text description,
        boolean isGradual,
        float completionLevel,
        boolean isComplete
) {}

package k4k.travelcorequesting.client.models;

import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record QuestDto (
    Text title,
    @Nullable Text description,
    List<TaskDto> tasks
) {}

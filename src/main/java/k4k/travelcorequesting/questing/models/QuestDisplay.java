package k4k.travelcorequesting.questing.models;

import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record QuestDisplay (
        Text title,
        @Nullable Text description,
        int sortIndex,
        List<String> tasks  // Порядок задач
) {
    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }
}

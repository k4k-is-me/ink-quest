package k4k.travelcorequesting.domain.abstractions;

import k4k.travelcorequesting.domain.models.TaskEventActions;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public interface Task {
    Text title();
    @Nullable Text description();

    TaskEventActions onLoad();
    TaskEventActions onTick();
    TaskEventActions onPinnedTick();
    TaskEventActions onUnload();
    TaskEventActions onSuccess();
    TaskEventActions onFailure();

    @Nullable ITaskCondition successCondition();
    @Nullable ITaskCondition failureCondition();
}

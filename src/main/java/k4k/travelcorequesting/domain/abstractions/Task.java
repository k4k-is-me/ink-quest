package k4k.travelcorequesting.domain.abstractions;

import k4k.travelcorequesting.domain.enums.TaskButton;
import k4k.travelcorequesting.domain.models.TaskEventActions;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface Task {
    Text title();
    @Nullable Text description();

    /** Набор кнопок ручного завершения задачи в квестовой книге. Пустое множество — кнопок нет. */
    Set<TaskButton> buttons();

    TaskEventActions onLoad();
    TaskEventActions onTick();
    TaskEventActions onPinnedTick();
    TaskEventActions onUnload();
    TaskEventActions onSuccess();
    TaskEventActions onFailure();

    @Nullable ITaskCondition successCondition();
    @Nullable ITaskCondition failureCondition();
}

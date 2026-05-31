package k4k.inkquest.domain.models;

import k4k.inkquest.domain.abstractions.ITaskCondition;
import k4k.inkquest.domain.abstractions.Task;
import k4k.inkquest.domain.enums.TaskButton;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class MutableTask implements Task {
    private Text title;
    private @Nullable Text description;
    private TaskEventActions onLoad;
    private TaskEventActions onTick;
    private TaskEventActions onPinnedTick;
    private TaskEventActions onUnload;
    private TaskEventActions onSuccess;
    private TaskEventActions onFailure;
    private @Nullable ITaskCondition successCondition;
    private @Nullable ITaskCondition failureCondition;
    private Set<TaskButton> buttons;

    private MutableTask(
            Text title,
            @Nullable Text description,
            TaskEventActions onLoad,
            TaskEventActions onTick,
            TaskEventActions onPinnedTick,
            TaskEventActions onUnload,
            TaskEventActions onSuccess,
            TaskEventActions onFailure,
            @Nullable ITaskCondition successCondition,
            @Nullable ITaskCondition failureCondition,
            Set<TaskButton> buttons
    ) {
        this.title = title;
        this.description = description;
        this.onLoad = onLoad;
        this.onTick = onTick;
        this.onPinnedTick = onPinnedTick;
        this.onUnload = onUnload;
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;
        this.successCondition = successCondition;
        this.failureCondition = failureCondition;
        this.buttons = EnumSet.copyOf(buttons.isEmpty() ? EnumSet.noneOf(TaskButton.class) : buttons);
    }

    /** Создаёт задачу с минимальным набором полей — только заголовком. */
    public static MutableTask create(Text title) {
        return new MutableTask(
                title,
                null,
                TaskEventActions.EMPTY,
                TaskEventActions.EMPTY,
                TaskEventActions.EMPTY,
                TaskEventActions.EMPTY,
                TaskEventActions.EMPTY,
                TaskEventActions.EMPTY,
                null,
                null,
                EnumSet.noneOf(TaskButton.class)
        );
    }

    @Override
    public Text title() {
        return title;
    }

    public void setTitle(Text title) {
        this.title = title;
    }

    @Override
    public @Nullable Text description() {
        return description;
    }

    public void setDescription(@Nullable Text description) {
        this.description = description;
    }

    @Override
    public TaskEventActions onLoad() {
        return onLoad;
    }

    /** Устанавливает единственную функцию on.load; null очищает действия. */
    public void setLoadFunction(@Nullable Identifier function) {
        this.onLoad = toSingleFunctionActions(function);
    }

    public void setOnLoad(TaskEventActions actions) {
        this.onLoad = actions;
    }

    @Override
    public TaskEventActions onTick() {
        return onTick;
    }

    /** Устанавливает единственную функцию on.tick; null очищает действия. */
    public void setTickFunction(@Nullable Identifier function) {
        this.onTick = toSingleFunctionActions(function);
    }

    public void setOnTick(TaskEventActions actions) {
        this.onTick = actions;
    }

    @Override
    public TaskEventActions onPinnedTick() {
        return onPinnedTick;
    }

    /** Устанавливает единственную функцию on.pinned_tick; null очищает действия. */
    public void setPinnedTickFunction(@Nullable Identifier function) {
        this.onPinnedTick = toSingleFunctionActions(function);
    }

    public void setOnPinnedTick(TaskEventActions actions) {
        this.onPinnedTick = actions;
    }

    @Override
    public TaskEventActions onUnload() {
        return onUnload;
    }

    /** Устанавливает единственную функцию on.unload; null очищает действия. */
    public void setUnloadFunction(@Nullable Identifier function) {
        this.onUnload = toSingleFunctionActions(function);
    }

    public void setOnUnload(TaskEventActions actions) {
        this.onUnload = actions;
    }

    @Override
    public TaskEventActions onSuccess() {
        return onSuccess;
    }

    /** Устанавливает единственную функцию on.success; null очищает действия. */
    public void setSuccessFunction(@Nullable Identifier function) {
        this.onSuccess = toSingleFunctionActions(function);
    }

    public void setOnSuccess(TaskEventActions actions) {
        this.onSuccess = actions;
    }

    @Override
    public TaskEventActions onFailure() {
        return onFailure;
    }

    /** Устанавливает единственную функцию on.failure; null очищает действия. */
    public void setFailureFunction(@Nullable Identifier function) {
        this.onFailure = toSingleFunctionActions(function);
    }

    public void setOnFailure(TaskEventActions actions) {
        this.onFailure = actions;
    }

    @Override
    public @Nullable ITaskCondition successCondition() {
        return successCondition;
    }

    public void setSuccessCondition(@Nullable ITaskCondition successCondition) {
        this.successCondition = successCondition;
    }

    @Override
    public @Nullable ITaskCondition failureCondition() {
        return failureCondition;
    }

    public void setFailureCondition(@Nullable ITaskCondition failureCondition) {
        this.failureCondition = failureCondition;
    }

    @Override
    public Set<TaskButton> buttons() {
        return Collections.unmodifiableSet(buttons);
    }

    /** Устанавливает набор кнопок ручного завершения. Дубликаты автоматически схлопываются. */
    public void setButtons(Set<TaskButton> buttons) {
        this.buttons = buttons.isEmpty() ? EnumSet.noneOf(TaskButton.class) : EnumSet.copyOf(buttons);
    }

    private static TaskEventActions toSingleFunctionActions(@Nullable Identifier function) {
        if (function == null) return TaskEventActions.EMPTY;
        return new TaskEventActions(List.of(function), List.of());
    }
}

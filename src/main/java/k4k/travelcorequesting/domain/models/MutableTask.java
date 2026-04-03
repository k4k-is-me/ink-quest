package k4k.travelcorequesting.domain.models;

import k4k.travelcorequesting.domain.abstractions.ITaskCondition;
import k4k.travelcorequesting.domain.abstractions.Task;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public final class MutableTask implements Task {
    private Text title;
    private @Nullable Text description;
    private @Nullable Identifier loadFunction;
    private @Nullable Identifier tickFunction;
    private @Nullable Identifier successFunction;
    private @Nullable Identifier failureFunction;
    private @Nullable Identifier unloadFunction;
    private @Nullable ITaskCondition successCondition;
    private @Nullable ITaskCondition failureCondition;
    private boolean isManualSuccess;
    private boolean isManualFailure;

    private MutableTask(
            Text title,
            @Nullable Text description,
            @Nullable Identifier loadFunction,
            @Nullable Identifier tickFunction,
            @Nullable Identifier successFunction,
            @Nullable Identifier failureFunction,
            @Nullable Identifier completedFunction,
            @Nullable ITaskCondition successCondition,
            @Nullable ITaskCondition failureCondition,
            boolean isManualSuccess,
            boolean isManualFailure
            // TaskReward reward
    ) {
        this.title = title;
        this.description = description;
        this.loadFunction = loadFunction;
        this.tickFunction = tickFunction;
        this.successFunction = successFunction;
        this.failureFunction = failureFunction;
        this.unloadFunction = completedFunction;
        this.successCondition = successCondition;
        this.failureCondition = failureCondition;
        this.isManualSuccess = isManualSuccess;
        this.isManualFailure = isManualFailure;
    }

    public static MutableTask create(Text title) {
        return new MutableTask(
                title,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                false
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
    public @Nullable Identifier loadFunction() {
        return loadFunction;
    }

    public void setLoadFunction(@Nullable Identifier loadFunction) {
        this.loadFunction = loadFunction;
    }

    @Override
    public @Nullable Identifier tickFunction() {
        return tickFunction;
    }

    public void setTickFunction(@Nullable Identifier tickFunction) {
        this.tickFunction = tickFunction;
    }

    @Override
    public @Nullable Identifier unloadFunction() {
        return unloadFunction;
    }

    public void setUnloadFunction(@Nullable Identifier unloadFunction) {
        this.unloadFunction = unloadFunction;
    }

    @Override
    public @Nullable Identifier successFunction() {
        return successFunction;
    }

    public void setSuccessFunction(@Nullable Identifier succeededFunction) {
        this.successFunction = succeededFunction;
    }

    @Override
    public boolean isManualSuccess() {
        return this.isManualSuccess;
    }

    public void setManualSuccess(boolean value) {
        this.isManualSuccess = value;
    }

    @Override
    public @Nullable ITaskCondition successCondition() {
        return successCondition;
    }

    public void setSuccessCondition(@Nullable ITaskCondition successCondition) {
        this.successCondition = successCondition;
    }

    @Override
    public @Nullable Identifier failureFunction() {
        return failureFunction;
    }

    public void setFailureFunction(@Nullable Identifier failedFunction) {
        this.failureFunction = failedFunction;
    }

    @Override
    public boolean isManualFailure() {
        return this.isManualFailure;
    }

    public void setManualFailure(boolean value) {
        this.isManualFailure = value;
    }

    @Override
    public @Nullable ITaskCondition failureCondition() {
        return failureCondition;
    }

    public void setFailureCondition(@Nullable ITaskCondition failureCondition) {
        this.failureCondition = failureCondition;
    }
}

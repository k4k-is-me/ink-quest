package k4k.travelcorequesting.domain.abstractions;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public interface Task {
    Text title();
    @Nullable Text description();
    @Nullable Identifier loadFunction();
    @Nullable Identifier tickFunction();
    @Nullable Identifier unloadFunction();
    @Nullable ITaskCondition successCondition();
    @Nullable Identifier successFunction();
    boolean isManualSuccess();
    @Nullable ITaskCondition failureCondition();
    @Nullable Identifier failureFunction();
    boolean isManualFailure();
}

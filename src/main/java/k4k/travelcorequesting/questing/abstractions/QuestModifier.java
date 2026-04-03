package k4k.travelcorequesting.questing.abstractions;

import k4k.travelcorequesting.domain.abstractions.Quest;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public interface QuestModifier {
    boolean isDirty();
    Quest setTitle(Text title);
    Quest setDescription(@Nullable Text description);
    Quest setIcon(Identifier icon);
    Quest setIndex(int index);
    Quest setBackground(boolean isBackground);
    Quest setPin(boolean pin);
    Quest addAndDependency(Identifier dependency);
    Quest addOrDependency(Identifier dependency);
    Quest removeDependencies();
    Quest addTaskRequired(String taskId);
    Quest addTaskOptional(String taskId);
    Quest setTaskTitle(String taskId, Text title);
    Quest setTaskDescription(String taskId, @Nullable Text description);
    Quest setTaskLoadFunction(String taskId, @Nullable Identifier function);
    Quest setTaskTickFunction(String taskId, @Nullable Identifier function);
    Quest setTaskFailedFunction(String taskId, @Nullable Identifier function);
    Quest setTaskSucceededFunction(String taskId, @Nullable Identifier function);
    Quest setTaskUnloadFunction(String taskId, @Nullable Identifier function);
    Quest setTaskSuccessConditionPredicate(String taskId, Identifier predicateId);
    Quest setTaskFailureConditionPredicate(String taskId, Identifier predicateId);
}

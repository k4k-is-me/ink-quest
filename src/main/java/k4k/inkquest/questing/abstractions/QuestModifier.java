package k4k.inkquest.questing.abstractions;

import k4k.inkquest.domain.abstractions.Quest;
import k4k.inkquest.domain.enums.QuestPinMode;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public interface QuestModifier {
    boolean isDirty();
    void setTitle(Text title);
    void setDescription(@Nullable Text description);
    void setIcon(Identifier icon);
    void setIndex(int index);
    void setRepeatable(boolean isRepeatable);
    void setPinMode(QuestPinMode pinMode);
    void addAndDependency(Identifier dependency);
    void addOrDependency(Identifier dependency);
    void removeDependencies();
    void addTaskRequired(String taskId);
    void addTaskOptional(String taskId);
    void removeTask(String taskId);
    void setTaskTitle(String taskId, Text title);
    void setTaskDescription(String taskId, @Nullable Text description);
    Quest setTaskLoadFunction(String taskId, @Nullable Identifier function);
    Quest setTaskTickFunction(String taskId, @Nullable Identifier function);
    Quest setTaskFailedFunction(String taskId, @Nullable Identifier function);
    Quest setTaskSucceededFunction(String taskId, @Nullable Identifier function);
    Quest setTaskUnloadFunction(String taskId, @Nullable Identifier function);
    Quest setTaskSuccessConditionPredicate(String taskId, Identifier predicateId);
    Quest setTaskFailureConditionPredicate(String taskId, Identifier predicateId);
}

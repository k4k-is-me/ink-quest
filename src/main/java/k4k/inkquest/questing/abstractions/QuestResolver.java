package k4k.inkquest.questing.abstractions;

import k4k.inkquest.domain.abstractions.Quest;
import k4k.inkquest.domain.abstractions.Task;
import k4k.inkquest.questing.enums.QuestSourceType;
import k4k.inkquest.questing.enums.TaskType;
import k4k.inkquest.questing.models.QuestEntry;
import k4k.inkquest.questing.models.TaskEntry;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface QuestResolver {
    Quest requireQuest(Identifier questId);
    QuestEntry requireQuestEntry(Identifier questId);
    Task requireTask(Identifier questId, String taskId);
    TaskEntry requireTaskEntry(Identifier questId, String taskId);
    @Nullable Quest getQuest(Identifier questId);
    @Nullable QuestEntry getQuestEntry(Identifier questId);
    @Nullable Task getTask(Identifier questId, String taskId);
    @Nullable TaskEntry getTaskEntry(Identifier questId, String taskId);
    List<Identifier> getQuestIds();

    @Nullable QuestSourceType getQuestSourceType(Identifier questId);
    List<Integer> getTaskStages(Identifier questId, String taskId);
    @Nullable TaskType getTaskType(Identifier questId, String taskId, int stageIndex);
}

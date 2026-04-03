package k4k.travelcorequesting.questing.models;

import k4k.travelcorequesting.domain.abstractions.Quest;
import k4k.travelcorequesting.domain.abstractions.Task;
import k4k.travelcorequesting.questing.enums.QuestSourceType;
import net.minecraft.util.Identifier;

public record TaskEntry(
        Identifier questId,
        Quest quest,
        QuestSourceType source,
        String taskId,
        Task task
) {
        public static TaskEntry fromQuestEntry(QuestEntry entry, String taskId, Task task) {
                return new TaskEntry(entry.questId(), entry.quest(), entry.source(), taskId, task);
        }

        public QuestEntry getQuestEntry() {
                return new QuestEntry(questId, quest, source);
        }
}

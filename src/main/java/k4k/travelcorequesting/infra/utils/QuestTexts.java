package k4k.travelcorequesting.infra.utils;

import k4k.travelcorequesting.questing.enums.TaskType;
import k4k.travelcorequesting.questing.models.QuestEntry;
import k4k.travelcorequesting.questing.models.TaskEntry;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class QuestTexts {
    private static final String MSG_TASKS_COUNT = "quest.tooltip.task_count";
    private static final String MSG_STAGES_COUNT = "quest.tooltip.stage_count";

    private static final Function<TaskType, String> TASK_TYPE_TO_MESSAGE = type -> switch (type) {
        case REQUIRED -> "quest.command.task_type.required";
        case OPTIONAL -> "quest.command.task_type.optional";
        case UNUSED -> "quest.command.task_type.unused";
    };

    public static Text getQuestText(QuestEntry entry) {
        var quest = entry.quest();

        var tooltip = Text.literal("").append(
                toSystemText(entry.questId().toString())
        );

        if (quest.description() != null)
            tooltip.append("\n").append(quest.description());

        if (quest.getTaskCount() > 0)
            tooltip.append("\n").append(Text.translatable(MSG_TASKS_COUNT, quest.getTaskCount()));

        if (quest.getStageCount() > 0)
            tooltip.append("\n").append(Text.translatable(MSG_STAGES_COUNT, quest.getStageCount()));

        return toHoverableText(quest.title(), tooltip);
    }

    public static Text getTaskText(TaskEntry entry, @Nullable TaskType type) {
        var task = entry.task();
        var tooltip = Text.literal("")
                .append(toSystemText(entry.questId().toString()))
                .append(Text.literal(" "))
                .append(toSystemText(entry.taskId()));

        if (task.description() != null)
            tooltip.append("\n").append(task.description());

        var result = toHoverableText(
                task.title(),
                tooltip
        );

        if (type != null)
            Text.literal("").append(result).append(" ")
                    .append(toSystemText(Text.translatable(TASK_TYPE_TO_MESSAGE.apply(type))));

        return result;
    }

    public static Text toHoverableText(Text displayText, Text hoverText) {
        return Texts.bracketed(displayText.copy().styled(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText))));
    }

    public static Text toSystemText(MutableText text) {
        return text.formatted(Formatting.DARK_GRAY, Formatting.ITALIC);
    }

    public static Text toSystemText(String text) {
        return Text.literal(text).formatted(Formatting.DARK_GRAY, Formatting.ITALIC);
    }
}

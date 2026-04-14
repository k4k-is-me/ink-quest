package k4k.travelcorequesting.questing.models;

import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Полные данные квеста для экрана деталей в квестовой книге.
 *
 * @param title       заголовок квеста
 * @param description описание квеста (может отсутствовать)
 * @param tasks       список задач текущего активного этапа (или последнего завершённого)
 */
public record QuestDetailData(
        Text title,
        @Nullable Text description,
        List<TaskDetailData> tasks
) {}

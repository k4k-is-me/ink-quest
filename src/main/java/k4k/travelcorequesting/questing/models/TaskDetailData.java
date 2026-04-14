package k4k.travelcorequesting.questing.models;

import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

/**
 * Детальные данные задачи для экрана квестовой книги.
 *
 * @param title           заголовок задачи
 * @param description     описание задачи (может отсутствовать)
 * @param isGradual       {@code true}, если условие постепенное (показывает прогресс-бар)
 * @param completionLevel текущий прогресс от 0.0 до 1.0; значимо только при {@code isGradual == true}
 * @param isComplete      {@code true}, если задача завершена
 */
public record TaskDetailData(
        Text title,
        @Nullable Text description,
        boolean isGradual,
        float completionLevel,
        boolean isComplete
) {}

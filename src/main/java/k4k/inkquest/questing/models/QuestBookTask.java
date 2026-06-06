package k4k.inkquest.questing.models;

import k4k.inkquest.domain.enums.CompletionStatus;
import k4k.inkquest.domain.enums.TaskButton;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Детальные данные задачи для экрана квестовой книги.
 *
 * @param taskId           идентификатор задачи (нужен для команды закрепления из книги)
 * @param title            заголовок задачи
 * @param description      описание задачи (может отсутствовать)
 * @param hasProgressBar   {@code true}, если условие имеет target > 1 и показывает прогресс-бар
 * @param completionLevel  текущий прогресс от 0.0 до 1.0; значимо только при {@code hasProgressBar == true}
 * @param completionStatus статус завершения; {@code null}, если задача ещё не завершена
 * @param buttons          кнопки ручного завершения задачи; пустое множество — кнопок нет
 */
public record QuestBookTask(
        String taskId,
        Text title,
        @Nullable Text description,
        boolean hasProgressBar,
        float completionLevel,
        @Nullable CompletionStatus completionStatus,
        Set<TaskButton> buttons
) {
    /** Возвращает {@code true}, если задача завершена (любым статусом). */
    public boolean isComplete() {
        return completionStatus != null;
    }
}

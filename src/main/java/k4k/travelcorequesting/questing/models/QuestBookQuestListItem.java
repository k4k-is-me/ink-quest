package k4k.travelcorequesting.questing.models;

import k4k.travelcorequesting.domain.enums.CompletionStatus;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Краткие данные квеста для отображения в списке квестовой книги.
 *
 * @param questId          идентификатор квеста
 * @param title            заголовок квеста
 * @param description      описание квеста (может отсутствовать)
 * @param icon             идентификатор иконки (всегда задан)
 * @param completionStatus статус завершения; {@code null} — квест активен
 * @param index            индекс для сортировки внутри группы
 * @param isPinned         закреплён ли квест в HUD
 * @param viewed           открывал ли игрок детали квеста в книге; всегда {@code false} для завершённых квестов
 */
public record QuestBookQuestListItem(
        Identifier questId,
        Text title,
        @Nullable Text description,
        Identifier icon,
        @Nullable CompletionStatus completionStatus,
        int index,
        boolean isPinned,
        boolean viewed
) {}

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
 * @param icon             идентификатор иконки
 * @param completionStatus статус завершения; {@code null} — квест активен
 * @param index            индекс для сортировки внутри группы
 * @param isPinned         закреплён ли квест в HUD
 */
public record QuestBookQuestListItem(
        Identifier questId,
        Text title,
        @Nullable Text description,
        @Nullable Identifier icon,  // TODO: Icon is not null in the source (MutableQuest)
        @Nullable CompletionStatus completionStatus,
        int index,
        boolean isPinned
) {}

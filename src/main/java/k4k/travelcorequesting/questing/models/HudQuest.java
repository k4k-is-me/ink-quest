package k4k.travelcorequesting.questing.models;

import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Данные квеста для отображения в HUD.
 * Содержит заголовок, описание, индекс сортировки, упорядоченный список
 * идентификаторов задач текущего этапа и tracking-состояние.
 *
 * <p>{@code pinnedTaskId} — идентификатор закреплённой задачи; {@code null} если
 * закреплена первая (required) задача или если квест в принципе не закреплён.
 * Упакован сюда для атомарной передачи полного состояния за один пакет на join.
 *
 * <p>Идентичность определяется по ссылке (не по содержимому), чтобы виджет мог
 * точно определить момент смены квеста.
 */
public record HudQuest(
        Text title,
        @Nullable Text description,
        int sortIndex,
        List<String> tasks,
        @Nullable String pinnedTaskId
) {
    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }
}

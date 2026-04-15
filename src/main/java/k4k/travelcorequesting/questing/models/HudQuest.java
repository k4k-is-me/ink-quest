package k4k.travelcorequesting.questing.models;

import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Данные квеста для отображения в HUD.
 * Содержит только то, что нужно виджету: заголовок, описание, индекс сортировки
 * и упорядоченный список идентификаторов задач текущего этапа.
 *
 * <p>Идентичность определяется по ссылке (не по содержимому), чтобы виджет мог
 * точно определить момент смены квеста.
 */
public record HudQuest(
        Text title,
        @Nullable Text description,
        int sortIndex,
        List<String> tasks  // Порядок задач
) {
    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }
}

package k4k.travelcorequesting.questing.models;

import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

/**
 * Данные задачи для отображения в HUD.
 * Содержит заголовок, описание и целевые значения условий (для прогресс-бара).
 *
 * <p>{@code successTarget}/{@code failureTarget} присутствуют только для постепенных
 * (gradual) условий. {@code null} означает бинарное условие без прогресс-бара.
 *
 * <p>Идентичность определяется по ссылке — аналогично {@link HudQuest}.
 */
public record HudTask(
        Text title,
        @Nullable Text description,
        @Nullable Integer successTarget,
        @Nullable Integer failureTarget
) {
    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }
}

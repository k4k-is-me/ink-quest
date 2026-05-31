package k4k.inkquest.questing.models;

import k4k.inkquest.domain.enums.CompletionStatus;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

/**
 * Данные задачи для отображения в HUD.
 * Содержит заголовок, описание, целевые значения условий (для прогресс-бара)
 * и tracking-состояние на момент отправки пакета (прогресс + статус завершения).
 *
 * <p>{@code successTarget}/{@code failureTarget} присутствуют только для постепенных
 * (gradual) условий. {@code null} означает бинарное условие без прогресс-бара.
 *
 * <p>{@code currentSuccessProgress}/{@code currentFailureProgress} — текущие значения
 * для активных задач; {@code null} для завершённых или если условие не gradual.
 *
 * <p>{@code completionStatus} — {@code null} если задача активна, иначе финальный статус.
 * Используется при инициализации HUD на join: клиент рисует задачу сразу в нужном виде.
 *
 * <p>Идентичность определяется по ссылке — аналогично {@link HudQuest}.
 */
public record HudTask(
        Text title,
        @Nullable Text description,
        @Nullable Integer successTarget,
        @Nullable Integer failureTarget,
        @Nullable Integer currentSuccessProgress,
        @Nullable Integer currentFailureProgress,
        @Nullable CompletionStatus completionStatus
) {
    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }
}

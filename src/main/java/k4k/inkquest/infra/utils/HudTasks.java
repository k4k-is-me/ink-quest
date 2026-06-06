package k4k.inkquest.infra.utils;

import k4k.inkquest.domain.abstractions.Task;
import k4k.inkquest.domain.enums.CompletionStatus;
import k4k.inkquest.questing.models.HudTask;
import net.minecraft.network.PacketByteBuf;
import org.jetbrains.annotations.Nullable;

/** Утилиты сериализации {@link HudTask} в/из сетевых пакетов и фабричные методы из доменной модели. */
public class HudTasks {

    /**
     * Записывает {@link HudTask} в буфер пакета.
     *
     * @param buf  буфер пакета
     * @param task данные для записи
     */
    public static void writeToPacketByteBuf(PacketByteBuf buf, HudTask task) {
        buf.writeText(task.title());

        buf.writeBoolean(task.description() != null);
        if (task.description() != null) buf.writeText(task.description());

        var successTarget = task.successTarget();
        buf.writeBoolean(successTarget != null);
        if (successTarget != null) buf.writeInt(successTarget);

        var failureTarget = task.failureTarget();
        buf.writeBoolean(failureTarget != null);
        if (failureTarget != null) buf.writeInt(failureTarget);

        var currentSuccessProgress = task.currentSuccessProgress();
        buf.writeBoolean(currentSuccessProgress != null);
        if (currentSuccessProgress != null) buf.writeInt(currentSuccessProgress);

        var currentFailureProgress = task.currentFailureProgress();
        buf.writeBoolean(currentFailureProgress != null);
        if (currentFailureProgress != null) buf.writeInt(currentFailureProgress);

        var completionStatus = task.completionStatus();
        buf.writeBoolean(completionStatus != null);
        if (completionStatus != null) buf.writeEnumConstant(completionStatus);
    }

    /**
     * Читает {@link HudTask} из буфера пакета.
     *
     * @param buf буфер пакета
     * @return прочитанные данные
     */
    public static HudTask readFromPacketByteBuf(PacketByteBuf buf) {
        var title = buf.readText();
        var description = buf.readBoolean() ? buf.readText() : null;
        var successTarget = buf.readBoolean() ? buf.readInt() : null;
        var failureTarget = buf.readBoolean() ? buf.readInt() : null;
        var currentSuccessProgress = buf.readBoolean() ? buf.readInt() : null;
        var currentFailureProgress = buf.readBoolean() ? buf.readInt() : null;
        var completionStatus = buf.readBoolean() ? buf.readEnumConstant(CompletionStatus.class) : null;
        return new HudTask(title, description, successTarget, failureTarget,
                currentSuccessProgress, currentFailureProgress, completionStatus);
    }

    /**
     * Создаёт {@link HudTask} из доменной модели задачи с tracking-информацией.
     *
     * <p>{@code successTarget}/{@code failureTarget} передаются только если
     * {@code getTargetValue(condition, context) > 1} (прогресс-бар показывается),
     * иначе — {@code null}. Вычисляются вызывающим кодом через диспетчер условий,
     * поскольку для некоторых типов условий цель зависит от контекста (например,
     * {@code optionals} — размер пула optional-задач активного этапа).
     *
     * <p>{@code currentSuccess}/{@code currentFailure} передаются только если
     * задача активна; для завершённых — {@code null}.
     *
     * @param task             задача
     * @param successTarget    цель условия успеха; {@code null} если условия нет или оно бинарное
     * @param failureTarget    цель условия провала; {@code null} если условия нет или оно бинарное
     * @param currentSuccess   текущее значение условия успеха; {@code null} если нет/неактивно
     * @param currentFailure   текущее значение условия провала; {@code null} если нет/неактивно
     * @param completionStatus статус завершения; {@code null} если задача активна
     * @return данные для HUD-виджета
     */
    public static HudTask fromTask(
            Task task,
            @Nullable Integer successTarget,
            @Nullable Integer failureTarget,
            @Nullable Integer currentSuccess,
            @Nullable Integer currentFailure,
            @Nullable CompletionStatus completionStatus
    ) {
        return new HudTask(
                task.title(),
                task.description(),
                successTarget,
                failureTarget,
                completionStatus == null ? currentSuccess : null,
                completionStatus == null ? currentFailure : null,
                completionStatus
        );
    }
}

package k4k.travelcorequesting.infra.utils;

import k4k.travelcorequesting.domain.abstractions.Task;
import k4k.travelcorequesting.questing.models.HudTask;
import net.minecraft.network.PacketByteBuf;

/** Утилиты сериализации {@link HudTask} в/из сетевых пакетов и фабричный метод из доменной модели. */
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
        return new HudTask(title, description, successTarget, failureTarget);
    }

    /**
     * Создаёт {@link HudTask} из доменной модели задачи.
     * {@code successTarget}/{@code failureTarget} заполняются только для gradual-условий.
     *
     * @param task задача
     * @return данные для HUD-виджета
     */
    public static HudTask fromTask(Task task) {
        var successCondition = task.successCondition();
        var failureCondition = task.failureCondition();
        return new HudTask(
                task.title(),
                task.description(),
                successCondition != null && successCondition.isGradual() ? successCondition.getTargetValue() : null,
                failureCondition != null && failureCondition.isGradual() ? failureCondition.getTargetValue() : null
        );
    }
}

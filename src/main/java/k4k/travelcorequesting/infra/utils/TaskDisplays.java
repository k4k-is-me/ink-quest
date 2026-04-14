package k4k.travelcorequesting.infra.utils;

import k4k.travelcorequesting.domain.abstractions.Task;
import k4k.travelcorequesting.questing.models.TaskDisplay;
import net.minecraft.network.PacketByteBuf;

public class TaskDisplays {
    public static void writeToPacketByteBuf(PacketByteBuf buf, TaskDisplay task) {
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

    public static TaskDisplay readFromPacketByteBuf(PacketByteBuf buf) {
        var title = buf.readText();
        var description = buf.readBoolean() ? buf.readText() : null;
        var successTarget = buf.readBoolean() ? buf.readInt() : null;
        var failureTarget = buf.readBoolean() ? buf.readInt() : null;
        return new TaskDisplay(title, description, successTarget, failureTarget);
    }

    public static TaskDisplay fromTask(Task task) {
        var successCondition = task.successCondition();
        var failureCondition = task.failureCondition();
        return new TaskDisplay(
                task.title(),
                task.description(),
                successCondition != null && successCondition.isGradual() ? successCondition.getTargetValue() : null,
                failureCondition != null && failureCondition.isGradual() ? failureCondition.getTargetValue() : null
        );
    }
}

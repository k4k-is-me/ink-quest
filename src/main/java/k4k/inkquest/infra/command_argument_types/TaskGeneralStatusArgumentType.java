package k4k.inkquest.infra.command_argument_types;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.Codec;
import k4k.inkquest.infra.enums.TaskGeneralStatus;
import net.minecraft.command.argument.EnumArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.StringIdentifiable;

import java.util.Locale;

public class TaskGeneralStatusArgumentType extends EnumArgumentType<TaskGeneralStatusArgumentType.StringIdentifiableTaskGeneralStatus> {
    private static final Codec<StringIdentifiableTaskGeneralStatus> CODEC = StringIdentifiable.createCodec(
            StringIdentifiableTaskGeneralStatus::values,
            name -> name.toLowerCase(Locale.ROOT)
    );

    protected TaskGeneralStatusArgumentType() {
        super(CODEC, StringIdentifiableTaskGeneralStatus::values);
    }

    public static TaskGeneralStatusArgumentType taskGeneralStatus() {
        return new TaskGeneralStatusArgumentType();
    }

    public static TaskGeneralStatus getTaskGeneralStatus(CommandContext<ServerCommandSource> context, String id) {
        return context.getArgument(id, StringIdentifiableTaskGeneralStatus.class).toTaskGeneralStatus();
    }

    protected enum StringIdentifiableTaskGeneralStatus implements StringIdentifiable {
        ACTIVE("active"),
        COMPLETE("complete"),
        SUCCEEDED("succeeded"),
        FAILED("failed"),
        SKIPPED("skipped"),
        PINNED("pinned");

        final String name;

        StringIdentifiableTaskGeneralStatus(String name) {
            this.name = name;
        }

        @Override
        public String asString() {
            return this.name;
        }

        public TaskGeneralStatus toTaskGeneralStatus() {
            return switch (this) {
                case ACTIVE -> TaskGeneralStatus.ACTIVE;
                case COMPLETE -> TaskGeneralStatus.COMPLETE;
                case SUCCEEDED -> TaskGeneralStatus.SUCCEEDED;
                case FAILED -> TaskGeneralStatus.FAILED;
                case SKIPPED -> TaskGeneralStatus.SKIPPED;
                case PINNED -> TaskGeneralStatus.PINNED;
            };
        }
    }
}

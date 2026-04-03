package k4k.travelcorequesting.infro.command_argument_types;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.Codec;
import k4k.travelcorequesting.domain.enums.CompletionStatus;
import net.minecraft.command.argument.EnumArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.StringIdentifiable;

import java.util.Locale;

public class CompletionStatusArgumentType extends EnumArgumentType<CompletionStatusArgumentType.StringIdentifiableCompletionStatus> {
    private static final Codec<CompletionStatusArgumentType.StringIdentifiableCompletionStatus> CODEC = StringIdentifiable.createCodec(
            StringIdentifiableCompletionStatus::values,
            name -> name.toLowerCase(Locale.ROOT)
    );

    private CompletionStatusArgumentType() {
        super(CODEC, StringIdentifiableCompletionStatus::values);
    }

    public static CompletionStatusArgumentType completionStatus() {
        return new CompletionStatusArgumentType();
    }

    public static CompletionStatus getCompletionStatus(CommandContext<ServerCommandSource> context, String id) {
        return context.getArgument(id, StringIdentifiableCompletionStatus.class).toCompletionStatus();
    }

    protected enum StringIdentifiableCompletionStatus implements StringIdentifiable {
        SUCCESS("success"),
        FAILURE("failure"),
        SKIPPED("skip");

        final String name;

        StringIdentifiableCompletionStatus(String name) {
            this.name = name;
        }

        @Override
        public String asString() {
            return this.name.toLowerCase(Locale.ROOT);
        }

        public CompletionStatus toCompletionStatus() {
            return switch (this) {
                case SUCCESS -> CompletionStatus.SUCCESS;
                case FAILURE -> CompletionStatus.FAILURE;
                case SKIPPED -> CompletionStatus.SKIPPED;
            };
        }
    }
}

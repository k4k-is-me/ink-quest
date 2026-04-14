package k4k.travelcorequesting.infra.command_argument_types;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.Codec;
import k4k.travelcorequesting.infra.enums.CompletionLevel;
import net.minecraft.command.argument.EnumArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.StringIdentifiable;

import java.util.Locale;

public class CompletionLevelArgumentType extends EnumArgumentType<CompletionLevelArgumentType.StringIdentifiableCompletionLevel> {
    private static final Codec<CompletionLevelArgumentType.StringIdentifiableCompletionLevel> CODEC = StringIdentifiable.createCodec(
            StringIdentifiableCompletionLevel::values,
            name -> name.toLowerCase(Locale.ROOT)
    );

    protected CompletionLevelArgumentType() {
        super(CODEC, StringIdentifiableCompletionLevel::values);
    }

    public static CompletionLevelArgumentType completionLevel() {
        return new CompletionLevelArgumentType();
    }

    public static CompletionLevel getCompletionLevel(CommandContext<ServerCommandSource> context, String id) {
        return context.getArgument(id, StringIdentifiableCompletionLevel.class).toCompletionLevel();
    }

    protected enum StringIdentifiableCompletionLevel implements StringIdentifiable {
        FULL("full"),
        REQUIRED("required");

        final String name;

        StringIdentifiableCompletionLevel(String name) {
            this.name = name;
        }

        @Override
        public String asString() {
            return this.name.toLowerCase(Locale.ROOT);
        }

        public CompletionLevel toCompletionLevel() {
            return switch (this) {
                case FULL -> CompletionLevel.FULL;
                case REQUIRED -> CompletionLevel.REQUIRED;
            };
        }
    }
}

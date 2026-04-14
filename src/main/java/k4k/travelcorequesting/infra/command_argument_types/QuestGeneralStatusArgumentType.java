package k4k.travelcorequesting.infra.command_argument_types;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.Codec;
import k4k.travelcorequesting.infra.enums.QuestGeneralStatus;
import net.minecraft.command.argument.EnumArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.StringIdentifiable;

import java.util.Locale;

public class QuestGeneralStatusArgumentType extends EnumArgumentType<QuestGeneralStatusArgumentType.StringIdentifiableQuestGeneralStatus> {
    private static final Codec<StringIdentifiableQuestGeneralStatus> CODEC = StringIdentifiable.createCodec(
            StringIdentifiableQuestGeneralStatus::values,
            name -> name.toLowerCase(Locale.ROOT)
    );

    protected QuestGeneralStatusArgumentType() {
        super(CODEC, StringIdentifiableQuestGeneralStatus::values);
    }

    public static QuestGeneralStatusArgumentType questGeneralStatus() {
        return new QuestGeneralStatusArgumentType();
    }

    public static QuestGeneralStatus getQuestGeneralStatus(CommandContext<ServerCommandSource> context, String id) {
        return context.getArgument(id, StringIdentifiableQuestGeneralStatus.class).toQuestGeneralStatus();
    }

    protected enum StringIdentifiableQuestGeneralStatus implements StringIdentifiable {
        ACTIVE("active"),
        COMPLETE("complete"),
        SUCCEEDED("succeeded"),
        FAILED("failed"),
        SKIPPED("skipped"),
        PINNED("pinned");

        final String name;

        StringIdentifiableQuestGeneralStatus(String name) {
            this.name = name;
        }

        @Override
        public String asString() {
            return this.name;
        }

        public QuestGeneralStatus toQuestGeneralStatus() {
            return switch (this) {
                case ACTIVE -> QuestGeneralStatus.ACTIVE;
                case COMPLETE -> QuestGeneralStatus.COMPLETE;
                case SUCCEEDED -> QuestGeneralStatus.SUCCEEDED;
                case FAILED -> QuestGeneralStatus.FAILED;
                case SKIPPED -> QuestGeneralStatus.SKIPPED;
                case PINNED -> QuestGeneralStatus.PINNED;
            };
        }
    }
}

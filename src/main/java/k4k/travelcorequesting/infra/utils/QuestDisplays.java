package k4k.travelcorequesting.infra.utils;

import k4k.travelcorequesting.domain.abstractions.Quest;
import k4k.travelcorequesting.questing.models.QuestDisplay;
import net.minecraft.network.PacketByteBuf;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.stream.IntStream;

public class QuestDisplays {
    public static void writeToPacketByteBuf(QuestDisplay quest, PacketByteBuf buf) {
        buf.writeText(quest.title());

        buf.writeBoolean(quest.description() != null);
        if (quest.description() != null) buf.writeText(quest.description());

        buf.writeInt(quest.sortIndex());

        buf.writeInt(quest.tasks().size());
        quest.tasks().forEach(buf::writeString);
    }

    public static QuestDisplay readFromPacketByteBuf(PacketByteBuf buf) {
        var title = buf.readText();

        var description = buf.readBoolean() ? buf.readText() : null;

        var sortIndex = buf.readInt();

        var taskCount = buf.readInt();
        var tasks = IntStream.range(0, taskCount)
                .mapToObj(i -> buf.readString())
                .toList();
        return new QuestDisplay(title, description, sortIndex, tasks);
    }

    public static QuestDisplay fromQuest(Quest quest, @Nullable Integer stage) {
        return new QuestDisplay(
                quest.title(),
                quest.description(),
                quest.index(),
                stage != null
                        ? quest.getStage(stage)
                        : Collections.emptyList()
        );
    }
}

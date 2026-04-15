package k4k.travelcorequesting.infra.utils;

import k4k.travelcorequesting.domain.abstractions.Quest;
import k4k.travelcorequesting.questing.models.HudQuest;
import net.minecraft.network.PacketByteBuf;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.stream.IntStream;

/** Утилиты сериализации {@link HudQuest} в/из сетевых пакетов и фабричный метод из доменной модели. */
public class HudQuests {

    /**
     * Записывает {@link HudQuest} в буфер пакета.
     *
     * @param quest данные для записи
     * @param buf   буфер пакета
     */
    public static void writeToPacketByteBuf(HudQuest quest, PacketByteBuf buf) {
        buf.writeText(quest.title());

        buf.writeBoolean(quest.description() != null);
        if (quest.description() != null) buf.writeText(quest.description());

        buf.writeInt(quest.sortIndex());

        buf.writeInt(quest.tasks().size());
        quest.tasks().forEach(buf::writeString);
    }

    /**
     * Читает {@link HudQuest} из буфера пакета.
     *
     * @param buf буфер пакета
     * @return прочитанные данные
     */
    public static HudQuest readFromPacketByteBuf(PacketByteBuf buf) {
        var title = buf.readText();

        var description = buf.readBoolean() ? buf.readText() : null;

        var sortIndex = buf.readInt();

        var taskCount = buf.readInt();
        var tasks = IntStream.range(0, taskCount)
                .mapToObj(i -> buf.readString())
                .toList();
        return new HudQuest(title, description, sortIndex, tasks);
    }

    /**
     * Создаёт {@link HudQuest} из доменной модели квеста.
     * Если этап не задан, список задач будет пустым.
     *
     * @param quest квест
     * @param stage индекс активного этапа; {@code null} — этап не определён
     * @return данные для HUD-виджета
     */
    public static HudQuest fromQuest(Quest quest, @Nullable Integer stage) {
        return new HudQuest(
                quest.title(),
                quest.description(),
                quest.index(),
                stage != null
                        ? quest.getStage(stage)
                        : Collections.emptyList()
        );
    }
}

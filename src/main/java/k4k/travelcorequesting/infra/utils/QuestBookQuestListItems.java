package k4k.travelcorequesting.infra.utils;

import k4k.travelcorequesting.domain.enums.CompletionStatus;
import k4k.travelcorequesting.questing.models.QuestBookQuestListItem;
import net.minecraft.network.PacketByteBuf;

/** Утилиты сериализации {@link QuestBookQuestListItem} в/из сетевых пакетов. */
public class QuestBookQuestListItems {

    /**
     * Записывает {@link QuestBookQuestListItem} в буфер пакета.
     *
     * @param data данные для записи
     * @param buf  буфер пакета
     */
    public static void writeToPacketByteBuf(QuestBookQuestListItem data, PacketByteBuf buf) {
        buf.writeIdentifier(data.questId());
        buf.writeText(data.title());

        buf.writeBoolean(data.description() != null);
        if (data.description() != null) buf.writeText(data.description());

        buf.writeIdentifier(data.icon());

        buf.writeBoolean(data.completionStatus() != null);
        if (data.completionStatus() != null) buf.writeEnumConstant(data.completionStatus());

        buf.writeInt(data.index());
        buf.writeBoolean(data.isPinned());
        buf.writeBoolean(data.viewed());
    }

    /**
     * Читает {@link QuestBookQuestListItem} из буфера пакета.
     *
     * @param buf буфер пакета
     * @return прочитанные данные
     */
    public static QuestBookQuestListItem readFromPacketByteBuf(PacketByteBuf buf) {
        var questId = buf.readIdentifier();
        var title = buf.readText();
        var description = buf.readBoolean() ? buf.readText() : null;
        var icon = buf.readIdentifier();
        var completionStatus = buf.readBoolean() ? buf.readEnumConstant(CompletionStatus.class) : null;
        var index = buf.readInt();
        var isPinned = buf.readBoolean();
        var viewed = buf.readBoolean();

        return new QuestBookQuestListItem(questId, title, description, icon, completionStatus, index, isPinned, viewed);
    }
}

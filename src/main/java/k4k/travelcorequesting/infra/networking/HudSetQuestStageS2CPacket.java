package k4k.travelcorequesting.infra.networking;

import k4k.travelcorequesting.TravelcoreQuesting;
import k4k.travelcorequesting.infra.utils.QuestDisplays;
import k4k.travelcorequesting.infra.utils.TaskDisplays;
import k4k.travelcorequesting.questing.models.QuestDisplay;
import k4k.travelcorequesting.questing.models.TaskDisplay;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public record HudSetQuestStageS2CPacket(
        Identifier questId,
        QuestDisplay quest,
        Map<String, TaskDisplay> tasks
) implements FabricPacket {
    @Override
    public PacketType<?> getType() {
        return TYPE;
    }

    public static final PacketType<HudSetQuestStageS2CPacket> TYPE = PacketType.create(
            Identifier.of(TravelcoreQuesting.MOD_ID, "quest-set-stage-s2c"),
            HudSetQuestStageS2CPacket::read
    );

    public static HudSetQuestStageS2CPacket read(PacketByteBuf buf) {
        var questId = buf.readIdentifier();
        var quest = QuestDisplays.readFromPacketByteBuf(buf);

        var tasksSize = buf.readInt();
        var tasks = new HashMap<String, TaskDisplay>(tasksSize);
        for (var i = 0; i < tasksSize; i++) {
            var key = buf.readString();
            var value = TaskDisplays.readFromPacketByteBuf(buf);
            tasks.put(key, value);
        }

        return new HudSetQuestStageS2CPacket(questId, quest, tasks);
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeIdentifier(this.questId);
        QuestDisplays.writeToPacketByteBuf(this.quest, buf);

        buf.writeInt(this.tasks.size());
        for (var taskEntry : this.tasks.entrySet()) {
            buf.writeString(taskEntry.getKey());
            TaskDisplays.writeToPacketByteBuf(buf, taskEntry.getValue());
        }
    }
}

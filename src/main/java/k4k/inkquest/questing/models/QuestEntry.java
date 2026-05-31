package k4k.inkquest.questing.models;

import k4k.inkquest.domain.abstractions.Quest;
import k4k.inkquest.questing.enums.QuestSourceType;
import net.minecraft.util.Identifier;

public record QuestEntry(
        Identifier questId,
        Quest quest,
        QuestSourceType source
) {}

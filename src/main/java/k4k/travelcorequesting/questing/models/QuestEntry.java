package k4k.travelcorequesting.questing.models;

import k4k.travelcorequesting.domain.abstractions.Quest;
import k4k.travelcorequesting.questing.enums.QuestSourceType;
import net.minecraft.util.Identifier;

public record QuestEntry(
        Identifier questId,
        Quest quest,
        QuestSourceType source
) {}

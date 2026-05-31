package k4k.inkquest.infra.items;

import k4k.inkquest.TravelcoreQuesting;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/** Реестр всех предметов мода. */
public final class ModItems {

    /** Книга квестов — открывает экран квестов по ПКМ. */
    public static final Item QUEST_BOOK = new QuestBookItem(new FabricItemSettings().maxCount(1));

    /** Свиток квеста — выдаёт квест из NBT-поля Quest по ПКМ; стакается до 16. */
    public static final Item QUEST_SCROLL = new QuestScrollItem(new FabricItemSettings().maxCount(16));

    private ModItems() {}

    /** Регистрирует предметы и добавляет их в группы creative-инвентаря. */
    public static void register() {
        Registry.register(Registries.ITEM, Identifier.of(TravelcoreQuesting.MOD_ID, "quest_book"), QUEST_BOOK);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(QUEST_BOOK));

        Registry.register(Registries.ITEM, Identifier.of(TravelcoreQuesting.MOD_ID, "quest_scroll"), QUEST_SCROLL);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(QUEST_SCROLL));
    }
}

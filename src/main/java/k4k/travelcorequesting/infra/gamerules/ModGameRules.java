package k4k.travelcorequesting.infra.gamerules;

import k4k.travelcorequesting.infra.items.ModItems;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.GameRules;

/** Реестр gamerule мода. */
public final class ModGameRules {

    public static final GameRules.Key<GameRules.BooleanRule> DO_QUEST_BOOK_ITEM_CHECK =
            GameRuleRegistry.register(
                    "doQuestBookItemCheck",
                    GameRules.Category.PLAYER,
                    GameRuleFactory.createBooleanRule(false)
            );

    private ModGameRules() {}

    /** Инициализирует класс, регистрируя все gamerule мода. */
    public static void register() {}

    /**
     * Возвращает {@code true}, если игрок может открыть квестовую книгу с учётом gamerule и инвентаря.
     * При {@code doQuestBookItemCheck = false} (по умолчанию) всегда {@code true}.
     */
    public static boolean canPlayerOpenQuestBook(PlayerEntity player) {
        if (!player.getWorld().getGameRules().getBoolean(DO_QUEST_BOOK_ITEM_CHECK)) return true;
        return player.getInventory().count(ModItems.QUEST_BOOK) > 0;
    }
}

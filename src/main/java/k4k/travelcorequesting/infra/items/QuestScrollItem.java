package k4k.travelcorequesting.infra.items;

import k4k.travelcorequesting.infra.sounds.ModSounds;
import k4k.travelcorequesting.questing.abstractions.ServerQuestManagerContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * Игровой предмет, выдающий квест игроку по ПКМ.
 * Идентификатор квеста хранится в NBT-поле {@value #NBT_QUEST_KEY}.
 */
public class QuestScrollItem extends Item {

    private static final String NBT_QUEST_KEY = "Quest";
    private static final int USE_COOLDOWN_TICKS = 20;

    /** @param settings стандартные настройки предмета */
    public QuestScrollItem(Settings settings) {
        super(settings);
    }

    /**
     * Выдаёт игроку квест, указанный в NBT-поле {@value #NBT_QUEST_KEY}.
     * Если квест уже активен или завершён — показывает сообщение на action-bar и не расходует свиток.
     * Если квест успешно выдан — уменьшает стак на 1 (кроме creative) и устанавливает cooldown.
     */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        var stack = user.getStackInHand(hand);

        // Клиент предсказывает успех для анимации руки; авторитетное решение принимает сервер
        if (world.isClient()) return TypedActionResult.success(stack, true);

        var server = user.getServer();
        if (server == null) return TypedActionResult.fail(stack);

        var nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(NBT_QUEST_KEY, NbtElement.STRING_TYPE)) {
            user.sendMessage(Text.translatable("item.travelcorequesting.quest_scroll.error.no_quest"), true);
            return TypedActionResult.fail(stack);
        }

        var questId = Identifier.tryParse(nbt.getString(NBT_QUEST_KEY));
        if (questId == null) {
            user.sendMessage(Text.translatable("item.travelcorequesting.quest_scroll.error.invalid_id"), true);
            return TypedActionResult.fail(stack);
        }

        var manager = ServerQuestManagerContainer.getQuestManager(server);
        var serverPlayer = (ServerPlayerEntity) user;

        if (!manager.isQuestExists(questId)) {
            user.sendMessage(Text.translatable("item.travelcorequesting.quest_scroll.error.unknown_quest", questId.toString()), true);
            return TypedActionResult.fail(stack);
        }

        // complete проверяется раньше active: если квест завершён, сообщение точнее
        if (manager.isQuestComplete(questId, serverPlayer)) {
            user.sendMessage(Text.translatable("item.travelcorequesting.quest_scroll.error.already_complete"), true);
            return TypedActionResult.fail(stack);
        }

        if (manager.isQuestActive(questId, serverPlayer)) {
            user.sendMessage(Text.translatable("item.travelcorequesting.quest_scroll.error.already_active"), true);
            return TypedActionResult.fail(stack);
        }

        manager.giveQuest(questId, serverPlayer);
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                ModSounds.QUEST_SCROLL_UNFURL, SoundCategory.PLAYERS, 1.0f, 1.0f);
        if (!user.getAbilities().creativeMode) stack.decrement(1);
        user.getItemCooldownManager().set(this, USE_COOLDOWN_TICKS);
        return TypedActionResult.success(stack, false);
    }
}

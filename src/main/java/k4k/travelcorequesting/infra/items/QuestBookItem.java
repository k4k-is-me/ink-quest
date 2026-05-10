package k4k.travelcorequesting.infra.items;

import k4k.travelcorequesting.client.screens.QuestBookQuestsScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** Игровой предмет, открывающий экран книги квестов по ПКМ. */
public class QuestBookItem extends Item {

    /** @param settings стандартные настройки предмета */
    public QuestBookItem(Settings settings) {
        super(settings);
    }

    /**
     * Открывает экран книги квестов на клиенте.
     * На dedicated server не выполняется — метод стрипается через @Environment.
     */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient) openClientScreen();
        return TypedActionResult.success(user.getStackInHand(hand), !world.isClient());
    }

    /** Вызывает MinecraftClient.setScreen на стороне клиента. */
    @Environment(EnvType.CLIENT)
    private static void openClientScreen() {
        MinecraftClient.getInstance().setScreen(new QuestBookQuestsScreen());
    }
}

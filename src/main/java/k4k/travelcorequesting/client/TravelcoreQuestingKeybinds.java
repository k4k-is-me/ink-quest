package k4k.travelcorequesting.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class TravelcoreQuestingKeybinds {
    public static final KeyBinding OPEN_QUESTS = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "travelcore-questing.questbook.questlist",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "travelcore-questing.keybindings.category"
    ));

    @SuppressWarnings("EmptyMethod")
    public static void register() {}
}

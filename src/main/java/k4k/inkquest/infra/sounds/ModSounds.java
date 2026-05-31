package k4k.inkquest.infra.sounds;

import k4k.inkquest.TravelcoreQuesting;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/** Реестр всех звуковых событий мода. */
public final class ModSounds {

    /** Звук разворачивания свитка при его использовании. */
    public static final SoundEvent QUEST_SCROLL_UNFURL =
            SoundEvent.of(Identifier.of(TravelcoreQuesting.MOD_ID, "item.quest_scroll.unfurl"));

    private ModSounds() {}

    /** Регистрирует звуковые события мода. */
    public static void register() {
        Registry.register(
                Registries.SOUND_EVENT,
                Identifier.of(TravelcoreQuesting.MOD_ID, "item.quest_scroll.unfurl"),
                QUEST_SCROLL_UNFURL
        );
    }
}

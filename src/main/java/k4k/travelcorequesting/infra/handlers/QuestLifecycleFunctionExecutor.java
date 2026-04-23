package k4k.travelcorequesting.infra.handlers;

import k4k.travelcorequesting.questing.events.QuestProgressEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Инфраструктурный обработчик, выполняющий Minecraft-функции из lifecycle-хуков задачи.
 *
 * <p>Слушает события {@link QuestProgressEvents} и вызывает соответствующие функции
 * через {@code CommandFunctionManager} сервера.
 */
public class QuestLifecycleFunctionExecutor {

    /** Регистрирует все обработчики lifecycle-функций задач. */
    public static void register() {
        QuestProgressEvents.TASK_LOADED.register((taskEntry, player, stageChanged) ->
                executeFunction(taskEntry.task().loadFunction(), player)
        );

        QuestProgressEvents.TASK_TICKED.register((taskEntry, player) ->
                executeFunction(taskEntry.task().tickFunction(), player)
        );

        QuestProgressEvents.TASK_UNLOADED.register((taskEntry, player, stageChanged) ->
                executeFunction(taskEntry.task().unloadFunction(), player)
        );

        QuestProgressEvents.TASK_COMPLETED.register((taskEntry, player, status) -> {
            if (taskEntry == null) return;
            var fn = switch (status) {
                case SUCCESS -> taskEntry.task().successFunction();
                case FAILURE -> taskEntry.task().failureFunction();
                case SKIPPED -> null;
            };
            executeFunction(fn, player);
        });
    }

    /**
     * Выполняет Minecraft-функцию с источником команды игрока.
     *
     * @param id     идентификатор функции, {@code null} — нет функции
     * @param player игрок, чей {@code CommandSource} будет executor-ом функции
     */
    private static void executeFunction(@Nullable Identifier id, ServerPlayerEntity player) {
        if (id == null) return;

        var server = player.getServer();
        if (server == null) return;

        server.getCommandFunctionManager().getFunction(id).ifPresent(function ->
                server.getCommandFunctionManager().execute(function, player.getCommandSource().withSilent())
        );
    }
}

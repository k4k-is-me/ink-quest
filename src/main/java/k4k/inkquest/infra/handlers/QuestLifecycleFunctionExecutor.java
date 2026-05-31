package k4k.inkquest.infra.handlers;

import k4k.inkquest.domain.models.TaskEventActions;
import k4k.inkquest.questing.events.QuestProgressEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Инфраструктурный обработчик, выполняющий Minecraft-функции и применяющий
 * scoreboard-теги из lifecycle-хуков задачи.
 *
 * <p>Слушает события {@link QuestProgressEvents} и вызывает соответствующие действия
 * через {@code CommandFunctionManager} сервера.
 */
public class QuestLifecycleFunctionExecutor {

    /** Регистрирует все обработчики lifecycle-действий задач. */
    public static void register() {
        QuestProgressEvents.TASK_LOADED.register((taskEntry, player, stageChanged) ->
                executeActions(taskEntry.task().onLoad(), player)
        );

        QuestProgressEvents.TASK_TICKED.register((taskEntry, player) ->
                executeActions(taskEntry.task().onTick(), player)
        );

        QuestProgressEvents.PINNED_TASK_TICKED.register((taskEntry, player) ->
                executeActions(taskEntry.task().onPinnedTick(), player)
        );

        QuestProgressEvents.TASK_UNLOADED.register((taskEntry, player, stageChanged) ->
                executeActions(taskEntry.task().onUnload(), player)
        );

        QuestProgressEvents.TASK_COMPLETED.register((taskEntry, player, status) -> {
            if (taskEntry == null) return;
            var actions = switch (status) {
                case SUCCESS -> taskEntry.task().onSuccess();
                case FAILURE -> taskEntry.task().onFailure();
                case SKIPPED -> TaskEventActions.EMPTY;
            };
            executeActions(actions, player);
        });
    }

    /**
     * Применяет все теги и выполняет все функции из набора действий события.
     *
     * <p>Теги применяются первыми, чтобы функции этого же хука уже видели их
     * при выполнении.
     *
     * @param actions набор действий события
     * @param player  игрок, чей {@code CommandSource} будет executor-ом функций
     */
    private static void executeActions(TaskEventActions actions, ServerPlayerEntity player) {
        for (var tag : actions.tags()) {
            player.addCommandTag(tag);
        }
        for (var id : actions.functions()) {
            executeFunction(id, player);
        }
    }

    /**
     * Выполняет Minecraft-функцию с источником команды игрока.
     *
     * @param id     идентификатор функции
     * @param player игрок, чей {@code CommandSource} будет executor-ом функции
     */
    private static void executeFunction(Identifier id, ServerPlayerEntity player) {
        var server = player.getServer();
        if (server == null) return;

        server.getCommandFunctionManager().getFunction(id).ifPresent(function ->
                server.getCommandFunctionManager().execute(function, player.getCommandSource().withSilent())
        );
    }
}

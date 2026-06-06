package k4k.inkquest.infra.requests;

import k4k.inkquest.TravelcoreQuesting;
import k4k.inkquest.common.requests.ClientRequest;
import k4k.inkquest.common.requests.ClientRequests;
import k4k.inkquest.common.requests.IPacketEncoder;
import k4k.inkquest.domain.enums.CompletionStatus;
import k4k.inkquest.domain.enums.TaskButton;
import k4k.inkquest.questing.abstractions.QuestResolver;
import k4k.inkquest.questing.abstractions.ServerQuestManagerContainer;
import k4k.inkquest.questing.models.QuestBookQuest;
import k4k.inkquest.questing.models.QuestBookTask;
import k4k.inkquest.questing.services.ServerQuestManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Инфраструктура C2S запроса деталей квеста.
 *
 * <p>Клиент отправляет {@link GetQuestDetailsRequest}, сервер отвечает {@link GetQuestDetailResponse}.
 * Сервер применяет rate limit {@value SERVER_RATE_LIMIT_MS} мс per (игрок, квест) — разные квесты
 * не конкурируют за один таймер. До rate-limit проверки выполняется проверка принадлежности квеста
 * игроку — это исключает раздувание Map чередой несуществующих/чужих id.
 *
 * <p>Записи для игрока очищаются при его отключении — см. {@link #registerServerCleanup()}.
 */
public class GetQuestDetailsClientRequest {

    /** Минимальный интервал между запросами от одного игрока для одного квеста на сервере, мс. */
    public static final long SERVER_RATE_LIMIT_MS = 500L;

    /**
     * Последние временные метки запросов per (игрок, квест). Внешний ключ — UUID игрока,
     * внутренний — {@link Identifier} квеста. Очищается при отключении игрока.
     */
    private static final Map<UUID, Map<Identifier, Long>> lastRequestTimes = new ConcurrentHashMap<>();

    private static final IPacketEncoder<GetQuestDetailsRequest> REQUEST_ENCODER = new IPacketEncoder<>() {
        @Override
        public void encode(GetQuestDetailsRequest request, PacketByteBuf buf) {
            buf.writeIdentifier(request.questId());
        }

        @Override
        public GetQuestDetailsRequest decode(PacketByteBuf buf) {
            return new GetQuestDetailsRequest(buf.readIdentifier());
        }
    };

    private static final IPacketEncoder<GetQuestDetailResponse> RESPONSE_ENCODER = new IPacketEncoder<>() {
        @Override
        public void encode(GetQuestDetailResponse response, PacketByteBuf buf) {
            buf.writeBoolean(response.data() != null);
            if (response.data() == null) return;

            var data = response.data();
            buf.writeText(data.title());

            buf.writeBoolean(data.description() != null);
            if (data.description() != null) buf.writeText(data.description());

            buf.writeBoolean(data.pinnedTaskId() != null);
            if (data.pinnedTaskId() != null) buf.writeString(data.pinnedTaskId());

            buf.writeInt(data.tasks().size());
            for (var task : data.tasks()) {
                buf.writeString(task.taskId());
                buf.writeText(task.title());

                buf.writeBoolean(task.description() != null);
                if (task.description() != null) buf.writeText(task.description());

                buf.writeBoolean(task.isGradual());
                buf.writeFloat(task.completionLevel());
                buf.writeBoolean(task.completionStatus() != null);
                if (task.completionStatus() != null) buf.writeByte(task.completionStatus().ordinal());

                // кнопки как 3-битная маска: bit0=SUCCESS, bit1=FAILURE, bit2=SKIP
                int buttonMask = 0;
                if (task.buttons().contains(TaskButton.SUCCESS)) buttonMask |= 1;
                if (task.buttons().contains(TaskButton.FAILURE)) buttonMask |= 2;
                if (task.buttons().contains(TaskButton.SKIP))    buttonMask |= 4;
                buf.writeByte(buttonMask);
            }
        }

        @Override
        public GetQuestDetailResponse decode(PacketByteBuf buf) {
            if (!buf.readBoolean()) return new GetQuestDetailResponse(null);

            var title = buf.readText();
            var description = buf.readBoolean() ? buf.readText() : null;
            var pinnedTaskId = buf.readBoolean() ? buf.readString() : null;

            var taskCount = buf.readInt();
            var tasks = new ArrayList<QuestBookTask>(taskCount);
            for (var i = 0; i < taskCount; i++) {
                var taskId = buf.readString();
                var taskTitle = buf.readText();
                var taskDescription = buf.readBoolean() ? buf.readText() : null;
                var isGradual = buf.readBoolean();
                var completionLevel = buf.readFloat();
                var completionStatus = buf.readBoolean() ? CompletionStatus.values()[buf.readByte()] : null;
                int buttonMask = buf.readByte() & 0xFF;
                var buttons = EnumSet.noneOf(TaskButton.class);
                if ((buttonMask & 1) != 0) buttons.add(TaskButton.SUCCESS);
                if ((buttonMask & 2) != 0) buttons.add(TaskButton.FAILURE);
                if ((buttonMask & 4) != 0) buttons.add(TaskButton.SKIP);
                tasks.add(new QuestBookTask(taskId, taskTitle, taskDescription, isGradual, completionLevel, completionStatus, buttons));
            }

            return new GetQuestDetailResponse(new QuestBookQuest(title, description, tasks, pinnedTaskId));
        }
    };

    public static final ClientRequest<GetQuestDetailsRequest, GetQuestDetailResponse> INSTANCE =
            new ClientRequest<>(
                    Identifier.of(TravelcoreQuesting.MOD_ID, "get-quest-details-rq"),
                    Identifier.of(TravelcoreQuesting.MOD_ID, "get-quest-details-rs"),
                    REQUEST_ENCODER,
                    RESPONSE_ENCODER,
                    GetQuestDetailsClientRequest::handle
            );

    /**
     * Регистрирует обработчик запроса в {@link ClientRequests}.
     * Должен вызываться как на сервере ({@link ClientRequest#registerServer()}),
     * так и на клиенте ({@link ClientRequest#registerClient()}).
     */
    public static void register() {
        ClientRequests.register(GetQuestDetailsRequest.class, INSTANCE);
    }

    /**
     * Регистрирует серверный хук отключения игрока — очищает его записи из {@link #lastRequestTimes}.
     * Должен вызываться при серверной инициализации.
     */
    public static void registerServerCleanup() {
        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> lastRequestTimes.remove(handler.player.getUuid()));
    }

    // -------------------------------------------------------------------------
    // Серверный обработчик
    // -------------------------------------------------------------------------

    /**
     * Обрабатывает запрос на сервере: проверяет принадлежность квеста игроку, применяет
     * per-quest rate limit, строит {@link QuestBookQuest}.
     *
     * @param server  сервер
     * @param player  игрок, отправивший запрос
     * @param request запрос
     * @return ответ с данными или {@code null} внутри при отказе rate limit / квест не выдан
     */
    private static GetQuestDetailResponse handle(
            net.minecraft.server.MinecraftServer server,
            net.minecraft.server.network.ServerPlayerEntity player,
            GetQuestDetailsRequest request
    ) {
        var questManager = ServerQuestManagerContainer.getQuestManager(server);

        // Квест должен быть выдан игроку (active или complete) — отсекает несуществующие/чужие id
        // до любой записи в rate-limit Map, защищая от раздувания Map чередой невалидных запросов
        if (!questManager.isQuestTracked(request.questId(), player)) {
            return new GetQuestDetailResponse(null);
        }

        var playerMap = lastRequestTimes.computeIfAbsent(player.getUuid(), k -> new ConcurrentHashMap<>());
        var now = System.currentTimeMillis();
        var last = playerMap.getOrDefault(request.questId(), 0L);
        if (now - last < SERVER_RATE_LIMIT_MS) {
            return new GetQuestDetailResponse(null);
        }
        playerMap.put(request.questId(), now);

        var resolver = questManager.getQuestResolver();

        // Страховка от рассинхрона датапака: квест выдан, но уже удалён из репозитория
        var quest = resolver.getQuest(request.questId());
        if (quest == null) return new GetQuestDetailResponse(null);

        questManager.markQuestViewed(request.questId(), player);

        var stageOpt = questManager.getActiveStage(request.questId(), player);

        // Нет активного этапа (квест завершён или ещё не начат) — задачи не отправляем
        if (stageOpt.isEmpty()) {
            return new GetQuestDetailResponse(
                    new QuestBookQuest(quest.title(), quest.description(), List.of(), null)
            );
        }

        var tasks = quest.getStage(stageOpt.get()).stream()
                .map(taskId -> buildTaskDetail(questManager, resolver, request.questId(), taskId, player))
                .toList();

        var pinnedTaskId = questManager.getPinnedTaskId(request.questId(), player).orElse(null);

        return new GetQuestDetailResponse(new QuestBookQuest(quest.title(), quest.description(), tasks, pinnedTaskId));
    }

    /**
     * Строит {@link QuestBookTask} для одной задачи.
     *
     * @param questManager менеджер квестов
     * @param resolver     резолвер квестов
     * @param questId      идентификатор квеста
     * @param taskId       идентификатор задачи
     * @param player       игрок
     * @return данные задачи
     */
    private static QuestBookTask buildTaskDetail(
            ServerQuestManager questManager,
            QuestResolver resolver,
            Identifier questId,
            String taskId,
            net.minecraft.server.network.ServerPlayerEntity player
    ) {  // TODO: Отправляется только прогресс успеха, а где прогресс провала?
        var task = resolver.getTask(questId, taskId);
        CompletionStatus completionStatus = questManager.getTaskCompletionStatus(questId, taskId, player).orElse(null);

        var successCondition = task != null ? task.successCondition() : null;
        var isGradual = successCondition != null && successCondition.isGradual();

        float completionLevel;
        if (isGradual) {
            var current = questManager.getTaskSuccessCompletion(questId, taskId, player);
            var target = questManager.getTaskSuccessTarget(questId, taskId, player);
            completionLevel = target > 0 ? (float) current / target : 0f;
        } else {
            completionLevel = completionStatus != null ? 1f : 0f;
        }

        var title = task != null ? task.title() : Text.literal(taskId);
        var description = task != null ? task.description() : null;
        var buttons = task != null ? task.buttons() : EnumSet.noneOf(TaskButton.class);

        return new QuestBookTask(taskId, title, description, isGradual, completionLevel, completionStatus, buttons);
    }
}

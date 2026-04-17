package k4k.travelcorequesting.infra.requests;

import k4k.travelcorequesting.TravelcoreQuesting;
import k4k.travelcorequesting.common.requests.ClientRequest;
import k4k.travelcorequesting.common.requests.ClientRequests;
import k4k.travelcorequesting.common.requests.IPacketEncoder;
import k4k.travelcorequesting.domain.enums.CompletionStatus;
import k4k.travelcorequesting.questing.abstractions.QuestResolver;
import k4k.travelcorequesting.questing.abstractions.ServerQuestManagerContainer;
import k4k.travelcorequesting.questing.models.QuestBookQuest;
import k4k.travelcorequesting.questing.models.QuestBookTask;
import k4k.travelcorequesting.questing.services.ServerQuestManager;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Инфраструктура C2S запроса деталей квеста.
 *
 * <p>Клиент отправляет {@link GetQuestDetailsRequest}, сервер отвечает {@link GetQuestDetailResponse}.
 * Сервер применяет rate limit {@value SERVER_RATE_LIMIT_MS} мс на игрока — защита от спама с
 * модифицированного клиента.
 */
public class GetQuestDetailsClientRequest {

    /** Минимальный интервал между запросами от одного игрока на сервере, мс. */
    public static final long SERVER_RATE_LIMIT_MS = 500L;

    /** Последние временные метки запросов от каждого игрока. Используется для rate limiting. */
    private static final Map<UUID, Long> lastRequestTimes = new ConcurrentHashMap<>();

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
                tasks.add(new QuestBookTask(taskId, taskTitle, taskDescription, isGradual, completionLevel, completionStatus));
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

    // -------------------------------------------------------------------------
    // Серверный обработчик
    // -------------------------------------------------------------------------

    /**
     * Обрабатывает запрос на сервере: применяет rate limit, строит {@link QuestBookQuest}.
     *
     * @param server  сервер
     * @param player  игрок, отправивший запрос
     * @param request запрос
     * @return ответ с данными или {@code null} внутри при rate limit / отсутствии квеста
     */
    private static GetQuestDetailResponse handle(
            net.minecraft.server.MinecraftServer server,
            net.minecraft.server.network.ServerPlayerEntity player,
            GetQuestDetailsRequest request
    ) {
        var now = System.currentTimeMillis();
        var last = lastRequestTimes.getOrDefault(player.getUuid(), 0L);
        if (now - last < SERVER_RATE_LIMIT_MS) {
            return new GetQuestDetailResponse(null);
        }
        lastRequestTimes.put(player.getUuid(), now);

        var questManager = ServerQuestManagerContainer.getQuestManager(server);
        var resolver = questManager.getQuestResolver();

        var quest = resolver.getQuest(request.questId());
        if (quest == null) return new GetQuestDetailResponse(null);

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
            var target = questManager.getTaskSuccessTarget(questId, taskId);
            completionLevel = target > 0 ? (float) current / target : 0f;
        } else {
            completionLevel = completionStatus != null ? 1f : 0f;
        }

        var title = task != null ? task.title() : Text.literal(taskId);
        var description = task != null ? task.description() : null;

        return new QuestBookTask(taskId, title, description, isGradual, completionLevel, completionStatus);
    }
}

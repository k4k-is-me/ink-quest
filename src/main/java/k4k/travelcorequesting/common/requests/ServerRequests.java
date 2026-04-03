package k4k.travelcorequesting.common.requests;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Медиатор запросов. Позволяет при отправке запроса автоматически найти менеджер по переданному объекту.
 */
public class ServerRequests {
    private static final Map<Class<?>, ServerRequest<?, ?>> requestMapping = new HashMap<>();

    /**
     * Регистрирует менеджер запросов.
     * @param requestClass Класс модели данных запроса
     * @param requestSender Менеджер запроса
     * @param <Rs> Тип модели данных ответа
     * @param <Rq> Тип модели данных запроса
     */
    public static <Rq extends IRequest<Rs>, Rs extends IResponse> void register(
            Class<Rq> requestClass,
            ServerRequest<Rq, Rs> requestSender
    ) {
        requestMapping.put(requestClass, requestSender);
    }

    /**
     * Отправляет запрос.
     * @param request Объект запроса к отправке
     * @return CompletableFuture ответа
     * @param <Rs> Тип модели данных ответа
     * @param <Rq> Тип модели данных запроса
     */
    @SuppressWarnings("unchecked")
    public static <Rq extends IRequest<Rs>, Rs extends IResponse> CompletableFuture<Rs> send(ServerPlayerEntity player, Rq request) {
        var requestClass = request.getClass();
        var sender = requestMapping.get(requestClass);

        if (sender == null)
            throw new IllegalArgumentException("No ClientRequest registered for request type: " + requestClass.getName());

        var typedSender = (ServerRequest<Rq, Rs>) sender;
        return typedSender.send(player, request);
    }
}

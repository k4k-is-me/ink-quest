package k4k.travelcorequesting.common.requests;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Менеджер запросов, отправляемых клиентом серверу.
 * @param <Rs> Модель данных ответа
 * @param <Rq> Модель данных запроса
 */
public class ClientRequest<Rq extends IRequest<Rs>, Rs extends IResponse> {
    private final Map<Integer, CompletableFuture<Rs>> pendingRequests = new ConcurrentHashMap<>();
    private int nextRequestId = 0;

    private final Identifier requestPacketId;
    private final Identifier responsePacketId;
    private final IPacketEncoder<Rq> requestEncoder;
    private final IPacketEncoder<Rs> responseEncoder;
    private final Handler<Rq, Rs> handler;

    public ClientRequest(
            Identifier requestPacketId,
            Identifier responsePacketId,
            IPacketEncoder<Rq> requestEncoder,
            IPacketEncoder<Rs> responseEncoder,
            Handler<Rq, Rs> handler
    ) {
        this.requestPacketId = requestPacketId;
        this.responsePacketId = responsePacketId;
        this.requestEncoder = requestEncoder;
        this.responseEncoder = responseEncoder;
        this.handler = handler;
    }

    /**
     * Отправляет запрос серверу, возвращает CompletableFuture, в который при завершении передаётся ответ
     * @param requestData Объект запроса для отправки
     * @return CompletableFuture ответа
     */
    public CompletableFuture<Rs> send(Rq requestData) {
        int requestId = nextRequestId++;
        var future = new CompletableFuture<Rs>();

        pendingRequests.put(requestId, future);

        var buf = PacketByteBufs.create();
        buf.writeInt(requestId);
        requestEncoder.encode(requestData, buf);

        ClientPlayNetworking.send(requestPacketId, buf);
        return future;
    }

    /**
     * Регистрация обработчика запроса на стороне сервера
     */
    public void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(requestPacketId,
                (server, player, handler, buf, responseSender) -> {
                    int requestId = buf.readInt();
                    var request = requestEncoder.decode(buf);

                    var response = this.handler.handle(server, player, request);

                    var rsBuf = PacketByteBufs.create();
                    rsBuf.writeInt(requestId);
                    responseEncoder.encode(response, rsBuf);

                    responseSender.sendPacket(responsePacketId, rsBuf);
                });
    }

    /**
     * Регистрация обработчика запроса на стороне клиента
     */
    public void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(responsePacketId,
                (client, handler, buf, responseSender) -> {
                    int requestId = buf.readInt();

                    CompletableFuture<Rs> future = pendingRequests.remove(requestId);
                    if (future == null) return;

                    var response = responseEncoder.decode(buf);

                    client.execute(() -> future.complete(response));
                });
    }

    @FunctionalInterface
    public interface Handler<Rq, Rs> {
        Rs handle(MinecraftServer server, ServerPlayerEntity player, Rq request);
    }
}

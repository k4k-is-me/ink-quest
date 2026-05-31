package k4k.inkquest.client.notifications;

import k4k.inkquest.client.huds.QuestHudOverlay;
import k4k.inkquest.client.screens.QuestBookQuestsScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Координатор уведомлений о новых квестах.
 *
 * <p>Реагирует на три сигнала (пакеты):
 * <ul>
 *   <li>{@link #onQuestEnteredBook} — квест попал в книгу игрока;</li>
 *   <li>{@link #onQuestPinned} — квест закреплён в HUD;</li>
 *   <li>{@link #onBookOpened} — книга квестов открылась.</li>
 * </ul>
 *
 * <p>Накапливает выданные квесты в settle-окне {@link #SETTLE_TICKS}. После окна
 * показывает {@link NewQuestNotificationWidget} через {@link QuestHudOverlay}.
 * Новое окно открывается только после того, как предыдущий виджет полностью угас
 * ({@link QuestHudOverlay#hasNotification()} вернул {@code false}).
 *
 * <p>Создаётся на {@code ClientPlayConnectionEvents.JOIN} и обнуляется на
 * {@code DISCONNECT} — по образцу {@code QUEST_HUD_OVERLAY}.
 */
public class NewQuestNotificationManager {
    private static final int SETTLE_TICKS = 60;

    private final QuestHudOverlay overlay;

    private final Set<Identifier> pendingQuests = new HashSet<>();
    private long settleDeadlineTick = -1;
    private long tickCounter = 0;

    private @Nullable Identifier activeTargetQuestId = null;
    /** Предыдущее значение hasNotification — для обнуления target при естественном угасании. */
    private boolean prevHasNotification = false;

    /**
     * @param overlay overlay HUD; оба объекта создаются на JOIN и разделяют время жизни
     */
    public NewQuestNotificationManager(QuestHudOverlay overlay) {
        this.overlay = overlay;
    }

    /**
     * Вызывается при получении {@code QuestBookQuestListItemAddedS2CPacket}.
     * Если книга открыта — игнорируем (игрок видит квест вживую).
     */
    public void onQuestEnteredBook(Identifier questId) {
        if (MinecraftClient.getInstance().currentScreen instanceof QuestBookQuestsScreen) return;
        pendingQuests.add(questId);
    }

    /**
     * Вызывается при получении {@code HudSetQuestStageS2CPacket} (квест закреплён в HUD).
     * Снимает квест из pending — уведомление о нём не нужно, он уже виден в HUD.
     * Влияет только на ещё не показанные квесты (те, что в pending).
     */
    public void onQuestPinned(Identifier questId) {
        pendingQuests.remove(questId);
    }

    /**
     * Вызывается при получении {@code QuestBookOpenAtQuestS2CPacket} (книга открылась).
     * Полный сброс: очищает pending, отменяет settle-окно и гасит активный виджет.
     */
    public void onBookOpened() {
        pendingQuests.clear();
        settleDeadlineTick = -1;
        overlay.dismissNotification();
        activeTargetQuestId = null;
        prevHasNotification = false;
    }

    /**
     * Тик-метод. Вызывается из {@code ClientTickEvents.END_CLIENT_TICK}.
     *
     * <p>Гейтинг: пока overlay показывает виджет ({@link QuestHudOverlay#hasNotification()}),
     * settle-окно не запускается — ждём полного угасания виджета. Это гарантирует
     * плавный цикл «показ → угасание → settle → показ» без рывков и наложений.
     */
    public void tick() {
        tickCounter++;

        boolean hasNotification = overlay.hasNotification();

        if (prevHasNotification && !hasNotification) {
            activeTargetQuestId = null;
        }
        prevHasNotification = hasNotification;

        if (hasNotification) return;

        if (pendingQuests.isEmpty()) {
            settleDeadlineTick = -1;
            return;
        }

        if (settleDeadlineTick == -1) {
            settleDeadlineTick = tickCounter + SETTLE_TICKS;
        } else if (tickCounter >= settleDeadlineTick) {
            int n = pendingQuests.size();
            Identifier target = n == 1 ? pendingQuests.iterator().next() : null;
            overlay.showNotification(new NewQuestNotificationWidget(n));
            activeTargetQuestId = target;
            pendingQuests.clear();
            settleDeadlineTick = -1;
        }
    }

    /**
     * Возвращает цель для открытия книги на конкретном квесте (при N==1) и обнуляет её.
     * Одноразовое использование: следующий вызов вернёт {@code null}.
     *
     * @return идентификатор квеста или {@code null}, если уведомление показывало N > 1
     */
    public @Nullable Identifier consumeActiveTarget() {
        Identifier target = activeTargetQuestId;
        activeTargetQuestId = null;
        return target;
    }
}

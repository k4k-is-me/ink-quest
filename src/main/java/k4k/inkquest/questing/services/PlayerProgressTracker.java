package k4k.inkquest.questing.services;

import k4k.inkquest.domain.enums.CompletionStatus;
import k4k.inkquest.questing.abstractions.QuestResolver;
import k4k.inkquest.questing.states.PlayerTrackerState;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PlayerProgressTracker {
    private final QuestResolver resolver;

    private final Map<Identifier, QuestProgressTracker> activeQuests = new HashMap<>();
    private final Map<Identifier, CompletionStatus> completedQuests = new HashMap<>();

    public PlayerProgressTracker(QuestResolver resolver) {
        this.resolver = resolver;
    }

    public static PlayerProgressTracker create(QuestResolver resolver, PlayerTrackerState state) {
        var tracker = new PlayerProgressTracker(resolver);

        for (var questTrackerStateEntry : state.activeQuestsTrackers().entrySet()) {
            var questTracker = QuestProgressTracker.create(questTrackerStateEntry.getKey(), resolver, questTrackerStateEntry.getValue());
            tracker.activeQuests.put(questTrackerStateEntry.getKey(), questTracker);
        }

        tracker.completedQuests.putAll(state.completedQuests());

        return tracker;
    }

    public PlayerTrackerState saveState() {
        return new PlayerTrackerState(
                Collections.unmodifiableMap(this.activeQuests.entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().saveState()
                ))),
                Collections.unmodifiableMap(this.completedQuests)
        );
    }

    /**
     * Начинает отслеживать прогресс по квесту.
     * Квест трекер может контролировать прогресс и по не загруженным квестам (ну скорее хранить).
     * Один квест может отслеживаться только единожды
     */
    public Optional<QuestProgressTracker> startTracking(Identifier questId) {
        Objects.requireNonNull(questId);

        if (activeQuests.containsKey(questId))
            return Optional.empty();
        if (completedQuests.containsKey(questId))
            return Optional.empty();

        var questTracker = new QuestProgressTracker(questId, this.resolver);
        this.activeQuests.put(questId, questTracker);
        return Optional.of(questTracker);
    }

    /**
     * Завершает отслеживание прогресса по квесту
     * @param questId Идентификатор квеста
     */
    public void stopTracking(Identifier questId) {
        activeQuests.remove(questId);
        completedQuests.remove(questId);
    }

    /**
     * Возвращает трекер активного квеста по идентификатору, если есть такой активный квест
     * @param questId Идентификатор квеста
     * @return Optional
     */
    public Optional<QuestProgressTracker> getQuestTracker(Identifier questId) {
        return Optional.ofNullable(this.activeQuests.get(questId));
    }

    /**
     * Возвращает список идентификаторов всех отслеживаемых квестов, выполненных и нет
     */
    public List<Identifier> getTrackedQuests() {
        return Stream.concat(  // NOTE: sets of active and complete quests should never intersect
                        this.activeQuests.keySet().stream(),
                        this.completedQuests.keySet().stream()
                )
                .toList();
    }

    /**
     * Возвращает список активно отслеживаемых (ещё не выполненных) квестов
     * @return Список активных квестов
     */
    public List<Identifier> getActiveQuests() {
        return List.copyOf(this.activeQuests.keySet());
    }

    /**
     * Возвращает список выполненных квестов
     * @return Список завершённых квестов
     */
    public List<Identifier> getCompleteQuests() {
        return this.completedQuests.keySet().stream().toList();
    }

    /**
     * Возвращает статус выполненного квеста (null если квест не отслеживается или не выполнен)
     * @param questId Идентификатор квеста
     * @return Статус квеста
     */
    public Optional<CompletionStatus> getCompletionStatus(Identifier questId) {
        return Optional.ofNullable(this.completedQuests.get(questId));
    }

    /**
     * Возвращает true если квест отслеживается
     * @param questId Идентификатор квеста
     * @return true, если квест активен
     */
    public boolean isTracked(Identifier questId) {
        return this.activeQuests.containsKey(questId)
                || this.completedQuests.containsKey(questId);
    }

    /**
     * Возвращает true если квест активно отслеживается (отслеживается и ещё не выполнен)
     * @param questId Идентификатор квеста
     * @return true, если квест активен
     */
    public boolean isActive(Identifier questId) {
        return this.activeQuests.containsKey(questId);
    }

    /**
     * Возвращает true если квест выполнен (отслеживается и ещё не выполнен)
     * @param questId Идентификатор квеста
     * @return true, если квест завершён
     */
    public boolean isComplete(Identifier questId) {
        return this.completedQuests.containsKey(questId);
    }

    /**
     * Возвращает true если квест выполнен (отслеживается и ещё не выполнен)
     * @param questId Идентификатор квеста
     * @return true, если квест завершён
     */
    public boolean isComplete(Identifier questId, CompletionStatus status) {
        return this.completedQuests.get(questId) == status;
    }

    /**
     * Проверяет выполненность квеста. Если в результате проверки квест оказался выполненным -
     * помечает квест как таковой и вызывает переданный обработчик, в противном случае ничего не делает.
     * Завершённые квесты перестают быть активными.
     * @param questId Идентификатор квеста
     * @param handler Обработчик завершения
     */
    public void checkCompletion(Identifier questId, QuestCompletionHandler handler) {
        if (this.completedQuests.containsKey(questId)) return;

        var status = this.computeCompletionStatus(questId);
        if (status == null) return;  // quest is not complete yet

        this.activeQuests.remove(questId);
        this.completedQuests.put(questId, status);

        handler.onCompletion(status);
    }


    /**
     * Рассчитывает статус выполнения активного квеста.
     * - Квест считается выполненным успешно, если ВСЕ его обязательные и не пропущенные задачи выполнены успешно;
     * - Квест считается проваленным, если ХОТЯБЫ ОДНА его обязательная задача была провалена;
     * - Квест считается пропущенным, если все его обязательные задачи были пропущены;
     * - Для квестов не содержащих этапов всегда будет возвращаться null.
     * @param questId Идентификатор квеста
     * @return Статус квеста
     */
    private @Nullable CompletionStatus computeCompletionStatus(Identifier questId) {
        var questTracker = this.activeQuests.get(questId);
        if (questTracker == null) return null;

        var quest = this.resolver.getQuest(questId);
        if (quest == null) return null;

        if (quest.getStageCount() == 0)
            return null;

        var allSkipped = true;

        for (var stageIndex = 0; stageIndex < quest.getStageCount(); stageIndex++) {
            var requiredTaskId = quest.getRequiredTask(stageIndex).orElse(null);
            if (requiredTaskId == null) continue;

            var status = questTracker.getCompletionStatus(requiredTaskId)
                    .orElse(null);

            if (Objects.equals(status, CompletionStatus.FAILURE)) {
                return CompletionStatus.FAILURE;
            }

            if (Objects.equals(status, CompletionStatus.SUCCESS)) {
                allSkipped = false;
                continue;
            }

            if (Objects.equals(status, CompletionStatus.SKIPPED)) {
                continue;
            }

            return null;  // Quest is not completed yet
        }

        if (allSkipped)
            return CompletionStatus.SKIPPED;

        return CompletionStatus.SUCCESS;
    }

    @FunctionalInterface
    public interface QuestCompletionHandler {
        void onCompletion(CompletionStatus status);
    }

    // ---

//    public @Nullable String getPinnedTaskId() {
//        return this.pinnedTask;
//    }

//    public Identifier getPinnedQuestId() {
//        return this.pinnedQuest;
//    }
}

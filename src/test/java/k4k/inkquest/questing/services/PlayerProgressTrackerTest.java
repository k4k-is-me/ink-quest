package k4k.inkquest.questing.services;

import k4k.inkquest.domain.enums.CompletionStatus;
import k4k.inkquest.domain.models.MutableQuest;
import k4k.inkquest.domain.models.MutableTask;
import k4k.inkquest.questing.states.PlayerTrackerState;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для {@link PlayerProgressTracker}: добавление и удаление отслеживаемых квестов,
 * проверка статусов, вычисление завершённости квеста (checkCompletion), сериализация.
 */
@SuppressWarnings("SameParameterValue")
class PlayerProgressTrackerTest {

    private QuestRepository repo;

    @BeforeEach
    void setUp() {
        repo = new QuestRepository();
    }

    // ── Вспомогательные методы ────────────────────────────────────────────────

    /** Создать Identifier с тестовым namespace. */
    private static Identifier id(String path) {
        return Identifier.of("test", path);
    }

    /** Создать пустой квест с заданным именем. */
    private static MutableQuest quest(String name) {
        return MutableQuest.create(Text.literal(name));
    }

    /** Создать задачу с заданным именем. */
    private static MutableTask task(String name) {
        return MutableTask.create(Text.literal(name));
    }

    /** Создать трекер игрока. */
    private PlayerProgressTracker tracker() {
        return new PlayerProgressTracker(repo);
    }

    /**
     * Зарегистрировать квест с одним этапом и одной обязательной задачей.
     * @param questId id квеста
     * @param taskId id обязательной задачи
     */
    private void setupSingleStageQuest(Identifier questId, String taskId) {
        var q = quest("Q");
        q.setTask(taskId, task("T"));
        q.addTaskToStage(taskId);
        repo.replaceStaticQuests(Map.of(questId, q));
    }

    /**
     * Зарегистрировать квест без этапов.
     * @param questId id квеста
     */
    private void setupNoStageQuest(Identifier questId) {
        repo.replaceStaticQuests(Map.of(questId, quest("Q")));
    }

    /**
     * Зарегистрировать квест с двумя этапами: t1 (stage 0) и t2 (stage 1).
     * @param questId id квеста
     * @param t1 id обязательной задачи первого этапа
     * @param t2 id обязательной задачи второго этапа
     */
    private void setupTwoStageQuest(Identifier questId, String t1, String t2) {
        var q = quest("Q");
        q.setTask(t1, task("T1"));
        q.addTaskToStage(t1);
        q.setTask(t2, task("T2"));
        q.addTaskToStage(t2);
        repo.replaceStaticQuests(Map.of(questId, q));
    }

    // ── startTracking ─────────────────────────────────────────────────────────

    @Test
    void startTracking_newQuest_returnsTracker() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker();

        var result = tracker.startTracking(questId);

        assertTrue(result.isPresent());
    }

    @Test
    void startTracking_newQuest_appearsInActiveQuests() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker();

        tracker.startTracking(questId);

        assertTrue(tracker.getActiveQuests().contains(questId));
    }

    @Test
    void startTracking_alreadyActive_returnsEmpty() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker();

        tracker.startTracking(questId);
        var result = tracker.startTracking(questId);

        assertTrue(result.isEmpty());
    }

    @Test
    void startTracking_alreadyCompleted_returnsEmpty() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker();

        var questTracker = tracker.startTracking(questId).orElseThrow();
        questTracker.complete("t1", CompletionStatus.SUCCESS);
        tracker.checkCompletion(questId, status -> {});

        var result = tracker.startTracking(questId);

        assertTrue(result.isEmpty());
    }

    // ── stopTracking ──────────────────────────────────────────────────────────

    @Test
    void stopTracking_activeQuest_removedFromActive() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker();

        tracker.startTracking(questId);
        tracker.stopTracking(questId);

        assertFalse(tracker.isActive(questId));
        assertFalse(tracker.isTracked(questId));
    }

    @Test
    void stopTracking_completedQuest_removedFromComplete() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker();

        var questTracker = tracker.startTracking(questId).orElseThrow();
        questTracker.complete("t1", CompletionStatus.SUCCESS);
        tracker.checkCompletion(questId, status -> {});
        tracker.stopTracking(questId);

        assertFalse(tracker.isComplete(questId));
        assertFalse(tracker.isTracked(questId));
    }

    // ── getQuestTracker ───────────────────────────────────────────────────────

    @Test
    void getQuestTracker_activeQuest_returnsTracker() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker();

        tracker.startTracking(questId);

        assertTrue(tracker.getQuestTracker(questId).isPresent());
    }

    @Test
    void getQuestTracker_unknownQuest_returnsEmpty() {
        var tracker = tracker();

        assertTrue(tracker.getQuestTracker(id("ghost")).isEmpty());
    }

    // ── getTrackedQuests / getActiveQuests / getCompleteQuests ─────────────────

    @Test
    void getTrackedQuests_afterStartTracking_containsQuest() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker();

        tracker.startTracking(questId);

        assertTrue(tracker.getTrackedQuests().contains(questId));
    }

    @Test
    void getActiveQuests_afterStartTracking_containsQuest() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker();

        tracker.startTracking(questId);

        assertTrue(tracker.getActiveQuests().contains(questId));
        assertFalse(tracker.getCompleteQuests().contains(questId));
    }

    @Test
    void getCompleteQuests_afterCheckCompletion_containsQuest() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker();

        var questTracker = tracker.startTracking(questId).orElseThrow();
        questTracker.complete("t1", CompletionStatus.SUCCESS);
        tracker.checkCompletion(questId, status -> {});

        assertTrue(tracker.getCompleteQuests().contains(questId));
        assertFalse(tracker.getActiveQuests().contains(questId));
    }

    // ── isTracked / isActive / isComplete ─────────────────────────────────────

    @Test
    void isTracked_untracked_returnsFalse() {
        var tracker = tracker();

        assertFalse(tracker.isTracked(id("q")));
    }

    @Test
    void isActive_afterStartTracking_returnsTrue() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker();

        tracker.startTracking(questId);

        assertTrue(tracker.isTracked(questId));
        assertTrue(tracker.isActive(questId));
        assertFalse(tracker.isComplete(questId));
    }

    @Test
    void isComplete_afterCheckCompletion_returnsTrue() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker();

        var questTracker = tracker.startTracking(questId).orElseThrow();
        questTracker.complete("t1", CompletionStatus.SUCCESS);
        tracker.checkCompletion(questId, status -> {});

        assertTrue(tracker.isTracked(questId));
        assertFalse(tracker.isActive(questId));
        assertTrue(tracker.isComplete(questId));
    }

    // ── getCompletionStatus ───────────────────────────────────────────────────

    @Test
    void getCompletionStatus_completedQuest_returnsStatus() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker();

        var questTracker = tracker.startTracking(questId).orElseThrow();
        questTracker.complete("t1", CompletionStatus.SUCCESS);
        tracker.checkCompletion(questId, status -> {});

        assertEquals(CompletionStatus.SUCCESS, tracker.getCompletionStatus(questId).orElseThrow());
    }

    @Test
    void getCompletionStatus_activeQuest_returnsEmpty() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker();

        tracker.startTracking(questId);

        assertTrue(tracker.getCompletionStatus(questId).isEmpty());
    }

    // ── checkCompletion ───────────────────────────────────────────────────────

    @Test
    void checkCompletion_questNotInRepo_handlerNotCalled() {
        var tracker = tracker();
        tracker.startTracking(id("ghost"));

        var called = new boolean[]{false};
        tracker.checkCompletion(id("ghost"), status -> called[0] = true);

        assertFalse(called[0]);
    }

    @Test
    void checkCompletion_noStages_handlerNotCalled() {
        var questId = id("q");
        setupNoStageQuest(questId);
        var tracker = tracker();

        tracker.startTracking(questId);

        var called = new boolean[]{false};
        tracker.checkCompletion(questId, status -> called[0] = true);

        assertFalse(called[0]);
    }

    @Test
    void checkCompletion_requiredTaskNotComplete_handlerNotCalled() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker();

        tracker.startTracking(questId);

        var called = new boolean[]{false};
        tracker.checkCompletion(questId, status -> called[0] = true);

        assertFalse(called[0]);
    }

    @Test
    void checkCompletion_alreadyCompleted_handlerNotCalledAgain() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker();

        var questTracker = tracker.startTracking(questId).orElseThrow();
        questTracker.complete("t1", CompletionStatus.SUCCESS);
        tracker.checkCompletion(questId, status -> {}); // первый вызов — завершает

        var called = new boolean[]{false};
        tracker.checkCompletion(questId, status -> called[0] = true); // второй — нет

        assertFalse(called[0]);
    }

    @Test
    void checkCompletion_requiredTaskFailure_handlerCalledWithFailure() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker();

        var questTracker = tracker.startTracking(questId).orElseThrow();
        questTracker.complete("t1", CompletionStatus.FAILURE);

        var received = new CompletionStatus[]{null};
        tracker.checkCompletion(questId, status -> received[0] = status);

        assertEquals(CompletionStatus.FAILURE, received[0]);
    }

    @Test
    void checkCompletion_allRequiredSuccess_handlerCalledWithSuccess() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker();

        var questTracker = tracker.startTracking(questId).orElseThrow();
        questTracker.complete("t1", CompletionStatus.SUCCESS);

        var received = new CompletionStatus[]{null};
        tracker.checkCompletion(questId, status -> received[0] = status);

        assertEquals(CompletionStatus.SUCCESS, received[0]);
    }

    @Test
    void checkCompletion_allRequiredSkipped_handlerCalledWithSuccess() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker();

        var questTracker = tracker.startTracking(questId).orElseThrow();
        questTracker.complete("t1", CompletionStatus.SKIPPED);

        var received = new CompletionStatus[]{null};
        tracker.checkCompletion(questId, status -> received[0] = status);

        assertEquals(CompletionStatus.SUCCESS, received[0]);
    }

    @Test
    void checkCompletion_mixSkippedAndSuccess_handlerCalledWithSuccess() {
        var questId = id("q");
        setupTwoStageQuest(questId, "t1", "t2");
        var tracker = tracker();

        var questTracker = tracker.startTracking(questId).orElseThrow();
        questTracker.complete("t1", CompletionStatus.SKIPPED);
        questTracker.complete("t2", CompletionStatus.SUCCESS);

        var received = new CompletionStatus[]{null};
        tracker.checkCompletion(questId, status -> received[0] = status);

        assertEquals(CompletionStatus.SUCCESS, received[0]);
    }

    @Test
    void checkCompletion_twoStages_firstSuccessSecondIncomplete_handlerNotCalled() {
        var questId = id("q");
        setupTwoStageQuest(questId, "t1", "t2");
        var tracker = tracker();

        var questTracker = tracker.startTracking(questId).orElseThrow();
        questTracker.complete("t1", CompletionStatus.SUCCESS);
        // t2 не завершена

        var called = new boolean[]{false};
        tracker.checkCompletion(questId, status -> called[0] = true);

        assertFalse(called[0]);
    }

    @Test
    void checkCompletion_twoStages_firstSuccessSecondFailure_handlerCalledWithFailure() {
        var questId = id("q");
        setupTwoStageQuest(questId, "t1", "t2");
        var tracker = tracker();

        var questTracker = tracker.startTracking(questId).orElseThrow();
        questTracker.complete("t1", CompletionStatus.SUCCESS);
        questTracker.complete("t2", CompletionStatus.FAILURE);

        var received = new CompletionStatus[]{null};
        tracker.checkCompletion(questId, status -> received[0] = status);

        assertEquals(CompletionStatus.FAILURE, received[0]);
    }

    @Test
    void checkCompletion_afterCompletion_questMovedToCompleteNotActive() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker();

        var questTracker = tracker.startTracking(questId).orElseThrow();
        questTracker.complete("t1", CompletionStatus.SUCCESS);
        tracker.checkCompletion(questId, status -> {});

        assertFalse(tracker.isActive(questId));
        assertTrue(tracker.isComplete(questId));
    }

    // ── isComplete(questId, status) ───────────────────────────────────────────

    @Test
    void isComplete_withMatchingStatus_returnsTrue() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker();

        var questTracker = tracker.startTracking(questId).orElseThrow();
        questTracker.complete("t1", CompletionStatus.FAILURE);
        tracker.checkCompletion(questId, status -> {});

        assertTrue(tracker.isComplete(questId, CompletionStatus.FAILURE));
    }

    @Test
    void isComplete_withDifferentStatus_returnsFalse() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker();

        var questTracker = tracker.startTracking(questId).orElseThrow();
        questTracker.complete("t1", CompletionStatus.SUCCESS);
        tracker.checkCompletion(questId, status -> {});

        assertFalse(tracker.isComplete(questId, CompletionStatus.FAILURE));
    }

    // ── saveState / create (round-trip) ───────────────────────────────────────

    @Test
    void saveState_and_create_restoresActiveAndCompletedQuests() {
        var activeId = id("active");
        var completeId = id("complete");
        setupSingleStageQuest(activeId, "t1");
        setupSingleStageQuest(completeId, "t2");
        var tracker = tracker();

        tracker.startTracking(activeId);
        var questTracker = tracker.startTracking(completeId).orElseThrow();
        questTracker.complete("t2", CompletionStatus.SUCCESS);
        tracker.checkCompletion(completeId, status -> {});

        PlayerTrackerState state = tracker.saveState();
        var restored = PlayerProgressTracker.create(repo, state);

        assertTrue(restored.isActive(activeId));
        assertTrue(restored.isComplete(completeId));
        assertEquals(CompletionStatus.SUCCESS, restored.getCompletionStatus(completeId).orElseThrow());
    }

    @Test
    void saveState_emptyTracker_createsEmptyTracker() {
        var tracker = tracker();

        PlayerTrackerState state = tracker.saveState();
        var restored = PlayerProgressTracker.create(repo, state);

        assertTrue(restored.getTrackedQuests().isEmpty());
    }
}

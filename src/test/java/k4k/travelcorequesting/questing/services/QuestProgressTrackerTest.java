package k4k.travelcorequesting.questing.services;

import k4k.travelcorequesting.domain.enums.CompletionStatus;
import k4k.travelcorequesting.domain.models.MutableQuest;
import k4k.travelcorequesting.domain.models.MutableTask;
import k4k.travelcorequesting.questing.states.QuestTrackerState;
import k4k.travelcorequesting.questing.states.TaskTrackerState;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для {@link QuestProgressTracker}: хранение активного этапа, активных задач,
 * завершение задач, вычисление активного этапа, закрепление задач, сериализация.
 */
@SuppressWarnings("SameParameterValue")
class QuestProgressTrackerTest {

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

    /**
     * Создать квест с одним этапом и одной обязательной задачей.
     * @param questId id квеста
     * @param taskId id задачи
     */
    private void setupSingleStageQuest(Identifier questId, String taskId) {
        var q = quest("Q");
        q.setTask(taskId, task("T"));
        q.addTaskToStage(taskId);
        repo.replaceStaticQuests(Map.of(questId, q));
    }

    /**
     * Создать квест без этапов, с одной задачей в unusedTasks.
     * @param questId id квеста
     * @param taskId id задачи (добавлена в квест, но не привязана ни к одному этапу)
     */
    private void setupNoStageQuest(Identifier questId, String taskId) {
        var q = quest("Q");
        q.setTask(taskId, task("T"));
        repo.replaceStaticQuests(Map.of(questId, q));
    }

    /**
     * Создать квест с двумя этапами: t1 (stage 0) и t2 (stage 1).
     */
    private void setupTwoStageQuest(Identifier questId, String t1, String t2) {
        var q = quest("Q");
        q.setTask(t1, task("T1"));
        q.addTaskToStage(t1);
        q.setTask(t2, task("T2"));
        q.addTaskToStage(t2);
        repo.replaceStaticQuests(Map.of(questId, q));
    }

    /** Создать трекер для квеста. */
    private QuestProgressTracker tracker(Identifier questId) {
        return new QuestProgressTracker(questId, repo);
    }

    /** Провайдер начального прогресса, всегда возвращающий 0. */
    private static final QuestProgressTracker.InitialProgressProvider ZERO_PROVIDER = (taskId, task) -> 0;

    // ── Начальное состояние ───────────────────────────────────────────────────

    @Test
    void initialActiveStage_isEmpty() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker(questId);

        assertTrue(tracker.getActiveStage().isEmpty());
    }

    @Test
    void initialActiveTasks_isEmpty() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker(questId);

        assertTrue(tracker.getActiveTasks().isEmpty());
    }

    @Test
    void initialIsPinned_returnsFalse() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker(questId);

        assertFalse(tracker.isPinned());
    }

    // ── recomputeActiveStage ──────────────────────────────────────────────────

    @Test
    void recomputeActiveStage_freshSingleStageQuest_setsStageZero() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker(questId);

        tracker.recomputeActiveStage(stage -> {});

        assertEquals(0, tracker.getActiveStage().orElseThrow());
    }

    @Test
    void recomputeActiveStage_stageChanged_returnsTrue() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker(questId);

        assertTrue(tracker.recomputeActiveStage(stage -> {}));
    }

    @Test
    void recomputeActiveStage_sameStage_returnsFalse() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker(questId);

        tracker.recomputeActiveStage(stage -> {}); // stage = 0 (first call)
        assertFalse(tracker.recomputeActiveStage(stage -> {})); // stage = 0 (second call, same)
    }

    @Test
    void recomputeActiveStage_stageChanged_callsHandlerWithNewStage() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker(questId);

        var received = new Integer[]{-1};
        tracker.recomputeActiveStage(stage -> received[0] = stage);

        assertEquals(0, received[0]);
    }

    @Test
    void recomputeActiveStage_requiredTaskSuccess_advancesToNextStage() {
        var questId = id("q");
        setupTwoStageQuest(questId, "t1", "t2");
        var tracker = tracker(questId);

        tracker.recomputeActiveStage(stage -> {}); // stage = 0
        tracker.complete("t1", CompletionStatus.SUCCESS);
        tracker.recomputeActiveStage(stage -> {});

        assertEquals(1, tracker.getActiveStage().orElseThrow());
    }

    @Test
    void recomputeActiveStage_requiredTaskFailure_setsNull() {
        var questId = id("q");
        setupTwoStageQuest(questId, "t1", "t2");
        var tracker = tracker(questId);

        tracker.recomputeActiveStage(stage -> {}); // stage = 0
        tracker.complete("t1", CompletionStatus.FAILURE);
        tracker.recomputeActiveStage(stage -> {});

        assertTrue(tracker.getActiveStage().isEmpty());
    }

    @Test
    void recomputeActiveStage_allStagesComplete_setsNull() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker(questId);

        tracker.recomputeActiveStage(stage -> {}); // stage = 0
        tracker.complete("t1", CompletionStatus.SUCCESS);
        tracker.recomputeActiveStage(stage -> {});

        assertTrue(tracker.getActiveStage().isEmpty());
    }

    @Test
    void recomputeActiveStage_questNotFound_setsNull() {
        // Квест не зарегистрирован в репозитории
        var tracker = tracker(id("ghost"));

        tracker.recomputeActiveStage(stage -> {});

        assertTrue(tracker.getActiveStage().isEmpty());
    }

    // ── complete ──────────────────────────────────────────────────────────────

    @Test
    void complete_marksTaskComplete() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker(questId);

        tracker.complete("t1", CompletionStatus.SUCCESS);

        assertTrue(tracker.isComplete("t1"));
    }

    @Test
    void complete_removesTaskFromActiveTasks() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker(questId);

        tracker.recomputeActiveStage(stage -> {});
        tracker.loadActiveStage(ZERO_PROVIDER, ZERO_PROVIDER);
        tracker.complete("t1", CompletionStatus.SUCCESS);

        assertFalse(tracker.getActiveTasks().contains("t1"));
    }

    @Test
    void complete_nonExistentTask_ignored() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker(questId);

        // Не бросает исключение, задача просто игнорируется
        assertDoesNotThrow(() -> tracker.complete("ghost", CompletionStatus.SUCCESS));
        assertFalse(tracker.isComplete("ghost"));
    }

    // ── isComplete / getCompletionStatus ──────────────────────────────────────

    @Test
    void isComplete_completedTask_returnsTrue() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker(questId);

        tracker.complete("t1", CompletionStatus.SUCCESS);

        assertTrue(tracker.isComplete("t1"));
    }

    @Test
    void isComplete_incompleteTask_returnsFalse() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker(questId);

        assertFalse(tracker.isComplete("t1"));
    }

    @Test
    void isComplete_withMatchingStatus_returnsTrue() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker(questId);

        tracker.complete("t1", CompletionStatus.FAILURE);

        assertTrue(tracker.isComplete("t1", CompletionStatus.FAILURE));
    }

    @Test
    void isComplete_withDifferentStatus_returnsFalse() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker(questId);

        tracker.complete("t1", CompletionStatus.SUCCESS);

        assertFalse(tracker.isComplete("t1", CompletionStatus.FAILURE));
    }

    @Test
    void getCompletionStatus_completedTask_returnsStatus() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker(questId);

        tracker.complete("t1", CompletionStatus.SKIPPED);

        assertEquals(CompletionStatus.SKIPPED, tracker.getCompletionStatus("t1").orElseThrow());
    }

    @Test
    void getCompletionStatus_incompleteTask_returnsEmpty() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker(questId);

        assertTrue(tracker.getCompletionStatus("t1").isEmpty());
    }

    // ── Pin ───────────────────────────────────────────────────────────────────

    @Test
    void isPinned_afterSetPin_returnsTrue() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker(questId);

        tracker.setTaskPin("t1");

        assertTrue(tracker.isPinned());
    }

    @Test
    void isPinned_taskId_matchingTask_returnsTrue() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker(questId);

        tracker.setTaskPin("t1");

        assertTrue(tracker.isPinned("t1"));
    }

    @Test
    void isPinned_taskId_differentTask_returnsFalse() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker(questId);

        tracker.setTaskPin("t1");

        assertFalse(tracker.isPinned("t2"));
    }

    @Test
    void setTaskPin_nonExistentTask_throws() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker(questId);

        assertThrows(IllegalArgumentException.class, () -> tracker.setTaskPin("ghost"));
    }

    @Test
    void resetTaskPin_clearsPinnedTask() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker(questId);

        tracker.setTaskPin("t1");
        tracker.resetTaskPin();

        assertFalse(tracker.isPinned());
        assertTrue(tracker.getTaskPin().isEmpty());
    }

    // ── loadActiveStage ───────────────────────────────────────────────────────

    @Test
    void loadActiveStage_nullStage_clearsActiveTasks() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker(questId);

        // activeStage = null → очистить активные задачи
        tracker.loadActiveStage(ZERO_PROVIDER, ZERO_PROVIDER);

        assertTrue(tracker.getActiveTasks().isEmpty());
    }

    @Test
    void loadActiveStage_loadsTasksFromActiveStage() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker(questId);

        tracker.recomputeActiveStage(stage -> {});
        tracker.loadActiveStage(ZERO_PROVIDER, ZERO_PROVIDER);

        assertTrue(tracker.getActiveTasks().contains("t1"));
        assertTrue(tracker.isActive("t1"));
    }

    @Test
    void loadActiveStage_completedTask_notLoadedAsActive() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker(questId);

        tracker.recomputeActiveStage(stage -> {});
        tracker.complete("t1", CompletionStatus.SUCCESS);
        tracker.loadActiveStage(ZERO_PROVIDER, ZERO_PROVIDER);

        assertFalse(tracker.isActive("t1"));
    }

    @Test
    void loadActiveStage_alreadyLoadedTask_notReloaded() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker(questId);

        tracker.recomputeActiveStage(stage -> {});
        tracker.loadActiveStage(ZERO_PROVIDER, ZERO_PROVIDER);

        // Изменяем прогресс вручную
        tracker.updateSuccessValue("t1", 42);

        // Повторная загрузка не должна сбросить прогресс
        tracker.loadActiveStage(ZERO_PROVIDER, ZERO_PROVIDER);

        // Если бы задача была перезагружена — updateSuccessValue вернул бы false (значение = 0)
        // Проверяем, что прогресс остался 42: снова пробуем задать 42 — нет изменений
        assertFalse(tracker.updateSuccessValue("t1", 42));
    }

    @Test
    void loadActiveStage_taskNotInCurrentStage_removed() {
        var questId = id("q");
        setupTwoStageQuest(questId, "t1", "t2");
        var tracker = tracker(questId);

        // stage 0: загрузить t1
        tracker.recomputeActiveStage(stage -> {});
        tracker.loadActiveStage(ZERO_PROVIDER, ZERO_PROVIDER);
        assertTrue(tracker.isActive("t1"));

        // stage 1: t1 должна пропасть из активных
        tracker.complete("t1", CompletionStatus.SUCCESS);
        tracker.recomputeActiveStage(stage -> {});
        tracker.loadActiveStage(ZERO_PROVIDER, ZERO_PROVIDER);

        assertFalse(tracker.isActive("t1"));
        assertTrue(tracker.isActive("t2"));
    }

    // ── updateSuccessValue / updateFailureValue ───────────────────────────────

    @Test
    void updateSuccessValue_differentValue_returnsTrue() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker(questId);

        tracker.recomputeActiveStage(stage -> {});
        tracker.loadActiveStage(ZERO_PROVIDER, ZERO_PROVIDER);

        assertTrue(tracker.updateSuccessValue("t1", 10));
    }

    @Test
    void updateSuccessValue_sameValue_returnsFalse() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker(questId);

        tracker.recomputeActiveStage(stage -> {});
        tracker.loadActiveStage(ZERO_PROVIDER, ZERO_PROVIDER); // инициализирует successProgress = 0

        assertFalse(tracker.updateSuccessValue("t1", 0));
    }

    @Test
    void updateSuccessValue_completedTask_returnsFalse() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker(questId);

        tracker.recomputeActiveStage(stage -> {});
        tracker.loadActiveStage(ZERO_PROVIDER, ZERO_PROVIDER);
        tracker.complete("t1", CompletionStatus.SUCCESS);

        assertFalse(tracker.updateSuccessValue("t1", 10));
    }

    @Test
    void updateSuccessValue_inactiveTask_returnsFalse() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker(questId);

        // t1 не загружена — нет активного этапа
        assertFalse(tracker.updateSuccessValue("t1", 10));
    }

    @Test
    void updateFailureValue_differentValue_returnsTrue() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker(questId);

        tracker.recomputeActiveStage(stage -> {});
        tracker.loadActiveStage(ZERO_PROVIDER, ZERO_PROVIDER);

        assertTrue(tracker.updateFailureValue("t1", 5));
    }

    @Test
    void updateFailureValue_sameValue_returnsFalse() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker(questId);

        tracker.recomputeActiveStage(stage -> {});
        tracker.loadActiveStage(ZERO_PROVIDER, ZERO_PROVIDER);

        assertFalse(tracker.updateFailureValue("t1", 0));
    }

    @Test
    void updateFailureValue_completedTask_returnsFalse() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker(questId);

        tracker.recomputeActiveStage(stage -> {});
        tracker.loadActiveStage(ZERO_PROVIDER, ZERO_PROVIDER);
        tracker.complete("t1", CompletionStatus.SUCCESS);

        assertFalse(tracker.updateFailureValue("t1", 5));
    }

    // ── saveState / create (round-trip) ───────────────────────────────────────

    @Test
    void saveState_preservesActiveStage() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker(questId);

        tracker.recomputeActiveStage(stage -> {});
        var state = tracker.saveState();

        assertEquals(0, state.activeStage());
    }

    @Test
    void saveState_preservesCompleteTasks() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");
        var tracker = tracker(questId);

        tracker.complete("t1", CompletionStatus.SUCCESS);
        var state = tracker.saveState();

        assertEquals(CompletionStatus.SUCCESS, state.completeTasks().get("t1"));
    }

    @Test
    void create_fromState_restoresActiveStage() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");

        var state = new QuestTrackerState(1, null, Map.of(), Map.of(), Set.of(), false);
        var tracker = QuestProgressTracker.create(questId, repo, state);

        assertEquals(1, tracker.getActiveStage().orElseThrow());
    }

    @Test
    void create_fromState_restoresCompleteTasks() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");

        var state = new QuestTrackerState(null, null, Map.of("t1", CompletionStatus.FAILURE), Map.of(), Set.of(), false);
        var tracker = QuestProgressTracker.create(questId, repo, state);

        assertTrue(tracker.isComplete("t1", CompletionStatus.FAILURE));
    }

    @Test
    void create_fromState_restoresActiveTasks() {
        var questId = id("q");
        setupSingleStageQuest(questId, "t1");

        var taskState = new TaskTrackerState(7, 3);
        var state = new QuestTrackerState(0, null, Map.of(), Map.of("t1", taskState), Set.of(), false);
        var tracker = QuestProgressTracker.create(questId, repo, state);

        assertTrue(tracker.isActive("t1"));
        // Значение прогресса = 7, поэтому update с 7 не должно давать изменений
        assertFalse(tracker.updateSuccessValue("t1", 7));
        // А с другим значением — должно
        assertTrue(tracker.updateSuccessValue("t1", 8));
    }

    // ── Квест без этапов ─────────────────────────────────────────────────────

    @Test
    void recomputeActiveStage_noStages_returnsFalse() {
        // activeStage изначально null, вычисленный тоже null — изменений нет
        var questId = id("q");
        setupNoStageQuest(questId, "t1");
        var tracker = tracker(questId);

        assertFalse(tracker.recomputeActiveStage(stage -> {}));
    }

    @Test
    void recomputeActiveStage_noStages_activeStageRemainsEmpty() {
        var questId = id("q");
        setupNoStageQuest(questId, "t1");
        var tracker = tracker(questId);

        tracker.recomputeActiveStage(stage -> {});

        assertTrue(tracker.getActiveStage().isEmpty());
    }

    @Test
    void recomputeActiveStage_noStages_handlerNotCalled() {
        var questId = id("q");
        setupNoStageQuest(questId, "t1");
        var tracker = tracker(questId);

        var called = new boolean[]{false};
        tracker.recomputeActiveStage(stage -> called[0] = true);

        assertFalse(called[0]);
    }

    @Test
    void loadActiveStage_noStages_activeTasksEmpty() {
        // activeStage = null → loadActiveStage очищает activeTasks
        var questId = id("q");
        setupNoStageQuest(questId, "t1");
        var tracker = tracker(questId);

        tracker.recomputeActiveStage(stage -> {});
        tracker.loadActiveStage(ZERO_PROVIDER, ZERO_PROVIDER);

        assertTrue(tracker.getActiveTasks().isEmpty());
    }

    @Test
    void complete_unusedTask_marksComplete() {
        // Задача в квесте (unusedTasks), но не привязана к этапу — complete() должен её пометить
        var questId = id("q");
        setupNoStageQuest(questId, "t1");
        var tracker = tracker(questId);

        tracker.complete("t1", CompletionStatus.SUCCESS);

        assertTrue(tracker.isComplete("t1"));
    }
}

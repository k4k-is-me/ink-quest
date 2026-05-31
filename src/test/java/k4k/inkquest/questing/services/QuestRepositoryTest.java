package k4k.inkquest.questing.services;

import k4k.inkquest.domain.models.MutableQuest;
import k4k.inkquest.domain.models.MutableTask;
import k4k.inkquest.questing.enums.QuestSourceType;
import k4k.inkquest.questing.enums.TaskType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для {@link QuestRepository}: хранилище статических и динамических квестов,
 * получение по id, приоритет dynamic над static, управление зависимостями, типы задач.
 */
class QuestRepositoryTest {

    private QuestRepository repo;

    @BeforeEach
    void setUp() {
        repo = new QuestRepository();
    }

    /** Вспомогательный метод — создать Identifier с тестовым namespace. */
    private static Identifier id(String path) {
        return Identifier.of("test", path);
    }

    /** Создать пустой квест с заданным именем — удобная фабрика для тестов. */
    private static MutableQuest quest(String name) {
        return MutableQuest.create(Text.literal(name));
    }

    // ── Static quests ────────────────────────────────────────────────────────

    @Test
    void getQuest_afterReplaceStaticQuests_returnsQuest() {
        var questId = id("q");
        var q = quest("Q");
        repo.replaceStaticQuests(Map.of(questId, q));

        assertEquals(q, repo.getQuest(questId));
    }

    @Test
    void getQuestSourceType_staticQuest_returnsStatic() {
        var questId = id("q");
        repo.replaceStaticQuests(Map.of(questId, quest("Q")));

        assertEquals(QuestSourceType.STATIC, repo.getQuestSourceType(questId));
    }

    @Test
    void getStaticQuestIds_containsLoadedIds() {
        var questId = id("q");
        repo.replaceStaticQuests(Map.of(questId, quest("Q")));

        assertTrue(repo.getStaticQuestIds().contains(questId));
    }

    @Test
    void replaceStaticQuests_clearsOldStaticQuests() {
        var oldId = id("old");
        repo.replaceStaticQuests(Map.of(oldId, quest("Old")));
        repo.replaceStaticQuests(Map.of(id("new"), quest("New")));

        assertNull(repo.getQuest(oldId));
    }

    // ── Dynamic quests ───────────────────────────────────────────────────────

    @Test
    void createDynamicQuest_returnsEntryWithCorrectIdAndSource() {
        var questId = id("dq");
        var entry = repo.createDynamicQuest(questId);

        assertEquals(questId, entry.questId());
        assertEquals(QuestSourceType.DYNAMIC, entry.source());
    }

    @Test
    void getQuest_afterCreateDynamic_returnsQuest() {
        var questId = id("dq");
        repo.createDynamicQuest(questId);

        assertNotNull(repo.getQuest(questId));
    }

    @Test
    void getQuestSourceType_dynamicQuest_returnsDynamic() {
        var questId = id("dq");
        repo.createDynamicQuest(questId);

        assertEquals(QuestSourceType.DYNAMIC, repo.getQuestSourceType(questId));
    }

    @Test
    void getDynamicQuestIds_containsCreatedId() {
        var questId = id("dq");
        repo.createDynamicQuest(questId);

        assertTrue(repo.getDynamicQuestIds().contains(questId));
    }

    @Test
    void createDynamicQuest_duplicateId_throwsIllegalArgument() {
        var questId = id("dq");
        repo.createDynamicQuest(questId);

        assertThrows(IllegalArgumentException.class, () -> repo.createDynamicQuest(questId));
    }

    @Test
    void removeDynamicQuest_questNoLongerFound() {
        var questId = id("dq");
        repo.createDynamicQuest(questId);
        repo.removeDynamicQuest(questId);

        assertNull(repo.getQuest(questId));
    }

    @Test
    void removeDynamicQuest_nonExistent_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> repo.removeDynamicQuest(id("ghost")));
    }

    // ── Priority: dynamic over static ────────────────────────────────────────

    @Test
    void getQuest_dynamicTakesPriorityOverStatic() {
        var questId = id("q");
        var staticQ = quest("Static");
        repo.replaceStaticQuests(Map.of(questId, staticQ));
        repo.createDynamicQuest(questId);

        assertNotSame(staticQ, repo.getQuest(questId));
    }

    @Test
    void getQuestSourceType_dynamicShadowsStatic_returnsDynamic() {
        var questId = id("q");
        repo.replaceStaticQuests(Map.of(questId, quest("Static")));
        repo.createDynamicQuest(questId);

        assertEquals(QuestSourceType.DYNAMIC, repo.getQuestSourceType(questId));
    }

    // ── getQuest / requireQuest ──────────────────────────────────────────────

    @Test
    void getQuest_nonExistent_returnsNull() {
        assertNull(repo.getQuest(id("ghost")));
    }

    @Test
    void requireQuest_nonExistent_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> repo.requireQuest(id("ghost")));
    }

    // ── getTask / requireTask ────────────────────────────────────────────────

    @Test
    void getTask_taskExists_returnsTask() {
        var questId = id("q");
        repo.createDynamicQuest(questId);
        repo.getQuestModifier(questId).addTaskRequired("t1");

        assertNotNull(repo.getTask(questId, "t1"));
    }

    @Test
    void getTask_nonExistentTask_returnsNull() {
        var questId = id("q");
        repo.createDynamicQuest(questId);

        assertNull(repo.getTask(questId, "ghost"));
    }

    @Test
    void getTask_nonExistentQuest_returnsNull() {
        assertNull(repo.getTask(id("ghost"), "t1"));
    }

    @Test
    void requireTask_nonExistentTask_throwsIllegalArgument() {
        var questId = id("q");
        repo.createDynamicQuest(questId);

        assertThrows(IllegalArgumentException.class, () -> repo.requireTask(questId, "ghost"));
    }

    // ── QuestEntry / TaskEntry ───────────────────────────────────────────────

    @Test
    void getQuestEntry_nonExistent_returnsNull() {
        assertNull(repo.getQuestEntry(id("ghost")));
    }

    @Test
    void requireQuestEntry_nonExistent_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> repo.requireQuestEntry(id("ghost")));
    }

    @Test
    void getTaskEntry_questNotExists_returnsNull() {
        assertNull(repo.getTaskEntry(id("ghost"), "t1"));
    }

    @Test
    void getTaskEntry_taskNotExists_returnsNull() {
        var questId = id("q");
        repo.createDynamicQuest(questId);

        assertNull(repo.getTaskEntry(questId, "ghost"));
    }

    @Test
    void getTaskEntry_taskExists_hasCorrectIds() {
        var questId = id("q");
        repo.createDynamicQuest(questId);
        repo.getQuestModifier(questId).addTaskRequired("t1");

        var entry = repo.getTaskEntry(questId, "t1");
        assertNotNull(entry);
        assertEquals(questId, entry.questId());
        assertEquals("t1", entry.taskId());
    }

    @Test
    void requireTaskEntry_nonExistentTask_throwsIllegalArgument() {
        var questId = id("q");
        repo.createDynamicQuest(questId);

        assertThrows(IllegalArgumentException.class, () -> repo.requireTaskEntry(questId, "ghost"));
    }

    // ── getQuestIds ──────────────────────────────────────────────────────────

    @Test
    void getQuestIds_containsBothStaticAndDynamic() {
        var staticId = id("static");
        var dynamicId = id("dynamic");
        repo.replaceStaticQuests(Map.of(staticId, quest("S")));
        repo.createDynamicQuest(dynamicId);

        var ids = repo.getQuestIds();
        assertTrue(ids.contains(staticId));
        assertTrue(ids.contains(dynamicId));
    }

    @Test
    void getQuestIds_sameIdInBoth_noDuplicates() {
        var questId = id("q");
        repo.replaceStaticQuests(Map.of(questId, quest("S")));
        repo.createDynamicQuest(questId);

        assertEquals(1, repo.getQuestIds().stream().filter(questId::equals).count());
    }

    // ── Dependencies / dependants ────────────────────────────────────────────

    @Test
    void getDependentQuests_noRegisteredDependencies_returnsEmpty() {
        assertTrue(repo.getDependentQuests(id("q")).isEmpty());
    }

    @Test
    void getDependentQuests_afterReplaceStaticQuests_recomputesDependants() {
        var depId = id("dep");
        var dependantId = id("dependant");

        var dependant = quest("Dependant");
        dependant.addDependency(depId);

        repo.replaceStaticQuests(Map.of(depId, quest("Dep"), dependantId, dependant));

        assertTrue(repo.getDependentQuests(depId).contains(dependantId));
    }

    @Test
    void getDependentQuests_afterAddOrDependency_includesDependant() {
        var depId = id("dep");
        repo.replaceStaticQuests(Map.of(depId, quest("Dep")));

        var questId = id("q");
        repo.createDynamicQuest(questId);
        repo.getQuestModifier(questId).addOrDependency(depId);

        assertTrue(repo.getDependentQuests(depId).contains(questId));
    }

    @Test
    void getDependentQuests_afterAddAndDependency_includesDependant() {
        var depId = id("dep");
        repo.replaceStaticQuests(Map.of(depId, quest("Dep")));

        var questId = id("q");
        repo.createDynamicQuest(questId);
        var mod = repo.getQuestModifier(questId);
        mod.addOrDependency(depId);     // создаёт первую группу
        mod.addAndDependency(depId);    // добавляет в ту же группу

        // Квест зависит от depId, значит depId.dependants должен содержать questId
        assertTrue(repo.getDependentQuests(depId).contains(questId));
    }

    @Test
    void getDependentQuests_afterRemoveDependencies_excludesDependant() {
        var depId = id("dep");
        repo.replaceStaticQuests(Map.of(depId, quest("Dep")));

        var questId = id("q");
        repo.createDynamicQuest(questId);
        var mod = repo.getQuestModifier(questId);
        mod.addOrDependency(depId);
        mod.removeDependencies();

        assertFalse(repo.getDependentQuests(depId).contains(questId));
    }

    // ── QuestModifier.isDirty ────────────────────────────────────────────────

    @Test
    void questModifier_isDirty_falseInitially() {
        var questId = id("q");
        repo.createDynamicQuest(questId);

        assertFalse(repo.getQuestModifier(questId).isDirty());
    }

    @Test
    void questModifier_isDirty_trueAfterSetTitle() {
        var questId = id("q");
        repo.createDynamicQuest(questId);
        var mod = repo.getQuestModifier(questId);
        mod.setTitle(Text.literal("New"));

        assertTrue(mod.isDirty());
    }

    @Test
    void getQuestModifier_staticQuest_throwsIllegalArgument() {
        var questId = id("q");
        repo.replaceStaticQuests(Map.of(questId, quest("Q")));

        assertThrows(IllegalArgumentException.class, () -> repo.getQuestModifier(questId));
    }

    // ── getTaskStages ────────────────────────────────────────────────────────

    @Test
    void getTaskStages_requiredTask_returnsSingleStage() {
        var questId = id("q");
        repo.createDynamicQuest(questId);
        repo.getQuestModifier(questId).addTaskRequired("t1");

        assertEquals(List.of(0), repo.getTaskStages(questId, "t1"));
    }

    @Test
    void getTaskStages_optionalTaskInFirstStage_returnsSameStage() {
        var questId = id("q");
        repo.createDynamicQuest(questId);
        var mod = repo.getQuestModifier(questId);
        mod.addTaskRequired("t1");
        mod.addTaskOptional("t2");

        assertEquals(List.of(0), repo.getTaskStages(questId, "t2"));
    }

    @Test
    void getTaskStages_taskInMultipleStages_returnsAllStageIndices() {
        var questId = id("q");
        // Строим квест вручную: t1 обязательна в stage 0, t2 обязательна в stage 1,
        // и t1 также добавлена как optional в stage 1
        var q = quest("Q");
        q.setTask("t1", MutableTask.create(Text.literal("T1")));
        q.addTaskToStage("t1");       // stage 0
        q.setTask("t2", MutableTask.create(Text.literal("T2")));
        q.addTaskToStage("t2");       // stage 1
        q.addTaskToStage(1, "t1");    // t1 optional в stage 1

        repo.replaceStaticQuests(Map.of(questId, q));

        assertEquals(List.of(0, 1), repo.getTaskStages(questId, "t1"));
    }

    // ── getTaskType ──────────────────────────────────────────────────────────

    @Test
    void getTaskType_requiredTask_returnsRequired() {
        var questId = id("q");
        repo.createDynamicQuest(questId);
        repo.getQuestModifier(questId).addTaskRequired("t1");

        assertEquals(TaskType.REQUIRED, repo.getTaskType(questId, "t1", 0));
    }

    @Test
    void getTaskType_optionalTask_returnsOptional() {
        var questId = id("q");
        repo.createDynamicQuest(questId);
        var mod = repo.getQuestModifier(questId);
        mod.addTaskRequired("t1");
        mod.addTaskOptional("t2");

        assertEquals(TaskType.OPTIONAL, repo.getTaskType(questId, "t2", 0));
    }

    @Test
    void getTaskType_invalidStageIndex_returnsNull() {
        var questId = id("q");
        repo.createDynamicQuest(questId);
        repo.getQuestModifier(questId).addTaskRequired("t1");

        assertNull(repo.getTaskType(questId, "t1", 99));
    }

    @Test
    void getTaskType_taskNotInGivenStage_returnsNull() {
        var questId = id("q");
        repo.createDynamicQuest(questId);
        var mod = repo.getQuestModifier(questId);
        mod.addTaskRequired("t1");  // stage 0
        mod.addTaskRequired("t2");  // stage 1

        // t2 не входит в stage 0
        assertNull(repo.getTaskType(questId, "t2", 0));
    }
}

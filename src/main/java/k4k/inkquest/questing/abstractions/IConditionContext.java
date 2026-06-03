package k4k.inkquest.questing.abstractions;

import k4k.inkquest.domain.enums.CompletionStatus;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Среда выполнения, предоставляемая обработчикам условий задач.
 *
 * <p>Экземпляр контекста всегда привязан к конкретной тройке (игрок, questId, taskId).
 * Обработчики не должны обращаться к состоянию за пределами этой тройки —
 * кросс-квестовые ссылки намеренно не поддерживаются.
 *
 * <p>Реализация живёт в инфраструктурном слое ({@code infra/conditions/}),
 * тем самым изолируя questing-слой от Minecraft-сервисов (Scoreboard, LootManager и т.д.).
 */
public interface IConditionContext {

    // ── Scoreboard ────────────────────────────────────────────────────────────

    /**
     * Гарантирует существование scoreboard objective с заданным именем и критерием.
     * Если objective уже существует — ничего не делает.
     * Вызывается из {@code load()} для подготовки scoreboard до первого тика.
     *
     * @param objectiveName имя objective
     * @param criterion     критерий objective
     */
    void ensureScoreboardObjective(String objectiveName, ScoreboardCriterion criterion);

    /**
     * Возвращает текущее значение scoreboard score.
     *
     * @param objectiveName  имя objective
     * @param playerOverride имя игрока/сущности, чей score читается;
     *                       {@code null} означает контекстного игрока
     * @return текущий score; 0 если objective не существует
     */
    int getScore(String objectiveName, @Nullable String playerOverride);

    /**
     * Устанавливает значение scoreboard score. Вызывается из {@code load()},
     * когда условие требует переписать счёт начальным значением (например,
     * {@code initial} у {@code ScoreCondition}). Если objective не существует —
     * ничего не делает; перед вызовом должен быть {@link #ensureScoreboardObjective}.
     *
     * @param objectiveName  имя objective
     * @param playerOverride имя игрока/сущности, чей score переписывается;
     *                       {@code null} означает контекстного игрока
     * @param value          новое значение score
     */
    void setScore(String objectiveName, @Nullable String playerOverride, int value);

    // ── Predicates ────────────────────────────────────────────────────────────

    /**
     * Вычисляет predicate против контекстного игрока.
     * Возвращает {@code false} если predicate не найден.
     *
     * @param predicateId идентификатор predicate
     */
    boolean testPredicate(Identifier predicateId);

    // ── Состояние задач (только тот же квест) ────────────────────────────────

    /**
     * Возвращает {@code true} если задача завершена с любым терминальным статусом.
     *
     * @param taskId идентификатор задачи в том же квесте
     */
    boolean isTaskComplete(String taskId);

    /**
     * Возвращает {@code true} если задача завершена с конкретным статусом.
     *
     * @param taskId   идентификатор задачи в том же квесте
     * @param expected ожидаемый статус
     */
    boolean isTaskComplete(String taskId, CompletionStatus expected);

    /**
     * Возвращает список task ID optional-задач активного этапа текущего квеста,
     * исключая required-задачу этапа и task ID самого контекста.
     * Пустой список если нет активного этапа или квест не найден.
     * Пустой список трактуется как «условие выполнено» (вакуумная истина).
     */
    List<String> getActiveStageOptionalTaskIds();
}

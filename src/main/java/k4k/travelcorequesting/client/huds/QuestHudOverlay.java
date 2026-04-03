package k4k.travelcorequesting.client.huds;

import com.mojang.blaze3d.systems.RenderSystem;
import k4k.travelcorequesting.client.utils.DrawContexts;
import k4k.travelcorequesting.common.animation.Animation;
import k4k.travelcorequesting.common.animation.Animator;
import k4k.travelcorequesting.common.animation.parameter_animations.*;
import k4k.travelcorequesting.domain.enums.CompletionStatus;
import k4k.travelcorequesting.questing.enums.TaskType;
import k4k.travelcorequesting.questing.models.QuestDisplay;
import k4k.travelcorequesting.questing.models.TaskDisplay;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.joml.Vector2d;

import java.util.*;

public class QuestHudOverlay implements HudRenderCallback {
    private static final Identifier TASK_ICONS_TEXTURE = Identifier.of("tq", "textures/icons/default.png");

    private static final int QUEST_TASKS_GAP = 2;
    private static final int TASK_ICON_GAP = 2;
    private static final int TASKS_PADDING = 4;
    private static final int MAIN_TASK_PADDING = 6;
    private static final int QUESTS_GAP = 4;
    private static final int PROGRESS_BAR_HEIGHT = 1;      // НОВОЕ: высота прогресс-бара
    private static final int PROGRESS_BAR_GAP = 1;         // НОВОЕ: отступ между текстом и прогресс-баром

    private static final Animation QUEST_IN_ANIMATION = new Animation.Builder()
            .addParameterAnimation("Position", SlideParameterAnimation.slideIn(-10, 0), Animation.ONE_TIME, 500, Vector2d.class)
            .addParameterAnimation("Opacity", FadeParameterAnimation.fadeIn(), Animation.ONE_TIME, 500, Float.class)
            .build();

    private static final Animation TASK_IN_ANIMATION = new Animation.Builder()
            .addParameterAnimation("IconU", SwitchValueParameterAnimation.switchTo(0), Animation.ONE_TIME, 1, 250, Integer.class)
            .addParameterAnimation("Position", SlideParameterAnimation.slideIn(-10, 0), Animation.ONE_TIME, 500, Vector2d.class)
            .addParameterAnimation("Opacity", FadeParameterAnimation.fadeIn(), Animation.ONE_TIME, 500, Float.class)
            .build();

    private static final Animation TASK_FAILED_ANIMATION = new Animation.Builder()
            .addParameterAnimation("IconU", SwitchValueParameterAnimation.switchTo(24), Animation.ONE_TIME, 1, 100, Integer.class)
            .addParameterAnimation("Position", BopParameterAnimation.bopRight(2), Animation.ONE_TIME, 200, 0, Vector2d.class)
            .addParameterAnimation("Strikethrough", SwitchValueParameterAnimation.switchTo(true), Animation.ONE_TIME, 1, 150, Boolean.class)
            .build();

    private static final Animation TASK_SUCCESS_ANIMATION = new Animation.Builder()
            .addParameterAnimation("IconU", SwitchValueParameterAnimation.switchTo(16), Animation.ONE_TIME, 1, 100, Integer.class)
            .addParameterAnimation("Position", BopParameterAnimation.bopRight(2), Animation.ONE_TIME, 200, 0, Vector2d.class)
            .build();

    private static final Animation TASK_OUT_SKIPPED_ANIMATION = new Animation.Builder()
            .addParameterAnimation("IconU", SwitchValueParameterAnimation.switchTo(32), Animation.ONE_TIME, 1, 100, Integer.class)
            .addParameterAnimation("Position", BopParameterAnimation.bopRight(2), Animation.ONE_TIME, 200, 0, Vector2d.class)
            .build();

    private final MinecraftClient client = MinecraftClient.getInstance();

    private final List<Identifier> questOrder = new ArrayList<>();
    private final Map<Identifier, QuestDisplay> quests = new HashMap<>();
    private final Map<Identifier, Map<String, TaskDisplay>> tasks = new HashMap<>();

    // НОВОЕ: хранилище текущих значений прогресса для каждой задачи
    private final Map<Identifier, Map<String, TaskProgress>> taskProgresses = new HashMap<>();
    // НОВОЕ: хранилище статусов завершения задач (если задача завершена, прогресс-бары не рисуем)
    private final Map<Identifier, Map<String, CompletionStatus>> taskCompletionStatuses = new HashMap<>();

    private final Animator EMPTY_ANIMATOR = new Animator();
    private final Map<TaskDisplay, Animator> taskAnimators = new WeakHashMap<>();
    private final Map<QuestDisplay, Animator> questAnimators = new WeakHashMap<>();

    public QuestHudOverlay() {}

    public void addQuest(Identifier questId, QuestDisplay quest, Map<String, TaskDisplay> tasks) {
        var isStageChange = this.quests.containsKey(questId);
        this.quests.put(questId, quest);

        // НОВОЕ: очищаем старые данные прогресса и статусов для этого квеста
        this.taskProgresses.remove(questId);
        this.taskCompletionStatuses.remove(questId);
        this.tasks.remove(questId);

        for (var taskEntry : tasks.entrySet()) {
            this.addTask(questId, taskEntry.getKey(), taskEntry.getValue());
        }

        if (isStageChange)
            return;

        var animator = new Animator();
        animator.play(QUEST_IN_ANIMATION, Util.getMeasuringTimeMs());

        this.questAnimators.put(quest, animator);

        this.questOrder.add(questId);
        this.questOrder.sort(Comparator.comparing(qid -> this.quests.containsKey(qid) ? this.quests.get(qid).sortIndex() : 0));
    }

    public void addTask(Identifier questId, String taskId, TaskDisplay task) {
        var quest = this.quests.get(questId);
        if (quest == null) return;

        var animator = new Animator();
        animator.play(TASK_IN_ANIMATION, Util.getMeasuringTimeMs());

        this.taskAnimators.put(task, animator);
        this.tasks.computeIfAbsent(questId, key -> new HashMap<>())
                .put(taskId, task);

        // НОВОЕ: инициализируем прогресс задачи (0,0) и статус null
        this.taskProgresses.computeIfAbsent(questId, key -> new HashMap<>())
                .put(taskId, new TaskProgress(0, 0));
        this.taskCompletionStatuses.computeIfAbsent(questId, key -> new HashMap<>())
                .put(taskId, null);
    }

    // ИЗМЕНЁННЫЙ МЕТОД: теперь обновляет хранимый прогресс
    public void setTaskProgress(Identifier questId, String taskId, int value, boolean isSuccessProgress) {
        var questProgressMap = this.taskProgresses.get(questId);
        if (questProgressMap == null) return;

        var progress = questProgressMap.get(taskId);
        if (progress == null) return;

        // Не обновляем прогресс, если задача уже завершена
        var statusMap = this.taskCompletionStatuses.get(questId);
        if (statusMap != null && statusMap.get(taskId) != null) return;

        if (isSuccessProgress) {
            progress = new TaskProgress(value, progress.failureValue());
        } else {
            progress = new TaskProgress(progress.successValue(), value);
        }
        questProgressMap.put(taskId, progress);
    }

    // ИЗМЕНЁННЫЙ МЕТОД: теперь сохраняет статус завершения
    public void completeTask(Identifier questId, String taskId, CompletionStatus status) {
        var taskMap = this.tasks.get(questId);
        if (taskMap == null) return;

        var task = taskMap.get(taskId);
        if (task == null) return;

        var animator = taskAnimators.get(task);
        if (animator == null) return;

        animator.play(switch (status) {
            case SUCCESS -> TASK_SUCCESS_ANIMATION;
            case FAILURE -> TASK_FAILED_ANIMATION;
            case SKIPPED -> TASK_OUT_SKIPPED_ANIMATION;
        }, Util.getMeasuringTimeMs());

        // НОВОЕ: сохраняем статус завершения задачи, чтобы прогресс-бары больше не рисовались
        var statusMap = this.taskCompletionStatuses.computeIfAbsent(questId, key -> new HashMap<>());
        statusMap.put(taskId, status);
    }

    public void removeQuest(Identifier questId) {
        this.quests.remove(questId);
        // НОВОЕ: удаляем связанные данные прогресса и статусов
        this.taskProgresses.remove(questId);
        this.taskCompletionStatuses.remove(questId);
    }

    @Override
    public void onHudRender(DrawContext drawContext, float v) {
        if (client.player == null || client.world == null) return;

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        var hudWidth = getHudWidth();
        var hudHeight = getHudHeight();

        MatrixStack matrices = drawContext.getMatrices();

        matrices.push();
        matrices.translate(1, (float) (screenHeight - hudHeight) / 2, 0);

        renderHud(drawContext, hudWidth, hudHeight);

        matrices.pop();
    }

    private int getHudWidth() {
        return 120;
    }

    private int getHudHeight() {
        var quests = this.quests;
        var result = quests.size() * QUESTS_GAP;
        for (var questEntry : quests.entrySet())
            result += getQuestHeight(questEntry.getKey(), questEntry.getValue());
        return result;
    }

    // ИЗМЕНЁННЫЙ МЕТОД: учитывает высоту прогресс-баров при расчёте высоты задачи
    private int getQuestHeight(Identifier questId, QuestDisplay quest) {
        var result = client.textRenderer.getWrappedLinesHeight(quest.title(), getHudWidth() - 2);
        var taskMap = this.tasks.get(questId);
        if (taskMap == null) return result;

        for (var taskIndex = 0; taskIndex < quest.tasks().size(); taskIndex++) {
            var taskId = quest.tasks().get(taskIndex);
            var task = taskMap.get(taskId);
            result += getTaskHeight(task, TaskType.from(taskIndex == 0), questId, taskId);
        }
        result += QUEST_TASKS_GAP * (tasks.size() > 1 ? 2 : 1);
        return result;
    }

    // ИЗМЕНЁННЫЙ МЕТОД: добавляет высоту прогресс-баров
    private int getTaskHeight(TaskDisplay task, TaskType type, Identifier questId, String taskId) {
        int textHeight = client.textRenderer.getWrappedLinesHeight(task.title(),
                getHudWidth() - TASKS_PADDING - (!type.isRequired() ? MAIN_TASK_PADDING : 0) - 8 - TASK_ICON_GAP - 2);

        int extraHeight = 0;
        // Проверяем, не завершена ли задача
        CompletionStatus status = null;
        var statusMap = this.taskCompletionStatuses.get(questId);
        if (statusMap != null) status = statusMap.get(taskId);

        if (status == null) {
            var progressMap = this.taskProgresses.get(questId);
            TaskProgress progress = progressMap != null ? progressMap.get(taskId) : null;
            if (progress != null) {
                if (task.successTarget() != null) extraHeight += PROGRESS_BAR_HEIGHT + PROGRESS_BAR_GAP;
                if (task.failureTarget() != null) extraHeight += PROGRESS_BAR_HEIGHT + PROGRESS_BAR_GAP;
            }
        }
        return textHeight + extraHeight;
    }

    private void renderHud(DrawContext drawContext, int width, int height) {
        var t = Util.getMeasuringTimeMs();

        var y = 0;

        for (var questId : this.questOrder) {
            var quest = this.quests.get(questId);
            if (quest == null) return;

            y += renderQuest(drawContext, questId, quest, t, 0, y, width) + QUESTS_GAP;
        }
    }

    private int renderQuest(DrawContext context, Identifier questId, QuestDisplay quest, long t, int x, int y, int w) {
        var animator = this.questAnimators.computeIfAbsent(quest, key -> EMPTY_ANIMATOR);
        var initialY = y;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(
                1, 1, 1,
                animator.getParameter("Opacity", t, Float.class).orElse(1f)
        );

        var questX = x + (int) animator
                .getParameter("Position", t, Vector2d.class)
                .orElseGet(Vector2d::new).x;

        y += DrawContexts.drawTextWrapped(
                context,
                client.textRenderer,
                quest.title(),
                questX + 1, y + 1,
                w - 2,
                0xFFFFFFFF,
                true
        );

        var taskMap = this.tasks.get(questId);
        if (taskMap == null) return y - initialY;

        y += QUEST_TASKS_GAP + 1;

        for (var taskIndex = 0; taskIndex < quest.tasks().size(); taskIndex++) {
            var taskId = quest.tasks().get(taskIndex);
            var task = taskMap.get(taskId);
            var type = TaskType.from(taskIndex == 0);

            var isLastTask = taskIndex == quest.tasks().size() - 1;

            var taskX = TASKS_PADDING + (type.isRequired() ? 0 : MAIN_TASK_PADDING);

            var taskH = this.renderTask(context, questId, taskId, task, type.isRequired(), t, x + taskX, y, w - taskX);

            y += taskH + (type.isRequired() && !isLastTask ? QUEST_TASKS_GAP : 0);
        }

        return y - initialY;
    }

    // ИЗМЕНЁННЫЙ МЕТОД: теперь принимает questId и taskId для доступа к прогрессу и статусу,
    // а также рисует прогресс-бары под текстом задачи
    private int renderTask(DrawContext context, Identifier questId, String taskId, TaskDisplay task, boolean isRequired, long t, int x, int y, int w) {
        var animator = this.taskAnimators.computeIfAbsent(task, key -> EMPTY_ANIMATOR);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.setShaderColor(
                0.25f, 0.25f, 0.25f,
                animator.getParameter("Opacity", t, Float.class).orElse(1f)
        );

        x += (int) animator
                .getParameter("Position", t, Vector2d.class)
                .orElseGet(Vector2d::new).x;

        // Отрисовка иконки
        int iconU = animator.getParameter("IconU", t, Integer.class).orElse(0);
        context.drawTexture(
                TASK_ICONS_TEXTURE,
                x + 1, y + 1,
                iconU, isRequired ? 8 : 0,
                8, 8
        );

        RenderSystem.setShaderColor(
                1, 1, 1,
                animator.getParameter("Opacity", t, Float.class).orElse(1f)
        );

        context.drawTexture(
                TASK_ICONS_TEXTURE,
                x, y,
                iconU, isRequired ? 8 : 0,
                8, 8
        );

        // Отрисовка текста задачи
        var text = animator.getParameter("Strikethrough", t, Boolean.class).orElse(false)
                ? Text.literal("").append(task.title()).formatted(Formatting.STRIKETHROUGH)
                : task.title();

        int textHeight = DrawContexts.drawTextWrapped(
                context,
                client.textRenderer,
                text,
                x + 8 + TASK_ICON_GAP, y,
                w - 8 - TASK_ICON_GAP,
                0xFFFFFFFF,
                true
        );

        // НОВОЕ: отрисовка прогресс-баров
        int totalHeight = textHeight;
        CompletionStatus status = null;
        var statusMap = this.taskCompletionStatuses.get(questId);
        if (statusMap != null) status = statusMap.get(taskId);

        // Рисуем бары только если задача ещё не завершена
        if (status == null) {
            TaskProgress progress = null;
            var progressMap = this.taskProgresses.get(questId);
            if (progressMap != null) progress = progressMap.get(taskId);

            int currentY = y + textHeight + PROGRESS_BAR_GAP;

            // Бар успеха
            if (task.successTarget() != null && progress != null) {
                int target = task.successTarget();
                int current = Math.min(progress.successValue(), target);
                int barWidth = (int) ((w - 8 - TASK_ICON_GAP) * ((float) current / target));
                if (barWidth > 0) {
                    context.fill(x + 8 + TASK_ICON_GAP, currentY,
                            x + 8 + TASK_ICON_GAP + barWidth, currentY + PROGRESS_BAR_HEIGHT,
                            0xAA00FF00); // зелёный с прозрачностью
                }
                currentY += PROGRESS_BAR_HEIGHT + PROGRESS_BAR_GAP;
                totalHeight += PROGRESS_BAR_HEIGHT + PROGRESS_BAR_GAP;
            }

            // Бар провала
            if (task.failureTarget() != null && progress != null) {
                int target = task.failureTarget();
                int current = Math.min(progress.failureValue(), target);
                int barWidth = (int) ((w - 8 - TASK_ICON_GAP) * ((float) current / target));
                if (barWidth > 0) {
                    context.fill(x + 8 + TASK_ICON_GAP, currentY,
                            x + 8 + TASK_ICON_GAP + barWidth, currentY + PROGRESS_BAR_HEIGHT,
                            0xAAFF0000); // красный с прозрачностью
                }
                totalHeight += PROGRESS_BAR_HEIGHT + PROGRESS_BAR_GAP;
            }
        }

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();

        return totalHeight;
    }

    // НОВЫЙ ВСПОМОГАТЕЛЬНЫЙ КЛАСС для хранения прогресса задачи
    private record TaskProgress(int successValue, int failureValue) {}

    private record AnimatedQuest(QuestDisplay quest, Map<Identifier, AnimatedTask> animatedTasks) {}
    private record AnimatedTask(TaskDisplay task, Animator animator) {}
}
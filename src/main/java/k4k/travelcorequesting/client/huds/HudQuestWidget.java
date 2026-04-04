package k4k.travelcorequesting.client.huds;

import k4k.travelcorequesting.client.utils.DrawContexts;
import k4k.travelcorequesting.common.animation.Animation;
import k4k.travelcorequesting.common.animation.Animator;
import k4k.travelcorequesting.common.animation.parameter_animations.FadeParameterAnimation;
import k4k.travelcorequesting.common.animation.parameter_animations.SlideParameterAnimation;
import k4k.travelcorequesting.domain.enums.CompletionStatus;
import k4k.travelcorequesting.questing.models.QuestDisplay;
import k4k.travelcorequesting.questing.models.TaskDisplay;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2d;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HudQuestWidget {
    private static final int QUEST_TASKS_GAP = 2;
    private static final int TASKS_PADDING = 4;
    private static final int OPTIONAL_TASK_EXTRA_PADDING = 6;

    private static final Animation IN_ANIMATION = new Animation.Builder()
            .addParameterAnimation("Position", SlideParameterAnimation.slideIn(-10, 0), Animation.ONE_TIME, 500, Vector2d.class)
            .addParameterAnimation("Opacity", FadeParameterAnimation.fadeIn(), Animation.ONE_TIME, 500, Float.class)
            .build();

    // TODO: анимация fade-out при завершении/откреплении

    private final MinecraftClient client = MinecraftClient.getInstance();

    private QuestDisplay display;
    private final LinkedHashMap<String, HudTaskWidget> taskWidgets = new LinkedHashMap<>();
    private final Animator animator = new Animator();
    private @Nullable Transition activeTransition = null;

    private record Transition(
            LinkedHashMap<String, HudTaskWidget> outgoingTasks,
            long expiresAt
    ) {}

    public HudQuestWidget(QuestDisplay display, Map<String, TaskDisplay> tasks) {
        this.display = display;
        populateTasks(tasks);
    }

    public void playInAnimation() {
        animator.play(IN_ANIMATION, Util.getMeasuringTimeMs());
        taskWidgets.values().forEach(HudTaskWidget::playInAnimation);
    }

    public void changeStage(QuestDisplay display, Map<String, TaskDisplay> tasks) {
        var outgoing = new LinkedHashMap<>(taskWidgets);
        outgoing.values().forEach(HudTaskWidget::playOutAnimation);
        activeTransition = new Transition(outgoing, Util.getMeasuringTimeMs() + HudTaskWidget.getOutAnimationDuration());

        this.display = display;
        taskWidgets.clear();
        populateTasks(tasks);
        taskWidgets.values().forEach(HudTaskWidget::playInAnimation);
    }

    public void addTask(String taskId, TaskDisplay task) {
        // TODO: анимация "расталкивания" — место появляется плавно
        boolean isRequired = taskWidgets.isEmpty();
        var widget = new HudTaskWidget(task, isRequired);
        widget.playInAnimation();
        taskWidgets.put(taskId, widget);
    }

    public void update(long t) {
        if (activeTransition != null && t >= activeTransition.expiresAt()) {
            activeTransition = null;
        }
    }

    public void completeTask(String taskId, CompletionStatus status) {
        var widget = taskWidgets.get(taskId);
        if (widget == null) return;
        widget.complete(status);
    }

    public void setTaskProgress(String taskId, int value, boolean isSuccess) {
        var widget = taskWidgets.get(taskId);
        if (widget == null) return;
        widget.setProgress(value, isSuccess);
    }

    public int getSortIndex() {
        return display.sortIndex();
    }

    public int getHeight(int hudWidth) {
        int titleHeight = client.textRenderer.getWrappedLinesHeight(display.title(), hudWidth - 2);
        int currentHeight = computeTasksHeight(taskWidgets, hudWidth);

        if (activeTransition == null) return titleHeight + currentHeight;

        int outgoingHeight = computeTasksHeight(activeTransition.outgoingTasks(), hudWidth);
        return titleHeight + Math.max(currentHeight, outgoingHeight);
    }

    public int render(DrawContext context, long t, int x, int y, int hudWidth) {
        float opacity = animator.getParameter("Opacity", t, Float.class).orElse(1f);
        int offsetX = (int) animator.getParameter("Position", t, Vector2d.class).orElseGet(Vector2d::new).x;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1, 1, 1, opacity);

        int drawX = x + offsetX;
        int titleHeight = DrawContexts.drawTextWrapped(context, client.textRenderer, display.title(), drawX + 1, y + 1, hudWidth - 2, 0xFFFFFFFF, true);
        int taskStartY = y + titleHeight + QUEST_TASKS_GAP + 1;

        int currentTasksHeight = renderTasks(context, t, drawX, taskStartY, hudWidth, taskWidgets);

        int tasksHeight = currentTasksHeight;
        if (activeTransition != null) {
            int outgoingTasksHeight = renderTasks(context, t, drawX, taskStartY, hudWidth, activeTransition.outgoingTasks());
            tasksHeight = Math.max(currentTasksHeight, outgoingTasksHeight);
        }

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();

        if (tasksHeight == 0) return titleHeight;
        return titleHeight + QUEST_TASKS_GAP + 1 + tasksHeight;
    }

    private int renderTasks(DrawContext context, long t, int x, int y, int hudWidth, LinkedHashMap<String, HudTaskWidget> widgets) {
        if (widgets.isEmpty()) return 0;

        int initialY = y;
        int taskIndex = 0;
        int taskCount = widgets.size();

        for (var entry : widgets.entrySet()) {
            boolean isRequired = taskIndex == 0;
            int taskPadding = TASKS_PADDING + (isRequired ? 0 : OPTIONAL_TASK_EXTRA_PADDING);
            int taskHeight = entry.getValue().render(context, t, x + taskPadding, y, hudWidth - taskPadding);

            y += taskHeight;
            if (isRequired && taskIndex < taskCount - 1) {
                y += QUEST_TASKS_GAP;
            }
            taskIndex++;
        }

        return y - initialY;
    }

    private int computeTasksHeight(LinkedHashMap<String, HudTaskWidget> widgets, int hudWidth) {
        if (widgets.isEmpty()) return 0;

        int height = QUEST_TASKS_GAP;
        int taskIndex = 0;
        for (var widget : widgets.values()) {
            height += widget.getHeight(getTaskAvailableWidth(taskIndex == 0, hudWidth));
            if (taskIndex == 0 && widgets.size() > 1) height += QUEST_TASKS_GAP;
            taskIndex++;
        }
        return height;
    }

    private void populateTasks(Map<String, TaskDisplay> tasks) {
        List<String> taskOrder = display.tasks();
        int index = 0;
        for (var taskId : taskOrder) {
            var taskDisplay = tasks.get(taskId);
            if (taskDisplay != null) {
                taskWidgets.put(taskId, new HudTaskWidget(taskDisplay, index == 0));
            }
            index++;
        }
    }

    private int getTaskAvailableWidth(boolean isRequired, int hudWidth) {
        return hudWidth - TASKS_PADDING - (isRequired ? 0 : OPTIONAL_TASK_EXTRA_PADDING);
    }
}
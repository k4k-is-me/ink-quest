package k4k.travelcorequesting.client.huds;

import k4k.travelcorequesting.client.utils.DrawContexts;
import k4k.travelcorequesting.common.animation.Animation;
import k4k.travelcorequesting.common.animation.Animator;
import k4k.travelcorequesting.common.animation.ParameterKey;
import static k4k.travelcorequesting.common.animation.ParameterAnimations.*;
import k4k.travelcorequesting.domain.enums.CompletionStatus;
import k4k.travelcorequesting.questing.models.QuestDisplay;
import k4k.travelcorequesting.questing.models.TaskDisplay;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HudQuestWidget {
    private static final int QUEST_TASKS_GAP = 2;
    private static final int TASKS_PADDING = 4;
    private static final int OPTIONAL_TASK_EXTRA_PADDING = 6;

    private static final ParameterKey<Integer> POSITION = new ParameterKey<>(Integer.class, 0);
    private static final ParameterKey<Float> OPACITY = new ParameterKey<>(Float.class, 0f);

    private static final Animation IN_ANIMATION = new Animation.Builder()
            .addParameter(POSITION, slideIn(-10), 0, 5000)
            .addParameter(OPACITY, fadeIn(), 0, 5000)
            .build();

    private static final Animation OUT_ANIMATION = new Animation.Builder()
            .addParameter(POSITION, slideOut(-10), 0, 5000)
            .addParameter(OPACITY, fadeOut(), 0, 5000)
            .build();

    private final MinecraftClient client = MinecraftClient.getInstance();

    private QuestDisplay display;
    private final LinkedHashMap<String, HudTaskWidget> taskWidgets = new LinkedHashMap<>();
    private final Animator animator = new Animator(Util::getMeasuringTimeMs);
    private @Nullable LinkedHashMap<String, HudTaskWidget> outgoingTasks = null;
    private @Nullable String pinnedTaskId = null;

    public HudQuestWidget(QuestDisplay display, Map<String, TaskDisplay> tasks) {
        this.display = display;
        populateTasks(tasks);
    }

    public void playInAnimation() {
        animator.play(IN_ANIMATION);
        taskWidgets.values().forEach(HudTaskWidget::playInAnimation);
    }

    public void playOutAnimation() {
        animator.play(OUT_ANIMATION);
        taskWidgets.values().forEach(HudTaskWidget::playOutAnimation);
    }

    public boolean isAnimatorIdle() {
        return animator.isIdle() && taskWidgets.values().stream().allMatch(HudTaskWidget::isAnimatorIdle);
    }

    public void changeStage(QuestDisplay display, Map<String, TaskDisplay> tasks) {
        outgoingTasks = new LinkedHashMap<>(taskWidgets);
        outgoingTasks.values().forEach(HudTaskWidget::playSwitchOutAnimation);

        this.display = display;
        taskWidgets.clear();
        pinnedTaskId = null;
        populateTasks(tasks);
    }

    public void addTask(String taskId, TaskDisplay task) {
        if (taskWidgets.containsKey(taskId)) return;
        boolean isRequired = taskWidgets.isEmpty();
        var widget = new HudTaskWidget(task, isRequired);
        widget.playInAnimation();
        taskWidgets.put(taskId, widget);
    }

    public void setTaskPin(String taskId) {
        String firstTaskId = taskWidgets.isEmpty() ? null : taskWidgets.keySet().iterator().next();
        pinnedTaskId = taskId.equals(firstTaskId) ? null : taskId;
    }

    public void update() {
        if (outgoingTasks != null && outgoingTasks.values().stream().allMatch(HudTaskWidget::isAnimatorIdle)) {
            outgoingTasks = null;
            taskWidgets.values().forEach(HudTaskWidget::playSwitchInAnimation);
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
        int currentHeight = computeCurrentTasksHeight(taskWidgets, hudWidth);

        if (outgoingTasks == null) return titleHeight + currentHeight;

        int outgoingHeight = computeTasksHeight(outgoingTasks, hudWidth, null);
        return titleHeight + Math.max(currentHeight, outgoingHeight);
    }

    public int render(DrawContext context, int x, int y, int hudWidth) {
        animator.tick();
        float opacity = animator.getParameter(OPACITY);
        int offsetX = animator.getParameter(POSITION);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1, 1, 1, opacity);

        int drawX = x + offsetX;
        int titleColor = ((int) (opacity * 255) << 24) | 0x00FFFFFF;
        int titleHeight = DrawContexts.drawTextWrapped(context, client.textRenderer, display.title(), drawX + 1, y + 1, hudWidth - 2, titleColor, true);
        int taskStartY = y + titleHeight + QUEST_TASKS_GAP + 1;

        int currentTasksHeight;
        if (isPinnedOptional()) {
            var pinnedWidget = taskWidgets.get(pinnedTaskId);
            int pinnedHeight = pinnedWidget.render(context, drawX + TASKS_PADDING, taskStartY, hudWidth - TASKS_PADDING);
            int mainStartY = taskStartY + pinnedHeight + QUEST_TASKS_GAP;
            int mainHeight = renderTasks(context, drawX, mainStartY, hudWidth, taskWidgets, pinnedTaskId);
            currentTasksHeight = pinnedHeight + QUEST_TASKS_GAP + mainHeight;
        } else {
            currentTasksHeight = renderTasks(context, drawX, taskStartY, hudWidth, taskWidgets, null);
        }

        int tasksHeight = currentTasksHeight;
        if (outgoingTasks != null) {
            int outgoingTasksHeight = renderTasks(context, drawX, taskStartY, hudWidth, outgoingTasks, null);
            tasksHeight = Math.max(currentTasksHeight, outgoingTasksHeight);
        }

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();

        if (tasksHeight == 0) return titleHeight;
        return titleHeight + QUEST_TASKS_GAP + 1 + tasksHeight;
    }

    private int renderTasks(DrawContext context, int x, int y, int hudWidth, LinkedHashMap<String, HudTaskWidget> widgets, @Nullable String skipTaskId) {
        int taskCount = countTasks(widgets, skipTaskId);
        if (taskCount == 0) return 0;

        int initialY = y;
        int taskIndex = 0;

        for (var entry : widgets.entrySet()) {
            if (entry.getKey().equals(skipTaskId)) continue;
            boolean isRequired = taskIndex == 0;
            int taskPadding = TASKS_PADDING + (isRequired ? 0 : OPTIONAL_TASK_EXTRA_PADDING);
            int taskHeight = entry.getValue().render(context, x + taskPadding, y, hudWidth - taskPadding);

            y += taskHeight;
            if (isRequired && taskIndex < taskCount - 1) {
                y += QUEST_TASKS_GAP;
            }
            taskIndex++;
        }

        return y - initialY;
    }

    private int computeCurrentTasksHeight(LinkedHashMap<String, HudTaskWidget> widgets, int hudWidth) {
        if (!isPinnedOptional()) return computeTasksHeight(widgets, hudWidth, null);
        var pinnedWidget = widgets.get(pinnedTaskId);
        int pinnedHeight = QUEST_TASKS_GAP + pinnedWidget.getHeight(getTaskAvailableWidth(true, hudWidth));
        int mainHeight = computeTasksHeight(widgets, hudWidth, pinnedTaskId);
        return pinnedHeight + mainHeight;
    }

    private int computeTasksHeight(LinkedHashMap<String, HudTaskWidget> widgets, int hudWidth, @Nullable String skipTaskId) {
        int taskCount = countTasks(widgets, skipTaskId);
        if (taskCount == 0) return 0;

        int height = QUEST_TASKS_GAP;
        int taskIndex = 0;

        for (var entry : widgets.entrySet()) {
            if (entry.getKey().equals(skipTaskId)) continue;
            height += entry.getValue().getHeight(getTaskAvailableWidth(taskIndex == 0, hudWidth));
            if (taskIndex == 0 && taskCount > 1) height += QUEST_TASKS_GAP;
            taskIndex++;
        }
        return height;
    }

    private int countTasks(LinkedHashMap<String, HudTaskWidget> widgets, @Nullable String skipTaskId) {
        if (skipTaskId == null) return widgets.size();
        return (int) widgets.keySet().stream().filter(k -> !k.equals(skipTaskId)).count();
    }

    private boolean isPinnedOptional() {
        if (pinnedTaskId == null || !taskWidgets.containsKey(pinnedTaskId)) return false;
        return !pinnedTaskId.equals(taskWidgets.keySet().iterator().next());
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
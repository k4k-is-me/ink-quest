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

    // TODO: анимация fade-out при смене этапа
    // TODO: анимация fade-out при завершении/откреплении

    private final MinecraftClient client = MinecraftClient.getInstance();

    private QuestDisplay display;
    private final LinkedHashMap<String, HudTaskWidget> taskWidgets = new LinkedHashMap<>();
    private final Animator animator = new Animator();

    public HudQuestWidget(QuestDisplay display, Map<String, TaskDisplay> tasks) {
        this.display = display;
        populateTasks(tasks);
    }

    public void introduce() {
        animator.play(IN_ANIMATION, Util.getMeasuringTimeMs());

        for (var taskWidget : taskWidgets.values()) {
            taskWidget.playInAnimation();
        }
    }

    public void changeStage(QuestDisplay display, Map<String, TaskDisplay> tasks) {
        // TODO: анимация fade-out старого этапа → fade-in нового
        this.display = display;
        this.taskWidgets.clear();
        populateTasks(tasks);

        for (var taskWidget : taskWidgets.values()) {
            taskWidget.playInAnimation();
        }
    }

    public void addTask(String taskId, TaskDisplay task) {
        // TODO: анимация "расталкивания" — место появляется плавно
        boolean isRequired = taskWidgets.isEmpty();
        var widget = new HudTaskWidget(task, isRequired);
        widget.playInAnimation();
        taskWidgets.put(taskId, widget);
    }

    public void completeTask(String taskId, CompletionStatus status) {
        var widget = taskWidgets.get(taskId);
        if (widget != null) {
            widget.complete(status);
        }
    }

    public void setTaskProgress(String taskId, int value, boolean isSuccess) {
        var widget = taskWidgets.get(taskId);
        if (widget != null) {
            widget.setProgress(value, isSuccess);
        }
    }

    public int getSortIndex() {
        return display.sortIndex();
    }

    public int getHeight(int hudWidth) {
        int height = client.textRenderer.getWrappedLinesHeight(display.title(), hudWidth - 2);

        if (!taskWidgets.isEmpty()) {
            height += QUEST_TASKS_GAP;
            int taskIndex = 0;
            for (var widget : taskWidgets.values()) {
                int taskWidth = getTaskAvailableWidth(taskIndex == 0, hudWidth);
                height += widget.getHeight(taskWidth);
                if (taskIndex == 0 && taskWidgets.size() > 1) {
                    height += QUEST_TASKS_GAP;
                }
                taskIndex++;
            }
        }

        return height;
    }

    public int render(DrawContext context, long t, int x, int y, int hudWidth) {
        float opacity = animator.getParameter("Opacity", t, Float.class).orElse(1f);
        int offsetX = (int) animator.getParameter("Position", t, Vector2d.class).orElseGet(Vector2d::new).x;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1, 1, 1, opacity);

        int drawX = x + offsetX;
        int initialY = y;

        // Заголовок квеста
        y += DrawContexts.drawTextWrapped(context, client.textRenderer, display.title(), drawX + 1, y + 1, hudWidth - 2, 0xFFFFFFFF, true);

        if (taskWidgets.isEmpty()) {
            return y - initialY;
        }

        y += QUEST_TASKS_GAP + 1;

        // Задачи
        int taskIndex = 0;
        int taskCount = taskWidgets.size();
        for (var entry : taskWidgets.entrySet()) {
            boolean isRequired = taskIndex == 0;
            int taskPadding = TASKS_PADDING + (isRequired ? 0 : OPTIONAL_TASK_EXTRA_PADDING);
            int taskWidth = hudWidth - taskPadding;

            int taskHeight = entry.getValue().render(context, t, drawX + taskPadding, y, taskWidth);

            y += taskHeight;
            if (isRequired && taskIndex < taskCount - 1) {
                y += QUEST_TASKS_GAP;
            }
            taskIndex++;
        }

        return y - initialY;
    }

    private void populateTasks(Map<String, TaskDisplay> tasks) {
        List<String> taskOrder = display.tasks();
        int index = 0;
        for (var taskId : taskOrder) {
            var taskDisplay = tasks.get(taskId);
            if (taskDisplay != null) {
                boolean isRequired = index == 0;
                taskWidgets.put(taskId, new HudTaskWidget(taskDisplay, isRequired));
            }
            index++;
        }
    }

    private int getTaskAvailableWidth(boolean isRequired, int hudWidth) {
        int padding = TASKS_PADDING + (isRequired ? 0 : OPTIONAL_TASK_EXTRA_PADDING);
        return hudWidth - padding;
    }
}
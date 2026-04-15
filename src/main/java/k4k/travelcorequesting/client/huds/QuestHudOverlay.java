package k4k.travelcorequesting.client.huds;

import k4k.travelcorequesting.domain.enums.CompletionStatus;
import k4k.travelcorequesting.questing.models.HudQuest;
import k4k.travelcorequesting.questing.models.HudTask;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.util.*;

public class QuestHudOverlay implements HudRenderCallback {
    private static final int HUD_WIDTH = 120;
    private static final int QUESTS_GAP = 10;

    private final MinecraftClient client = MinecraftClient.getInstance();

    private final List<Identifier> questOrder = new ArrayList<>();
    private final Map<Identifier, HudQuestWidget> questWidgets = new HashMap<>();
    private final Set<Identifier> outgoingQuests = new HashSet<>();

    public QuestHudOverlay() {}

    public void addQuest(Identifier questId, HudQuest quest, Map<String, HudTask> tasks) {
        var existing = questWidgets.get(questId);

        if (existing != null) {
            existing.changeStage(quest, tasks);
            return;
        }

        var widget = new HudQuestWidget(quest, tasks);
        widget.playInAnimation();

        questWidgets.put(questId, widget);

        questOrder.add(questId);
        questOrder.sort(Comparator.comparingInt(id -> {
            var w = questWidgets.get(id);
            return w != null ? w.getSortIndex() : 0;
        }));
    }

    public void addTask(Identifier questId, String taskId, HudTask task) {
        var widget = questWidgets.get(questId);
        if (widget == null) return;
        widget.addTask(taskId, task);
    }

    public void completeTask(Identifier questId, String taskId, CompletionStatus status) {
        var widget = questWidgets.get(questId);
        if (widget == null) return;
        widget.completeTask(taskId, status);
    }

    public void setTaskProgress(Identifier questId, String taskId, int value, boolean isSuccessProgress) {
        var widget = questWidgets.get(questId);
        if (widget == null) return;
        widget.setTaskProgress(taskId, value, isSuccessProgress);
    }

    public void setTaskPin(Identifier questId, String taskId) {
        var widget = questWidgets.get(questId);
        if (widget == null) return;
        widget.setTaskPin(taskId);
    }

    public void removeTask(Identifier questId, String taskId) {
        var widget = questWidgets.get(questId);
        if (widget == null) return;
        widget.removeTask(taskId);
    }

    public void removeQuest(Identifier questId) {
        var widget = questWidgets.get(questId);
        if (widget == null) return;

        widget.playOutAnimation();
        outgoingQuests.add(questId);
    }

    @Override
    public void onHudRender(DrawContext drawContext, float v) {
        if (client.player == null || client.world == null) return;

        int screenHeight = client.getWindow().getScaledHeight();
        int hudHeight = getHudHeight();

        MatrixStack matrices = drawContext.getMatrices();
        matrices.push();
        matrices.translate(1, (float) (screenHeight - hudHeight) / 2, 0);

        renderHud(drawContext);

        matrices.pop();
    }

    private int getHudHeight() {
        int height = 0;
        for (var questId : questOrder) {
            var widget = questWidgets.get(questId);
            if (widget != null) {
                if (height > 0) height += QUESTS_GAP;
                height += widget.getHeight(HUD_WIDTH);
            }
        }
        return height;
    }

    private void renderHud(DrawContext drawContext) {
        outgoingQuests.removeIf(questId -> {
            var widget = questWidgets.get(questId);
            if (widget != null && !widget.isAnimatorIdle()) return false;

            questWidgets.remove(questId);
            questOrder.remove(questId);
            return true;
        });

        for (var widget : questWidgets.values()) widget.update();

        int y = 0;
        for (var questId : questOrder) {
            var widget = questWidgets.get(questId);
            if (widget == null) continue;

            y += widget.render(drawContext, 0, y, HUD_WIDTH) + QUESTS_GAP;
        }
    }
}
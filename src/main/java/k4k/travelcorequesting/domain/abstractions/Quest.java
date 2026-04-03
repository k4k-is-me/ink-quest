package k4k.travelcorequesting.domain.abstractions;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public interface Quest {
    Text title();
    @Nullable Text description();
    Identifier icon();
    int index();
    boolean background();
    boolean pin();

    // Methods to work with collections that do not require copying

    int getDependencyGroupsCount();
    List<Identifier> getDependencyGroup(int group);

    Set<String> getTasks();
    @Nullable Task getTask(String taskId);
    boolean containsTask(String taskId);
    boolean containsTask(String taskId, int stage);
    int getTaskCount();

    int getStageCount();
    List<String> getStage(int stage);
    String getRequiredTask(int stage);

    Set<String> getUnusedTasks();
}

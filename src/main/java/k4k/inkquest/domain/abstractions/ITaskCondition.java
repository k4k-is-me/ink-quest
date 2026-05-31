package k4k.inkquest.domain.abstractions;

public interface ITaskCondition {
    boolean isGradual();
    int getTargetValue();
}

package k4k.travelcorequesting.questing.enums;

public enum TaskType {
    REQUIRED,
    OPTIONAL,
    UNUSED;  // TODO: handle properly

    public static TaskType from(boolean isRequired) {
        return isRequired ? TaskType.REQUIRED : TaskType.OPTIONAL;
    }

    public boolean isRequired() {
        return this == TaskType.REQUIRED;
    }
}

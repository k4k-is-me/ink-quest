package k4k.inkquest.client.animation;

public final class ParameterKey<T> {
    private final Class<T> type;
    private final T defaultValue;

    public ParameterKey(Class<T> type, T defaultValue) {
        this.type = type;
        this.defaultValue = defaultValue;
    }

    public Class<T> getType() {
        return this.type;
    }

    public T getDefault() {
        return this.defaultValue;
    }
}
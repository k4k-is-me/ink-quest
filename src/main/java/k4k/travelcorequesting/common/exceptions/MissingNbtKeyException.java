package k4k.travelcorequesting.common.exceptions;

public class MissingNbtKeyException extends NbtDataException {
    private final String missingKey;

    public MissingNbtKeyException(String key) {
        super("Required NBT key not found: " + key);
        this.missingKey = key;
    }

    public String getMissingKey() {
        return missingKey;
    }
}

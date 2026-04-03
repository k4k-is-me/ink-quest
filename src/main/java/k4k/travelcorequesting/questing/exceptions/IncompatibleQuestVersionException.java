package k4k.travelcorequesting.questing.exceptions;

public class IncompatibleQuestVersionException extends RuntimeException {
    private final String questVersion;
    private final String parserVersion;

    public IncompatibleQuestVersionException(String questVersion, String parserVersion) {
        super("");
        this.questVersion = questVersion;
        this.parserVersion = parserVersion;
    }

    public String getQuestVersion() {
        return questVersion;
    }

    public String getParserVersion() {
        return parserVersion;
    }
}

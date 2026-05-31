package k4k.inkquest.infra.loaders;

import com.google.gson.*;
import k4k.inkquest.TravelcoreQuesting;
import k4k.inkquest.domain.abstractions.Quest;
import k4k.inkquest.questing.exceptions.IncompatibleQuestVersionException;
import k4k.inkquest.infra.serializers.json.QuestJsonSerializer;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;


/**
 * Отвечает за загрузку квестов из датапаков. Предоставляет доступ к загруженным квестам.
 */
public class QuestResourceLoader extends ResourceManagerBase {
    private final Map<Identifier, Quest> loadedQuests = new HashMap<>();

    public Map<Identifier, Quest> getLoadedQuests() {
        return Collections.unmodifiableMap(this.loadedQuests);
    }


    // ResourceManagerBase implementation

    @Override public Identifier getFabricId() { return Identifier.of(TravelcoreQuesting.MOD_ID, "quest"); }
    @Override public String getStartingPath() { return "quests"; }
    @Override public String getFileExtension() { return ".json"; }

    @Override
    public void setup() {
        this.loadedQuests.clear();
    }

    @Override
    public void process(InputStream stream, Identifier resourceId, Identifier strippedResourceId) {
        var reader = new BufferedReader(new InputStreamReader(stream));

        try {
            var quest = QuestJsonSerializer.deserialize(reader);
            this.loadedQuests.put(strippedResourceId, quest);
        } catch (IncompatibleQuestVersionException e) {
            TravelcoreQuesting.LOGGER.error(("Unable to read quest '%s': Version %s is incompatible with version of " +
                    "the parser %s. Try switching to newer or older version of the mod or adapt the quest to expected " +
                    "format. Skipped...")
                    .formatted(strippedResourceId, e.getQuestVersion(), e.getParserVersion()));
        } catch (JsonParseException e) {
            TravelcoreQuesting.LOGGER.warn("Unable to read quest '{}': {}", strippedResourceId, e.getMessage());
        }
    }
}

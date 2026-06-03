package k4k.inkquest.infra.serializers.json;

import com.google.gson.*;
import k4k.inkquest.TravelcoreQuesting;
import k4k.inkquest.common.serialization.IdentifierSerializer;
import k4k.inkquest.common.serialization.JUtil;
import k4k.inkquest.domain.enums.QuestPinMode;
import k4k.inkquest.domain.enums.TaskButton;
import k4k.inkquest.domain.models.MutableQuest;
import k4k.inkquest.domain.models.MutableTask;
import k4k.inkquest.domain.abstractions.ITaskCondition;
import k4k.inkquest.domain.models.QuestRequirement;
import k4k.inkquest.domain.models.TaskEventActions;
import k4k.inkquest.domain.enums.CompletionStatus;
import k4k.inkquest.domain.models.taskConditions.AllCondition;
import k4k.inkquest.domain.models.taskConditions.AnyCondition;
import k4k.inkquest.domain.models.taskConditions.NoneCondition;
import k4k.inkquest.domain.models.taskConditions.PredicateCondition;
import k4k.inkquest.domain.models.taskConditions.GlobalScoreCondition;
import k4k.inkquest.domain.models.taskConditions.ScoreCondition;
import k4k.inkquest.domain.models.taskConditions.OptionalsCondition;
import k4k.inkquest.questing.exceptions.IncompatibleQuestVersionException;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.LowercaseEnumTypeAdapterFactory;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class QuestJsonSerializer {

    // Increase variant if new changes in format are compatible with previous versions of the serializer,
    //   eg: added or removed an optional field, or added new enum value
    // Increase version if new changes are not compatible!
    //   eg: added or removed a required field, changed field type or enum value is removed
    protected static final int VERSION = 3;
    protected static final int VARIANT = 1;

    private static final Gson GSON = new GsonBuilder()
            .registerTypeHierarchyAdapter(MutableQuest.class, new GsonSerializer())
            .registerTypeHierarchyAdapter(Text.class, new Text.Serializer())
            .registerTypeHierarchyAdapter(Style.class, new Style.Serializer())
            .registerTypeAdapterFactory(new LowercaseEnumTypeAdapterFactory())
            .registerTypeAdapter(Identifier.class, new IdentifierSerializer())
            .disableHtmlEscaping()
            .create();

    public static MutableQuest deserialize(Reader reader) {
        return GSON.fromJson(reader, MutableQuest.class);
    }

    private static class GsonSerializer implements JsonDeserializer<MutableQuest> {
        @Override
        public MutableQuest deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            var version = JUtil.getRequiredMember(json, "version", JsonElement::getAsInt);
            var variant = JUtil.getRequiredMember(json, "variant", JsonElement::getAsInt);

            if (version != VERSION)
                throw new IncompatibleQuestVersionException(
                        "%s.%s".formatted(version, variant),
                        "%s.%s".formatted(VERSION, VARIANT)
                );

            if (variant != VARIANT)
                TravelcoreQuesting.LOGGER.warn(
                        "Quest version {}.{} is different from version of the serializer {}.{}. Some things might not be loaded.",
                        version, variant, VERSION, VARIANT
                );

            return deserializeQuest(json, context);
        }

        /** Logs a WARN for every key in {@code json} that is not in {@code known}. */
        private static void warnUnknownKeys(JsonElement json, String context, Set<String> known) {
            if (!json.isJsonObject()) return;
            for (var key : json.getAsJsonObject().keySet()) {
                if (!known.contains(key)) {
                    TravelcoreQuesting.LOGGER.warn(
                            "Unknown key \"{}\" in {} — it will be ignored", key, context);
                }
            }
        }

        private MutableQuest deserializeQuest(JsonElement json, JsonDeserializationContext context) {
            warnUnknownKeys(json, "quest", Set.of(
                    "$schema", "version", "variant", "title", "description",
                    "icon", "index", "repeatable", "pin_mode",
                    "after", "require", "tasks", "stages"));

            var title = JUtil.getRequiredMember(json, "title",
                    element -> (Text) context.deserialize(element, Text.class));

            var description = JUtil.getOptionalMember(json, "description",
                    element -> (Text) context.deserialize(element, Text.class));

            var icon = JUtil.getMemberWithDefault(json, "icon",
                    element -> context.deserialize(element, Identifier.class),
                    Identifier.of(TravelcoreQuesting.MOD_ID, "default"));

            var index = JUtil.getMemberWithDefault(json, "index", JsonElement::getAsInt, 0);

            var isRepeatable = JUtil.getMemberWithDefault(json, "repeatable", JsonElement::getAsBoolean, false);

            var pinMode = JUtil.getMemberWithDefault(json, "pin_mode", element -> {
                String smth = element.getAsString().toLowerCase();
                return switch (smth) {
                    case "auto" -> QuestPinMode.AUTO;
                    case "force" -> QuestPinMode.FORCE;
                    case "off" -> QuestPinMode.OFF;
                    default -> throw new JsonParseException("Unknown pin_mode '%s', expected: auto, force, off".formatted(smth));
                };
            }, QuestPinMode.AUTO);

            var dependencies = JUtil.getMemberArray(
                            json, "after",
                            element -> JUtil.readArray(element,
                                    subElement -> (Identifier) context.deserialize(subElement, Identifier.class)
                            )).stream()
                    .toList();

            var require = deserializeRequireIfPresent(json, context, dependencies);

            var tasks = JUtil.getMemberDictionary(json, "tasks",
                    element -> deserializeTask(element, context));

            var stages = JUtil.getMemberArray(json, "stages",
                            element -> JUtil.readArray(element, JsonElement::getAsString)).stream()
                    .filter(stage -> !stage.isEmpty())
                    .toList();

            var quest = MutableQuest.create(title);
            description.ifPresent(quest::setDescription);
            quest.setIcon(icon);
            quest.setIndex(index);
            quest.setRepeatable(isRepeatable);
            quest.setPinMode(pinMode);

            for (var taskEntry : tasks.entrySet()) {
                quest.setTask(taskEntry.getKey(), taskEntry.getValue());
            }

            quest.setDependencies(dependencies);
            quest.setRequire(require);
            quest.setStages(stages);

            return quest;
        }

        /** Десериализует блок require, или возвращает null если его нет. Предупреждает если require есть, а after пустой. */
        private QuestRequirement deserializeRequireIfPresent(JsonElement json, JsonDeserializationContext context, List<List<Identifier>> after) {
            var requireElement = JUtil.getOptionalMember(json, "require", e -> e);
            if (requireElement.isEmpty()) return null;

            var elem = requireElement.get();
            warnUnknownKeys(elem, "quest.require", Set.of("tags", "predicate"));

            if (after.isEmpty()) {
                TravelcoreQuesting.LOGGER.warn(
                        "Quest has 'require' but no 'after' — require will be ignored (quest unlocks immediately)");
            }

            var tags = JUtil.getMemberArray(elem, "tags", JsonElement::getAsString);

            var predicate = JUtil.getOptionalMember(elem, "predicate",
                    element -> (Identifier) context.deserialize(element, Identifier.class));

            return new QuestRequirement(predicate.orElse(null), tags);
        }

        private MutableTask deserializeTask(JsonElement json, JsonDeserializationContext context) {
            warnUnknownKeys(json, "task", Set.of("title", "description", "condition", "on", "buttons"));

            JUtil.getOptionalMember(json, "condition", e -> e).ifPresent(condBlock ->
                    warnUnknownKeys(condBlock, "task condition block", Set.of("success", "failure")));

            var title = JUtil.getRequiredMember(json, "title",
                    element -> (Text) context.deserialize(element, Text.class));

            var description = JUtil.getOptionalMember(json, "description",
                    element -> (Text) context.deserialize(element, Text.class));

            var successCondition = JUtil.getOptionalMember(json, "condition.success",
                    element -> deserializeTaskCondition(element, context));

            var failureCondition = JUtil.getOptionalMember(json, "condition.failure",
                    element -> deserializeTaskCondition(element, context));

            var onLoad = deserializeEventActions(json, "on.load", context);
            var onTick = deserializeEventActions(json, "on.tick", context);
            var onPinnedTick = deserializeEventActions(json, "on.pinned_tick", context);
            var onUnload = deserializeEventActions(json, "on.unload", context);
            var onSuccess = deserializeEventActions(json, "on.success", context);
            var onFailure = deserializeEventActions(json, "on.failure", context);

            var buttonsList = JUtil.getMemberArray(json, "buttons", el -> {
                var raw = el.getAsString().toLowerCase();
                return switch (raw) {
                    case "success" -> TaskButton.SUCCESS;
                    case "failure" -> TaskButton.FAILURE;
                    case "skip"    -> TaskButton.SKIP;
                    default -> throw new JsonParseException(
                            "Unknown button '%s', expected: success, failure, skip".formatted(raw));
                };
            });
            var buttons = EnumSet.noneOf(TaskButton.class);
            for (var btn : buttonsList) {
                if (!buttons.add(btn)) {
                    TravelcoreQuesting.LOGGER.warn(
                            "Duplicate button \"{}\" in task — it will be ignored", btn.name().toLowerCase());
                }
            }

            var task = MutableTask.create(title);
            description.ifPresent(task::setDescription);

            successCondition.ifPresent(task::setSuccessCondition);
            failureCondition.ifPresent(task::setFailureCondition);

            task.setOnLoad(onLoad);
            task.setOnTick(onTick);
            task.setOnPinnedTick(onPinnedTick);
            task.setOnUnload(onUnload);
            task.setOnSuccess(onSuccess);
            task.setOnFailure(onFailure);
            task.setButtons(buttons);

            return task;
        }

        /** Читает блок события (например on.load) в TaskEventActions. Возвращает EMPTY если блок отсутствует. */
        private TaskEventActions deserializeEventActions(JsonElement json, String path, JsonDeserializationContext context) {
            var eventElement = JUtil.getOptionalMember(json, path, e -> e);
            if (eventElement.isEmpty()) return TaskEventActions.EMPTY;

            var elem = eventElement.get();
            warnUnknownKeys(elem, "task " + path, Set.of("functions", "tags"));

            var functions = JUtil.getMemberArray(elem, "functions",
                    element -> (Identifier) context.deserialize(element, Identifier.class));

            var tags = JUtil.getMemberArray(elem, "tags", JsonElement::getAsString);

            return new TaskEventActions(functions, tags);
        }

        private ITaskCondition deserializeTaskCondition(JsonElement json, JsonDeserializationContext context) {
            var conditionType = JUtil.getRequiredMember(json, "type", JsonElement::getAsString);

            switch (conditionType) {
                case "predicate":
                    warnUnknownKeys(json, "condition (predicate)", Set.of("type", "predicate"));
                    Identifier predicate = JUtil.getRequiredMember(json, "predicate",
                            element -> context.deserialize(element, Identifier.class));

                    return new PredicateCondition(predicate);

                case "score":
                    warnUnknownKeys(json, "condition (score)", Set.of("type", "objective", "criterion", "from", "to", "reset"));
                    var objective = JUtil.getRequiredMember(json, "objective", JsonElement::getAsString);

                    var criterionString = JUtil.getMemberWithDefault(json, "criterion", JsonElement::getAsString, "dummy");
                    var criterion = ScoreboardCriterion.getOrCreateStatCriterion(criterionString).orElseThrow(() ->
                            new JsonParseException("Unknown criterion '%s'".formatted(criterionString)));

                    var from = JUtil.getMemberWithDefault(json, "from", JsonElement::getAsInt, 0);
                    var to = JUtil.getRequiredMember(json, "to", JsonElement::getAsInt);
                    var reset = JUtil.getMemberWithDefault(json, "reset", JsonElement::getAsBoolean, true);

                    return new ScoreCondition(objective, criterion, from, to, reset);

                case "global_score":
                    warnUnknownKeys(json, "condition (global_score)", Set.of("type", "objective", "player", "from", "to"));
                    var gsObjective = JUtil.getRequiredMember(json, "objective", JsonElement::getAsString);
                    var gsPlayer = JUtil.getMemberWithDefault(json, "player", JsonElement::getAsString, "#GLOBAL");
                    var gsFrom = JUtil.getMemberWithDefault(json, "from", JsonElement::getAsInt, 0);
                    var gsTo = JUtil.getRequiredMember(json, "to", JsonElement::getAsInt);

                    return new GlobalScoreCondition(gsObjective, gsPlayer, gsFrom, gsTo);

                case "all":
                    warnUnknownKeys(json, "condition (all)", Set.of("type", "conditions"));
                    var subConditions = JUtil.getMemberArray(json, "conditions",
                            element -> deserializeTaskCondition(element, context));
                    return new AllCondition(subConditions);

                case "any":
                    warnUnknownKeys(json, "condition (any)", Set.of("type", "conditions"));
                    var anySubConditions = JUtil.getMemberArray(json, "conditions",
                            element -> deserializeTaskCondition(element, context));
                    return new AnyCondition(anySubConditions);

                case "none":
                    warnUnknownKeys(json, "condition (none)", Set.of("type", "conditions"));
                    var noneSubConditions = JUtil.getMemberArray(json, "conditions",
                            element -> deserializeTaskCondition(element, context));
                    return new NoneCondition(noneSubConditions);

                case "optionals":
                    warnUnknownKeys(json, "condition (optionals)", Set.of("type", "status", "min"));
                    var optStatus = JUtil.getOptionalMember(json, "status",
                            e -> CompletionStatus.valueOf(e.getAsString().toUpperCase()));
                    var optMin = JUtil.getOptionalMember(json, "min", JsonElement::getAsInt);
                    return new OptionalsCondition(optStatus.orElse(null), optMin.orElse(null));

                default:
                    throw new JsonParseException(
                            "Condition type '%s' is not implemented".formatted(conditionType)
                    );
            }
        }
    }
}

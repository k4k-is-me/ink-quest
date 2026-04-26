package k4k.travelcorequesting.infra.serializers.json;

import com.google.gson.*;
import k4k.travelcorequesting.TravelcoreQuesting;
import k4k.travelcorequesting.common.serialization.IdentifierSerializer;
import k4k.travelcorequesting.common.serialization.JUtil;
import k4k.travelcorequesting.domain.enums.QuestPinMode;
import k4k.travelcorequesting.domain.models.MutableQuest;
import k4k.travelcorequesting.domain.models.MutableTask;
import k4k.travelcorequesting.domain.abstractions.ITaskCondition;
import k4k.travelcorequesting.domain.models.QuestRequirement;
import k4k.travelcorequesting.domain.models.TaskEventActions;
import k4k.travelcorequesting.domain.enums.CompletionStatus;
import k4k.travelcorequesting.domain.models.taskConditions.AllCondition;
import k4k.travelcorequesting.domain.models.taskConditions.AnyCondition;
import k4k.travelcorequesting.domain.models.taskConditions.NoneCondition;
import k4k.travelcorequesting.domain.models.taskConditions.PredicateCondition;
import k4k.travelcorequesting.domain.models.taskConditions.ScoreCondition;
import k4k.travelcorequesting.domain.models.taskConditions.TasksCondition;
import k4k.travelcorequesting.questing.exceptions.IncompatibleQuestVersionException;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.List;

/**
 *
 */
public class QuestJsonSerializer {

    // Increase variant if new changes in format are compatible with previous versions of the serializer,
    //   eg: added or removed an optional field, or added new enum value
    // Increase version if new changes are not compatible!
    //   eg: added or removed a required field, changed field type or enum value is removed
    protected static final int VERSION = 2;
    protected static final int VARIANT = 0;

    private static final Gson GSON = new GsonBuilder()
            .registerTypeHierarchyAdapter(MutableQuest.class, new GsonSerializer())
            .registerTypeHierarchyAdapter(Text.class, new Text.Serializer())
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

        private MutableQuest deserializeQuest(JsonElement json, JsonDeserializationContext context) {
            var title = JUtil.getRequiredMember(json, "title",
                    element -> (Text) context.deserialize(element, Text.class));

            var description = JUtil.getOptionalMember(json, "description",
                    element -> (Text) context.deserialize(element, Text.class));

            var icon = JUtil.getMemberWithDefault(json, "icon",
                    element -> context.deserialize(element, Identifier.class),
                    Identifier.of(TravelcoreQuesting.MOD_ID, "default"));

            var index = JUtil.getMemberWithDefault(json, "index", JsonElement::getAsInt, 0);

            var isBackground = JUtil.getMemberWithDefault(json, "background", JsonElement::getAsBoolean, false);

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
            quest.setBackground(isBackground);
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
            var onUnload = deserializeEventActions(json, "on.unload", context);
            var onSuccess = deserializeEventActions(json, "on.success", context);
            var onFailure = deserializeEventActions(json, "on.failure", context);

            var task = MutableTask.create(title);
            description.ifPresent(task::setDescription);

            successCondition.ifPresent(task::setSuccessCondition);
            failureCondition.ifPresent(task::setFailureCondition);

            task.setOnLoad(onLoad);
            task.setOnTick(onTick);
            task.setOnUnload(onUnload);
            task.setOnSuccess(onSuccess);
            task.setOnFailure(onFailure);

            return task;
        }

        /** Читает блок события (например on.load) в TaskEventActions. Возвращает EMPTY если блок отсутствует. */
        private TaskEventActions deserializeEventActions(JsonElement json, String path, JsonDeserializationContext context) {
            var eventElement = JUtil.getOptionalMember(json, path, e -> e);
            if (eventElement.isEmpty()) return TaskEventActions.EMPTY;

            var elem = eventElement.get();

            var functions = JUtil.getMemberArray(elem, "functions",
                    element -> (Identifier) context.deserialize(element, Identifier.class));

            var tags = JUtil.getMemberArray(elem, "tags", JsonElement::getAsString);

            return new TaskEventActions(functions, tags);
        }

        private ITaskCondition deserializeTaskCondition(JsonElement json, JsonDeserializationContext context) {
            var conditionType = JUtil.getRequiredMember(json, "type", JsonElement::getAsString);

            switch (conditionType) {
                case "predicate":
                    Identifier predicate = JUtil.getRequiredMember(json, "predicate",
                            element -> context.deserialize(element, Identifier.class));

                    return new PredicateCondition(predicate);

                case "score":
                    var objective = JUtil.getRequiredMember(json, "objective", JsonElement::getAsString);

                    var criterionString = JUtil.getMemberWithDefault(json, "criterion", JsonElement::getAsString, "dummy");
                    var criterion = ScoreboardCriterion.getOrCreateStatCriterion(criterionString).orElseThrow(() ->
                            new JsonParseException("Unknown criterion '%s'".formatted(criterionString)));

                    var player = JUtil.getOptionalMember(json, "player", JsonElement::getAsString);

                    var initial = JUtil.getOptionalMember(json, "initial", JsonElement::getAsInt);

                    var target = JUtil.getRequiredMember(json, "target", JsonElement::getAsInt);

                    return new ScoreCondition(
                            objective,
                            criterion,
                            player.orElse(null),
                            initial.orElse(null),
                            target
                    );

                case "all":
                    var subConditions = JUtil.getMemberArray(json, "conditions",
                            element -> deserializeTaskCondition(element, context));
                    return new AllCondition(subConditions);

                case "any":
                    var anySubConditions = JUtil.getMemberArray(json, "conditions",
                            element -> deserializeTaskCondition(element, context));
                    return new AnyCondition(anySubConditions);

                case "none":
                    var noneSubConditions = JUtil.getMemberArray(json, "conditions",
                            element -> deserializeTaskCondition(element, context));
                    return new NoneCondition(noneSubConditions);

                case "tasks":
                    var tasksStatus = JUtil.getOptionalMember(json, "status",
                            e -> CompletionStatus.valueOf(e.getAsString().toUpperCase()));
                    var tasksCount = JUtil.getOptionalMember(json, "count", JsonElement::getAsInt);
                    var tasksList = JUtil.getOptionalMember(json, "tasks",
                            e -> JUtil.readArray(e, JsonElement::getAsString));
                    return new TasksCondition(
                            tasksStatus.orElse(null),
                            tasksCount.orElse(null),
                            tasksList.orElse(null)
                    );

                default:
                    throw new JsonParseException(
                            "Condition type '%s' is not implemented".formatted(conditionType)
                    );
            }
        }
    }
}

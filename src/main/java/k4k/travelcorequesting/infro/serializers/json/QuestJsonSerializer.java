package k4k.travelcorequesting.infro.serializers.json;

import com.google.gson.*;
import k4k.travelcorequesting.TravelcoreQuesting;
import k4k.travelcorequesting.common.serialization.IdentifierSerializer;
import k4k.travelcorequesting.common.serialization.JUtil;
import k4k.travelcorequesting.domain.enums.QuestPinMode;
import k4k.travelcorequesting.domain.models.MutableQuest;
import k4k.travelcorequesting.domain.models.MutableTask;
import k4k.travelcorequesting.domain.abstractions.ITaskCondition;
import k4k.travelcorequesting.domain.models.taskConditions.AllCondition;
import k4k.travelcorequesting.domain.models.taskConditions.PredicateCondition;
import k4k.travelcorequesting.domain.models.taskConditions.ScoreCondition;
import k4k.travelcorequesting.questing.exceptions.IncompatibleQuestVersionException;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.Reader;
import java.lang.reflect.Type;

/**
 *
 */
public class QuestJsonSerializer {

    // Increase variant if new changes in format are compatible with previous versions of the serializer,
    //   eg: added or removed an optional field, or added new enum value
    // Increase version if new changes are not compatible!
    //   eg: added or removed a required field, changed field type or enum value is removed
    protected static final int VERSION = 1;
    protected static final int VARIANT = 1;

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
            // Основные поля квеста
            var title = JUtil.getRequiredMember(json, "title",
                    element -> (Text) context.deserialize(element, Text.class));

            var description = JUtil.getOptionalMember(json, "description",
                    element -> (Text) context.deserialize(element, Text.class));

            var icon = JUtil.getMemberWithDefault(json, "icon",
                    element -> context.deserialize(element, Identifier.class),
                    Identifier.of(TravelcoreQuesting.MOD_ID, "textures/icons/default"));

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

            // Зависимости квеста
            var dependencies = JUtil.getMemberArray(
                            json, "dependencies",
                            element -> JUtil.readArray(element,
                                    subElement -> (Identifier) context.deserialize(subElement, Identifier.class)
                            )).stream()
                    .toList();

            // Задачи квеста
            var tasks = JUtil.getMemberDictionary(json, "tasks",
                    element -> deserializeTask(element, context));

            // Этапы квеста
            var stages = JUtil.getMemberArray(json, "stages",
                            element -> JUtil.readArray(element, JsonElement::getAsString)).stream()
                    .filter(stage -> !stage.isEmpty())
                    .toList();

            // TODO: Test with empty and no stages
//            if (stages.isEmpty())
//                throw new JsonParseException("Quest must have at least one stage");
//
//            // Валидация: все задачи в этапах должны существовать
//            for (var stage : stages)
//                for (var taskId : stage)
//                    if (!tasks.containsKey(taskId))
//                        throw new JsonParseException("Stage must contain valid task ids");

            // Создаем квест
            var quest = MutableQuest.create(title);
            description.ifPresent(quest::setDescription);
            quest.setIcon(icon);
            quest.setIndex(index);
            quest.setBackground(isBackground);
            quest.setPinMode(pinMode);

            // Добавляем задачи
            for (var taskEntry : tasks.entrySet()) {
                quest.setTask(taskEntry.getKey(), taskEntry.getValue());
            }

            quest.setDependencies(dependencies);
            quest.setStages(stages);

            return quest;
        }

        private MutableTask deserializeTask(JsonElement json, JsonDeserializationContext context) {
            var title = JUtil.getRequiredMember(json, "title",
                    element -> (Text) context.deserialize(element, Text.class));

            var description = JUtil.getOptionalMember(json, "description",
                    element -> (Text) context.deserialize(element, Text.class));

            // Десериализация lifetime функций
            var loadFunction = JUtil.getOptionalMember(json, "lifetime.load",
                    element -> (Identifier) context.deserialize(element, Identifier.class));

            var tickFunction = JUtil.getOptionalMember(json, "lifetime.tick",
                    element -> (Identifier) context.deserialize(element, Identifier.class));

            var unloadFunction = JUtil.getOptionalMember(json, "lifetime.unload",
                    element -> (Identifier) context.deserialize(element, Identifier.class));

            // Десериализация условия успеха
            var successCondition = JUtil.getOptionalMember(json, "success.condition",
                    element -> deserializeTaskCondition(element, context));

            var isManualSuccess = JUtil.getOptionalMember(json, "success.manual", JsonElement::getAsBoolean);

            var successFunction = JUtil.getOptionalMember(json, "success.reward.function",
                    element -> (Identifier) context.deserialize(element, Identifier.class));

            // Десериализация условия провала (может отсутствовать)
            var failureCondition = JUtil.getOptionalMember(json, "failure.condition",
                    element -> deserializeTaskCondition(element, context));

            var isManualFailure = JUtil.getOptionalMember(json, "failure.manual", JsonElement::getAsBoolean);

            var failureFunction = JUtil.getOptionalMember(json, "failure.reward.function",
                    element -> (Identifier) context.deserialize(element, Identifier.class));

            // Создаем задачу
            var task = MutableTask.create(title);
            description.ifPresent(task::setDescription);

            loadFunction.ifPresent(task::setLoadFunction);
            tickFunction.ifPresent(task::setTickFunction);
            unloadFunction.ifPresent(task::setUnloadFunction);

            successCondition.ifPresent(task::setSuccessCondition);
            failureCondition.ifPresent(task::setFailureCondition);

            isManualSuccess.ifPresent(task::setManualSuccess);
            isManualFailure.ifPresent(task::setManualFailure);

            successFunction.ifPresent(task::setSuccessFunction);
            failureFunction.ifPresent(task::setFailureFunction);

            return task;
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

                default:
                    throw new JsonParseException(
                            "Condition type '%s' is not implemented".formatted(conditionType)
                    );
            }
        }
    }
}

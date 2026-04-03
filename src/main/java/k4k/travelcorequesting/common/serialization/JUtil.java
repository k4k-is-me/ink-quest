package k4k.travelcorequesting.common.serialization;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class JUtil {

    public static JsonObject getAsJsonObject(JsonElement json) {
        return getAsJsonObject(json, "Expected JsonObject");
    }

    public static JsonObject getAsJsonObject(JsonElement json, String exceptionMessage) {
        if (!json.isJsonObject())
            throw new JsonParseException(exceptionMessage);
        return json.getAsJsonObject();
    }

    public static JsonArray getAsJsonArray(JsonElement json) {
        return getAsJsonArray(json, "Expected JsonArray");
    }

    public static JsonArray getAsJsonArray(JsonElement json, String exceptionMessage) {
        if (!json.isJsonArray())
            throw new JsonParseException(exceptionMessage);
        return json.getAsJsonArray();
    }


    public static <T> T getMemberWithDefault(JsonElement json, String memberName, Reader<T> reader, T defaultValue) {
        return getMemberWithDefault(getAsJsonObject(json), memberName, reader, defaultValue);
    }

    public static <T> T getMemberWithDefault(JsonObject json, String memberName, Reader<T> reader, T defaultValue) {
        var member = getMember(json, memberName);
        if (member != null)
            return reader.read(member);
        return defaultValue;
    }


    public static <T> Optional<T> getOptionalMember(JsonElement json, String memberName, Reader<T> reader) {
        return getOptionalMember(getAsJsonObject(json), memberName, reader);
    }

    public static <T> Optional<T> getOptionalMember(JsonObject json, String memberName, Reader<T> reader) {
        var member = getMember(json, memberName);
        if (member != null)
            return Optional.of(reader.read(member));
        return Optional.empty();
    }


    public static <T> T getRequiredMember(JsonElement json, String memberName, Reader<T> reader) {
        return getRequiredMember(getAsJsonObject(json), memberName, reader);
    }

    public static <T> T getRequiredMember(JsonObject json, String memberName, Reader<T> reader) {
        var member = getMember(json, memberName);
        if (member != null)
            return reader.read(member);
        throw new JsonParseException("Cannot find member '%s'".formatted(memberName));
    }


    public static <T> List<T> getMemberArray(JsonElement json, String memberName, Reader<T> reader) {
        return getMemberArray(getAsJsonObject(json), memberName, reader);
    }

    public static <T> List<T> getMemberArray(JsonObject json, String memberName, Reader<T> reader) {
        var member = getMember(json, memberName);
        if (member == null)
            return new ArrayList<>();

        if (!member.isJsonArray())
            throw new JsonParseException("Cannot find member '%s' or it is of incorrect type".formatted(memberName));

        var result = new ArrayList<T>();

        for (var item : member.getAsJsonArray())
            result.add(reader.read(item));

        return result;
    }


    public static <T> List<T> readArray(JsonElement json, Reader<T> reader) {
        return readArray(getAsJsonArray(json), reader);
    }

    public static <T> List<T> readArray(JsonArray json, Reader<T> reader) {
        var result = new ArrayList<T>();

        for (var item : json)
            result.add(reader.read(item));

        return result;
    }


    public static <T> HashMap<String, T> getMemberDictionary(JsonElement json, String memberName, Reader<T> reader) {
        return getMemberDictionary(getAsJsonObject(json), memberName, reader);
    }

    public static <T> HashMap<String, T> getMemberDictionary(JsonObject json, String memberName, Reader<T> reader) {
        if (!json.has(memberName))
            return new HashMap<>();

        var member = json.get(memberName);

        if (!member.isJsonObject())
            throw new JsonParseException("Cannot find member '%s' or it is of incorrect type".formatted(memberName));

        var result = new HashMap<String, T>();

        for (var entry : member.getAsJsonObject().entrySet())
            result.put(entry.getKey(), reader.read(entry.getValue()));

        return result;
    }

    public static JsonElement getMember(JsonElement json, String memberName) {
        var pathParts = memberName.split("\\.");
        if (pathParts.length == 0) throw new IllegalArgumentException("Path must contain at least one part");

        var currentElement = json;
        for (var part : pathParts) {
            if (!currentElement.isJsonObject())
                throw new JsonParseException("Member '%s' is of incorrect type, object expected".formatted(part));
            currentElement = currentElement.getAsJsonObject().get(part);
            if (currentElement == null) return null;
        }
        return currentElement;
    }


    @FunctionalInterface
    public interface Reader<T> {
        T read(JsonElement json);
    }

    @FunctionalInterface
    public interface DefaultProvider<T> {
        T get();
    }
}

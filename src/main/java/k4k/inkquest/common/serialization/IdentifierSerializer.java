package k4k.inkquest.common.serialization;

import com.google.gson.*;
import net.minecraft.util.Identifier;

import java.lang.reflect.Type;

public class IdentifierSerializer implements JsonDeserializer<Identifier>, JsonSerializer<Identifier> {

    @Override
    public Identifier deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonNull())
            return null;

        if (json.isJsonPrimitive())
            return Identifier.tryParse(json.getAsString());

        if (!json.isJsonObject())
            throw new JsonParseException("Identifier must be represented ether by null, string or object with namespace and path");

        var jsonObject = json.getAsJsonObject();
        var namespace = jsonObject.getAsJsonPrimitive("namespace").getAsString();
        var path = jsonObject.getAsJsonPrimitive("path").getAsString();

        return Identifier.of(namespace, path);
    }

    @Override
    public JsonElement serialize(Identifier src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.toString());
    }

}

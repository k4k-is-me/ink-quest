package k4k.travelcorequesting.common.serialization;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для {@link JUtil}: чтение членов JSON-объектов, массивов, словарей, вложенных путей.
 */
class JUtilTest {

    // --- getAsJsonObject ---

    @Test
    void getAsJsonObject_givenObject_returnsIt() {
        var obj = new JsonObject();
        obj.addProperty("x", 1);
        assertSame(obj, JUtil.getAsJsonObject(obj));
    }

    @Test
    void getAsJsonObject_givenNonObject_throws() {
        assertThrows(JsonParseException.class, () -> JUtil.getAsJsonObject(new JsonPrimitive("hello")));
    }

    @Test
    void getAsJsonObject_givenNonObject_usesCustomMessage() {
        var ex = assertThrows(JsonParseException.class,
                () -> JUtil.getAsJsonObject(new JsonPrimitive(42), "custom message"));
        assertEquals("custom message", ex.getMessage());
    }

    // --- getAsJsonArray ---

    @Test
    void getAsJsonArray_givenArray_returnsIt() {
        var arr = new JsonArray();
        arr.add(1);
        assertSame(arr, JUtil.getAsJsonArray(arr));
    }

    @Test
    void getAsJsonArray_givenNonArray_throws() {
        assertThrows(JsonParseException.class, () -> JUtil.getAsJsonArray(new JsonPrimitive("hello")));
    }

    @Test
    void getAsJsonArray_givenNonArray_usesCustomMessage() {
        var ex = assertThrows(JsonParseException.class,
                () -> JUtil.getAsJsonArray(new JsonPrimitive(42), "array expected"));
        assertEquals("array expected", ex.getMessage());
    }

    // --- getRequiredMember ---

    @Test
    void getRequiredMember_presentMember_returnsReadValue() {
        var obj = new JsonObject();
        obj.addProperty("name", "hero");
        String result = JUtil.getRequiredMember(obj, "name", e -> e.getAsString());
        assertEquals("hero", result);
    }

    @Test
    void getRequiredMember_missingMember_throws() {
        var obj = new JsonObject();
        assertThrows(JsonParseException.class, () -> JUtil.getRequiredMember(obj, "missing", e -> e.getAsString()));
    }

    @Test
    void getRequiredMember_fromJsonElement_works() {
        var obj = new JsonObject();
        obj.addProperty("value", 7);
        int result = JUtil.getRequiredMember((com.google.gson.JsonElement) obj, "value", e -> e.getAsInt());
        assertEquals(7, result);
    }

    // --- getOptionalMember ---

    @Test
    void getOptionalMember_presentMember_returnsNonEmpty() {
        var obj = new JsonObject();
        obj.addProperty("flag", true);
        Optional<Boolean> result = JUtil.getOptionalMember(obj, "flag", e -> e.getAsBoolean());
        assertTrue(result.isPresent());
        assertTrue(result.get());
    }

    @Test
    void getOptionalMember_missingMember_returnsEmpty() {
        var obj = new JsonObject();
        Optional<String> result = JUtil.getOptionalMember(obj, "missing", e -> e.getAsString());
        assertTrue(result.isEmpty());
    }

    // --- getMemberWithDefault ---

    @Test
    void getMemberWithDefault_presentMember_returnsReadValue() {
        var obj = new JsonObject();
        obj.addProperty("count", 5);
        int result = JUtil.getMemberWithDefault(obj, "count", e -> e.getAsInt(), 0);
        assertEquals(5, result);
    }

    @Test
    void getMemberWithDefault_missingMember_returnsDefault() {
        var obj = new JsonObject();
        int result = JUtil.getMemberWithDefault(obj, "count", e -> e.getAsInt(), 42);
        assertEquals(42, result);
    }

    // --- getMemberArray ---

    @Test
    void getMemberArray_presentArray_returnsAllElements() {
        var obj = new JsonObject();
        var arr = new JsonArray();
        arr.add("a");
        arr.add("b");
        arr.add("c");
        obj.add("items", arr);

        List<String> result = JUtil.getMemberArray(obj, "items", e -> e.getAsString());
        assertEquals(List.of("a", "b", "c"), result);
    }

    @Test
    void getMemberArray_missingMember_returnsEmptyList() {
        var obj = new JsonObject();
        List<String> result = JUtil.getMemberArray(obj, "items", e -> e.getAsString());
        assertTrue(result.isEmpty());
    }

    @Test
    void getMemberArray_memberIsNotArray_throws() {
        var obj = new JsonObject();
        obj.addProperty("items", "not-an-array");
        assertThrows(JsonParseException.class, () -> JUtil.getMemberArray(obj, "items", e -> e.getAsString()));
    }

    // --- readArray ---

    @Test
    void readArray_givenArray_returnsAllElements() {
        var arr = new JsonArray();
        arr.add(10);
        arr.add(20);
        arr.add(30);
        List<Integer> result = JUtil.readArray(arr, e -> e.getAsInt());
        assertEquals(List.of(10, 20, 30), result);
    }

    @Test
    void readArray_emptyArray_returnsEmptyList() {
        List<Integer> result = JUtil.readArray(new JsonArray(), e -> e.getAsInt());
        assertTrue(result.isEmpty());
    }

    @Test
    void readArray_fromJsonElement_throwsWhenNotArray() {
        assertThrows(JsonParseException.class, () -> JUtil.readArray(new JsonPrimitive("x"), e -> e.getAsInt()));
    }

    // --- getMemberDictionary ---

    @Test
    void getMemberDictionary_presentObject_returnsAllEntries() {
        var dict = new JsonObject();
        dict.addProperty("a", 1);
        dict.addProperty("b", 2);
        var obj = new JsonObject();
        obj.add("map", dict);

        var result = JUtil.getMemberDictionary(obj, "map", e -> e.getAsInt());
        assertEquals(2, result.size());
        assertEquals(1, result.get("a"));
        assertEquals(2, result.get("b"));
    }

    @Test
    void getMemberDictionary_missingMember_returnsEmptyMap() {
        var obj = new JsonObject();
        var result = JUtil.getMemberDictionary(obj, "map", e -> e.getAsInt());
        assertTrue(result.isEmpty());
    }

    @Test
    void getMemberDictionary_memberIsNotObject_throws() {
        var obj = new JsonObject();
        var arr = new JsonArray();
        arr.add(1);
        obj.add("map", arr);
        assertThrows(JsonParseException.class, () -> JUtil.getMemberDictionary(obj, "map", e -> e.getAsInt()));
    }

    // --- getMember (dot-path) ---

    @Test
    void getMember_singleSegment_returnsDirectChild() {
        var obj = new JsonObject();
        obj.addProperty("title", "quest1");
        var result = JUtil.getMember(obj, "title");
        assertNotNull(result);
        assertEquals("quest1", result.getAsString());
    }

    @Test
    void getMember_nestedPath_traversesObjects() {
        var inner = new JsonObject();
        inner.addProperty("value", 99);
        var outer = new JsonObject();
        outer.add("nested", inner);

        var result = JUtil.getMember(outer, "nested.value");
        assertNotNull(result);
        assertEquals(99, result.getAsInt());
    }

    @Test
    void getMember_missingTopLevelKey_returnsNull() {
        var obj = new JsonObject();
        assertNull(JUtil.getMember(obj, "missing"));
    }

    @Test
    void getMember_missingIntermediateKey_returnsNull() {
        var obj = new JsonObject();
        assertNull(JUtil.getMember(obj, "a.b.c"));
    }

    @Test
    void getMember_intermediateIsNotObject_throws() {
        var obj = new JsonObject();
        obj.addProperty("leaf", "value");
        assertThrows(JsonParseException.class, () -> JUtil.getMember(obj, "leaf.child"));
    }

    @Test
    void getRequiredMember_nestedPath_traversesAndReads() {
        var inner = new JsonObject();
        inner.addProperty("id", "task_1");
        var outer = new JsonObject();
        outer.add("data", inner);

        String result = JUtil.getRequiredMember(outer, "data.id", e -> e.getAsString());
        assertEquals("task_1", result);
    }
}

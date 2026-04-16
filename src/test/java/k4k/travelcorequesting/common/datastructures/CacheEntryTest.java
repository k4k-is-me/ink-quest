package k4k.travelcorequesting.common.datastructures;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для {@link CacheEntry}: проверка {@code isExpired()} и фабричного метода {@code of()}.
 *
 * <p>Класс не поддерживает инъекцию часов, поэтому тесты используют граничные значения
 * времени (0L, Long.MAX_VALUE) и большие отступы TTL, чтобы избежать флакинеса.
 */
class CacheEntryTest {

    // --- isExpired ---

    @Test
    void isExpired_epochExpiresAt_returnsTrue() {
        var entry = new CacheEntry<>("x", 0L);
        assertTrue(entry.isExpired());
    }

    @Test
    void isExpired_maxLongExpiresAt_returnsFalse() {
        var entry = new CacheEntry<>("x", Long.MAX_VALUE);
        assertFalse(entry.isExpired());
    }

    // --- of ---

    @Test
    void of_storesValue() {
        var entry = CacheEntry.of("hello", 1000L);
        assertEquals("hello", entry.value());
    }

    @Test
    void of_nullValue_storesNull() {
        var entry = CacheEntry.of(null, 1000L);
        assertNull(entry.value());
    }

    @Test
    void of_positiveTtl_notExpiredImmediately() {
        assertFalse(CacheEntry.of("x", 5_000L).isExpired());
    }

    @Test
    void of_negativeTtl_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> CacheEntry.of("x", -5_000L));
    }

    @Test
    void of_setsExpiresAtWithinBounds() {
        long ttl = 1000L;
        long before = System.currentTimeMillis();
        var entry = CacheEntry.of("x", ttl);
        long after = System.currentTimeMillis();

        assertTrue(entry.expiresAt() >= before + ttl);
        assertTrue(entry.expiresAt() <= after + ttl);
    }
}
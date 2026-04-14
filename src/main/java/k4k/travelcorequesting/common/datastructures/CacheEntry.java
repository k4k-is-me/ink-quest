package k4k.travelcorequesting.common.datastructures;

/**
 * Обёртка значения с временем жизни.
 * Используется для клиентских кэшей, где нужно избегать повторных запросов на сервер.
 *
 * @param value     кэшированное значение
 * @param expiresAt момент истечения кэша в мс ({@link System#currentTimeMillis()})
 */
public record CacheEntry<T>(T value, long expiresAt) {

    /** @return {@code true}, если кэш устарел и значение нужно запросить заново */
    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    /**
     * Создаёт запись с временем жизни относительно текущего момента.
     *
     * @param value   кэшируемое значение
     * @param ttlMs   время жизни в миллисекундах
     */
    public static <T> CacheEntry<T> of(T value, long ttlMs) {
        return new CacheEntry<>(value, System.currentTimeMillis() + ttlMs);
    }
}

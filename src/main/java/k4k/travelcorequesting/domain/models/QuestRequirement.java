package k4k.travelcorequesting.domain.models;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Дополнительные условия разблокировки квеста, проверяемые вместе с {@code after}.
 * Оба поля опциональны; все условия объединяются через AND.
 */
public record QuestRequirement(@Nullable Identifier predicate, List<String> tags) {}

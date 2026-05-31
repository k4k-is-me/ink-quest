package k4k.inkquest.domain.models;

import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Действия, выполняемые при наступлении lifecycle-события задачи:
 * функции датапака и scoreboard-теги, добавляемые игроку.
 */
public record TaskEventActions(List<Identifier> functions, List<String> tags) {

    /** Пустые действия — ничего не вызывается и не добавляется. */
    public static final TaskEventActions EMPTY = new TaskEventActions(List.of(), List.of());
}

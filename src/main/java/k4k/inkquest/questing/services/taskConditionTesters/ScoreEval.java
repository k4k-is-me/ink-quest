package k4k.inkquest.questing.services.taskConditionTesters;

import k4k.inkquest.questing.abstractions.EvalResult;

/**
 * Утилиты для оценки прогресса и выполненности score-условий.
 *
 * <p>Методы чистые и не имеют зависимостей от Minecraft.
 * Используются {@link ScoreConditionHandler} и {@link GlobalScoreConditionHandler}.
 */
class ScoreEval {

    private ScoreEval() {}

    /**
     * Возвращает {@code true} если условие нисходящее ({@code from > to}).
     */
    static boolean descending(int from, int to) {
        return from > to;
    }

    /**
     * Возвращает размах диапазона {@code |to - from|}.
     */
    static int range(int from, int to) {
        return Math.abs(to - from);
    }

    /**
     * Возвращает пройденный путь к цели, зажатый в [{@code 0}, {@link #range}].
     *
     * <p>Для восходящих условий: {@code value - from}.
     * Для нисходящих: {@code from - value}.
     */
    static int progress(int value, int from, int to) {
        int raw = descending(from, to) ? from - value : value - from;
        return Math.max(0, Math.min(raw, range(from, to)));
    }

    /**
     * Возвращает {@code true} если условие выполнено.
     *
     * <p>Восходящее: {@code value >= to}. Нисходящее: {@code value <= to}.
     * Edge-case {@code from == to}: всегда {@code true}.
     */
    static boolean met(int value, int from, int to) {
        return descending(from, to) ? value <= to : value >= to;
    }

    /**
     * Вычисляет прогресс и выполненность за один проход.
     *
     * @param value текущее значение scoreboard
     * @param from  начальная точка (задаёт направление)
     * @param to    целевое значение
     * @return результат оценки
     */
    static EvalResult evaluate(int value, int from, int to) {
        return new EvalResult(progress(value, from, to), met(value, from, to));
    }
}

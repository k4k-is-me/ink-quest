package k4k.travelcorequesting.questing.abstractions;

/**
 * Результат оценки условия задачи за один проход.
 *
 * <p>Возвращается из {@link ITaskConditionHandler#evaluate}, объединяя
 * текущее значение прогресса и факт выполнения условия. Используется,
 * чтобы избежать двойной работы хэндлеров (некоторые {@code test} и
 * {@code getCurrentValue} требуют одинаковых вычислений — например,
 * проверки predicate).
 *
 * @param value текущее значение прогресса (для UI и обновления трекера)
 * @param met   {@code true} если условие выполнено
 */
public record EvalResult(int value, boolean met) {
}

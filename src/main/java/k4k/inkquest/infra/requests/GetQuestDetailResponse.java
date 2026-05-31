package k4k.inkquest.infra.requests;

import k4k.inkquest.common.requests.IResponse;
import k4k.inkquest.questing.models.QuestBookQuest;
import org.jetbrains.annotations.Nullable;

/**
 * Ответ сервера на {@link GetQuestDetailsRequest}.
 *
 * @param data детали квеста; {@code null} — если квест не найден или запрос отклонён rate limit-ом
 */
public record GetQuestDetailResponse(@Nullable QuestBookQuest data) implements IResponse {}

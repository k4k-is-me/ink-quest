package k4k.inkquest.infra.requests;

import k4k.inkquest.common.requests.IRequest;
import net.minecraft.util.Identifier;

/**
 * C2S запрос на получение деталей квеста для экрана квестовой книги.
 *
 * @param questId идентификатор запрашиваемого квеста
 */
public record GetQuestDetailsRequest(Identifier questId) implements IRequest<GetQuestDetailResponse> {}

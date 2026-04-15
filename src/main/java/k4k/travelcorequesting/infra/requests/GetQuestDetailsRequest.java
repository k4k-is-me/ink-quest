package k4k.travelcorequesting.infra.requests;

import k4k.travelcorequesting.common.requests.IRequest;
import net.minecraft.util.Identifier;

/**
 * C2S запрос на получение деталей квеста для экрана квестовой книги.
 *
 * @param questId идентификатор запрашиваемого квеста
 */
public record GetQuestDetailsRequest(Identifier questId) implements IRequest<GetQuestDetailResponse> {}

package dio.budgeting.support.request;

import dio.budgeting.infrastructure.http.request.TextCommandRequest;
import org.instancio.Instancio;
import org.instancio.InstancioApi;

import static dio.budgeting.support.TestConstants.USER_MESSAGE;
import static org.instancio.Select.field;

public final class TextCommandRequestSupport {

    private TextCommandRequestSupport() {
    }

    public static InstancioApi<TextCommandRequest> defaultTextCommandRequest() {
        return Instancio.of(TextCommandRequest.class)
                .set(field(TextCommandRequest::message), USER_MESSAGE);
    }
}

package dio.budgeting.support.input;

import dio.budgeting.application.input.TextCommandInput;
import org.instancio.Instancio;
import org.instancio.InstancioApi;

import static dio.budgeting.support.TestConstants.USER_MESSAGE;
import static org.instancio.Select.field;

public final class TextCommandInputSupport {

    private TextCommandInputSupport() {
    }

    public static InstancioApi<TextCommandInput> defaultTextCommandInput() {
        return Instancio.of(TextCommandInput.class)
                .set(field(TextCommandInput::message), USER_MESSAGE);
    }
}

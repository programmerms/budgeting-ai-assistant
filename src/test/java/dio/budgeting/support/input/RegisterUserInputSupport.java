package dio.budgeting.support.input;

import dio.budgeting.auth.application.input.RegisterUserInput;
import org.instancio.Instancio;
import org.instancio.InstancioApi;

import static dio.budgeting.support.TestConstants.RAW_PASSWORD;
import static dio.budgeting.support.TestConstants.USERNAME;
import static org.instancio.Select.field;

public final class RegisterUserInputSupport {

    private RegisterUserInputSupport() {
    }

    public static InstancioApi<RegisterUserInput> defaultRegisterUserInput() {
        return Instancio.of(RegisterUserInput.class)
                .set(field(RegisterUserInput::username), USERNAME)
                .set(field(RegisterUserInput::password), RAW_PASSWORD);
    }
}

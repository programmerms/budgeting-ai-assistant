package dio.budgeting.support.request;

import dio.budgeting.auth.infrastructure.http.request.RegisterUserRequest;
import org.instancio.Instancio;
import org.instancio.InstancioApi;

import static dio.budgeting.support.TestConstants.RAW_PASSWORD;
import static dio.budgeting.support.TestConstants.USERNAME;
import static org.instancio.Select.field;

public final class RegisterUserRequestSupport {

    private RegisterUserRequestSupport() {
    }

    public static InstancioApi<RegisterUserRequest> defaultRegisterUserRequest() {
        return Instancio.of(RegisterUserRequest.class)
                .set(field(RegisterUserRequest::username), USERNAME)
                .set(field(RegisterUserRequest::password), RAW_PASSWORD);
    }
}

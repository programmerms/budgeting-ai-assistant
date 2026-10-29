package dio.budgeting.auth.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class RestUsernamePasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    static final String LOGIN_URL = "/api/auth/login";
    static final String AUTHENTICATION_FAILED_MESSAGE = "Invalid credentials or access not granted";

    private final ObjectMapper objectMapper;

    public RestUsernamePasswordAuthenticationFilter(AuthenticationConfiguration authenticationConfiguration,
                                                    ObjectMapper objectMapper) {
        super(authenticationConfiguration.getAuthenticationManager());
        this.objectMapper = objectMapper;
        setFilterProcessesUrl(LOGIN_URL);
        setAuthenticationSuccessHandler((request, response, authentication) ->
                response.setStatus(HttpServletResponse.SC_OK));
        setAuthenticationFailureHandler((request, response, exception) ->
                writeUnauthorized(response));
    }

    static void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(AUTHENTICATION_FAILED_MESSAGE);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        LoginRequest loginRequest;

        try {
            loginRequest = objectMapper.readValue(request.getInputStream(), LoginRequest.class);
        } catch (IOException | RuntimeException exception) {
            throw new AuthenticationServiceException(AUTHENTICATION_FAILED_MESSAGE, exception);
        }

        var token = UsernamePasswordAuthenticationToken.unauthenticated(
                loginRequest.username(),
                loginRequest.password());

        return getAuthenticationManager().authenticate(token);
    }

    public record LoginRequest(String username, String password) {
    }
}

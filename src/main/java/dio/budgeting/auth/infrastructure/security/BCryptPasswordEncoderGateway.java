package dio.budgeting.auth.infrastructure.security;

import dio.budgeting.auth.application.PasswordEncoderGateway;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class BCryptPasswordEncoderGateway implements PasswordEncoderGateway {
    private final PasswordEncoder passwordEncoder;

    public BCryptPasswordEncoderGateway(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}

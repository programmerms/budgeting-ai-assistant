package dio.budgeting.auth.application;

public interface PasswordEncoderGateway {
    String encode(String rawPassword);
}

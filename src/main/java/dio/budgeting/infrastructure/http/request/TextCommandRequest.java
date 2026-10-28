package dio.budgeting.infrastructure.http.request;

import dio.budgeting.application.input.TextCommandInput;
import jakarta.validation.constraints.NotBlank;

public record TextCommandRequest(@NotBlank String message) {
    public TextCommandInput toInput() {
        return new TextCommandInput(message);
    }
}

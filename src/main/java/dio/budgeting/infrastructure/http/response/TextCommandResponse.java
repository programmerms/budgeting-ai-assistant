package dio.budgeting.infrastructure.http.response;

import dio.budgeting.application.output.TextCommandOutput;

public record TextCommandResponse(String message) {
    public static TextCommandResponse from(TextCommandOutput output) {
        return new TextCommandResponse(output.message());
    }
}

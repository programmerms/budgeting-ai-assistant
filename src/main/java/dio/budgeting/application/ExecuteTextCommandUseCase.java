package dio.budgeting.application;

import dio.budgeting.application.input.TextCommandInput;
import dio.budgeting.application.output.TextCommandOutput;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class ExecuteTextCommandUseCase {
    private final VoiceAssistantGateway voiceAssistantGateway;

    public ExecuteTextCommandUseCase(VoiceAssistantGateway voiceAssistantGateway) {
        this.voiceAssistantGateway = voiceAssistantGateway;
    }

    public TextCommandOutput execute(TextCommandInput input) {
        Assert.notNull(input, "Text command input must not be null");
        Assert.hasText(input.message(), "Message must not be blank");

        var reply = voiceAssistantGateway.chat(input.message());
        return new TextCommandOutput(reply);
    }
}

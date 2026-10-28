package dio.budgeting.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class ExecuteVoiceCommandUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteVoiceCommandUseCase.class);

    private final VoiceAssistantGateway voiceAssistantGateway;

    public ExecuteVoiceCommandUseCase(VoiceAssistantGateway voiceAssistantGateway) {
        this.voiceAssistantGateway = voiceAssistantGateway;
    }

    public byte[] execute(Resource audioResource) {
        Assert.notNull(audioResource, "Audio file must not be empty");
        Assert.isTrue(hasContent(audioResource), "Audio file must not be empty");

        var userMessage = voiceAssistantGateway.transcribe(audioResource);
        LOGGER.info("Transcribed voice command: {}", userMessage);

        var assistantReply = voiceAssistantGateway.chat(userMessage);
        return voiceAssistantGateway.synthesize(assistantReply);
    }

    private boolean hasContent(Resource audioResource) {
        try {
            return audioResource.contentLength() > 0;
        } catch (Exception ex) {
            return false;
        }
    }
}

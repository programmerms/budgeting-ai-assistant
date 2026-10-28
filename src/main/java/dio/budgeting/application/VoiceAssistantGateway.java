package dio.budgeting.application;

import org.springframework.core.io.Resource;

public interface VoiceAssistantGateway {
    String transcribe(Resource audioResource);

    String chat(String userMessage);

    byte[] synthesize(String text);
}

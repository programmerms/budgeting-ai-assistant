package dio.budgeting.infrastructure.ai;

import dio.budgeting.application.VoiceAssistantGateway;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class SpringAiVoiceAssistantGateway implements VoiceAssistantGateway {
    private final TranscriptionModel transcriptionModel;
    private final ChatClient chatClient;
    private final TextToSpeechModel textToSpeechModel;

    public SpringAiVoiceAssistantGateway(TranscriptionModel transcriptionModel,
                                         @Value("classpath:prompts/system-message.st") Resource systemPrompt,
                                         ChatClient.Builder chatClientBuilder,
                                         TransactionTools transactionTools,
                                         TextToSpeechModel textToSpeechModel) throws IOException {
        this.transcriptionModel = transcriptionModel;
        this.chatClient = chatClientBuilder
                .defaultSystem(systemPrompt.getContentAsString(StandardCharsets.UTF_8))
                .defaultTools(transactionTools)
                .build();
        this.textToSpeechModel = textToSpeechModel;
    }

    @Override
    public String transcribe(Resource audioResource) {
        return transcriptionModel.transcribe(audioResource);
    }

    @Override
    public String chat(String userMessage) {
        return chatClient.prompt().user(userMessage).call().content();
    }

    @Override
    public byte[] synthesize(String text) {
        return textToSpeechModel.call(text);
    }
}

package dio.budgeting.infrastructure.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static dio.budgeting.support.TestConstants.ASSISTANT_REPLY;
import static dio.budgeting.support.TestConstants.AUDIO_BYTES;
import static dio.budgeting.support.TestConstants.USER_MESSAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpringAiVoiceAssistantGatewayTest {

    @Mock
    private TranscriptionModel transcriptionModel;

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @Mock
    private TransactionTools transactionTools;

    @Mock
    private TextToSpeechModel textToSpeechModel;

    private SpringAiVoiceAssistantGateway gateway;

    @BeforeEach
    void setUp() throws Exception {
        when(chatClientBuilder.defaultSystem(anyString())).thenReturn(chatClientBuilder);
        when(chatClientBuilder.defaultTools(any(Object.class))).thenReturn(chatClientBuilder);
        when(chatClientBuilder.build()).thenReturn(chatClient);

        final var systemPrompt = new ClassPathResource("prompts/system-message.st");
        gateway = new SpringAiVoiceAssistantGateway(
                transcriptionModel,
                systemPrompt,
                chatClientBuilder,
                transactionTools,
                textToSpeechModel);
    }

    @Test
    @DisplayName("Given an audio resource When transcribe is called Then it delegates to TranscriptionModel")
    void givenAudioResourceWhenTranscribeIsCalledThenItDelegatesToTranscriptionModel() {
        final var resource = new ByteArrayResource(AUDIO_BYTES);
        setupGivenAudioResourceWhenTranscribeIsCalledThenItDelegatesToTranscriptionModel(resource);

        final var result = gateway.transcribe(resource);

        verifyGivenAudioResourceWhenTranscribeIsCalledThenItDelegatesToTranscriptionModel(result, resource);
    }

    @Test
    @DisplayName("Given a user message When chat is called Then it delegates to ChatClient and returns the content")
    void givenUserMessageWhenChatIsCalledThenItDelegatesToChatClientAndReturnsContent() {
        setupGivenUserMessageWhenChatIsCalledThenItDelegatesToChatClientAndReturnsContent();

        final var result = gateway.chat(USER_MESSAGE);

        verifyGivenUserMessageWhenChatIsCalledThenItDelegatesToChatClientAndReturnsContent(result);
    }

    @Test
    @DisplayName("Given a text reply When synthesize is called Then it delegates to TextToSpeechModel")
    void givenTextReplyWhenSynthesizeIsCalledThenItDelegatesToTextToSpeechModel() {
        setupGivenTextReplyWhenSynthesizeIsCalledThenItDelegatesToTextToSpeechModel();

        final var result = gateway.synthesize(ASSISTANT_REPLY);

        verifyGivenTextReplyWhenSynthesizeIsCalledThenItDelegatesToTextToSpeechModel(result);
    }

    private void setupGivenAudioResourceWhenTranscribeIsCalledThenItDelegatesToTranscriptionModel(
            final Resource resource) {
        when(transcriptionModel.transcribe(resource)).thenReturn(USER_MESSAGE);
    }

    private void verifyGivenAudioResourceWhenTranscribeIsCalledThenItDelegatesToTranscriptionModel(
            final String actual, final Resource resource) {
        verify(transcriptionModel, times(1)).transcribe(resource);
        assertThat(actual).isEqualTo(USER_MESSAGE);
    }

    private void setupGivenUserMessageWhenChatIsCalledThenItDelegatesToChatClientAndReturnsContent() {
        when(chatClient.prompt().user(USER_MESSAGE).call().content()).thenReturn(ASSISTANT_REPLY);
    }

    private void verifyGivenUserMessageWhenChatIsCalledThenItDelegatesToChatClientAndReturnsContent(
            final String actual) {
        assertThat(actual).isEqualTo(ASSISTANT_REPLY);
    }

    private void setupGivenTextReplyWhenSynthesizeIsCalledThenItDelegatesToTextToSpeechModel() {
        when(textToSpeechModel.call(ASSISTANT_REPLY)).thenReturn(AUDIO_BYTES);
    }

    private void verifyGivenTextReplyWhenSynthesizeIsCalledThenItDelegatesToTextToSpeechModel(
            final byte[] actual) {
        verify(textToSpeechModel, times(1)).call(ASSISTANT_REPLY);
        assertThat(actual).isEqualTo(AUDIO_BYTES);
    }
}

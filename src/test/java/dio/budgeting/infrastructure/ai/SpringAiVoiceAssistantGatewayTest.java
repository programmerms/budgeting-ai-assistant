package dio.budgeting.infrastructure.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.time.LocalDate;
import java.time.ZoneOffset;

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

    @Mock
    private ChatClient chatClient;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient.ChatClientRequestSpec requestSpec;

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
    @DisplayName("Given a user message When chat is called Then it injects the current UTC date into the system prompt and returns the content")
    void givenUserMessageWhenChatIsCalledThenItInjectsCurrentUtcDateIntoSystemPromptAndReturnsContent() {
        final var expectedDate = LocalDate.now(ZoneOffset.UTC).toString();
        setupGivenUserMessageWhenChatIsCalledThenItInjectsCurrentUtcDateIntoSystemPromptAndReturnsContent();

        final var result = gateway.chat(USER_MESSAGE);

        verifyGivenUserMessageWhenChatIsCalledThenItInjectsCurrentUtcDateIntoSystemPromptAndReturnsContent(
                result, expectedDate);
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

    private void setupGivenUserMessageWhenChatIsCalledThenItInjectsCurrentUtcDateIntoSystemPromptAndReturnsContent() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(USER_MESSAGE)).thenReturn(requestSpec);
        when(requestSpec.call().content()).thenReturn(ASSISTANT_REPLY);
    }

    private void verifyGivenUserMessageWhenChatIsCalledThenItInjectsCurrentUtcDateIntoSystemPromptAndReturnsContent(
            final String actual, final String expectedDate) {
        final var systemPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec, times(1)).system(systemPromptCaptor.capture());

        assertThat(systemPromptCaptor.getValue())
                .contains("Você é um assistente financeiro.")
                .contains(expectedDate)
                .doesNotContain("{currentDate}")
                .contains("português do Brasil")
                .contains("reais")
                .contains("esclarecimento");
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

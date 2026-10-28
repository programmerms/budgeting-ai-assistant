package dio.budgeting.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import static dio.budgeting.support.TestConstants.ASSISTANT_REPLY;
import static dio.budgeting.support.TestConstants.AUDIO_BYTES;
import static dio.budgeting.support.TestConstants.USER_MESSAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExecuteVoiceCommandUseCaseTest {

    @Mock
    private VoiceAssistantGateway voiceAssistantGateway;

    @InjectMocks
    private ExecuteVoiceCommandUseCase useCase;

    @Test
    @DisplayName("Given a non-empty audio resource When execute is called Then it transcribes, chats and synthesizes in order")
    void givenNonEmptyAudioResourceWhenExecuteIsCalledThenItTranscribesChatsAndSynthesizesInOrder() {
        final var audioResource = new ByteArrayResource(AUDIO_BYTES);
        setupGivenNonEmptyAudioResourceWhenExecuteIsCalledThenItTranscribesChatsAndSynthesizesInOrder(audioResource);

        final var result = useCase.execute(audioResource);

        verifyGivenNonEmptyAudioResourceWhenExecuteIsCalledThenItTranscribesChatsAndSynthesizesInOrder(
                result, audioResource);
    }

    @Test
    @DisplayName("Given an empty audio resource When execute is called Then it throws IllegalArgumentException and does not call the gateway")
    void givenEmptyAudioResourceWhenExecuteIsCalledThenItThrowsIllegalArgumentExceptionAndDoesNotCallGateway() {
        final var emptyResource = new ByteArrayResource(new byte[0]);

        final var thrown = catchThrowable(() -> useCase.execute(emptyResource));

        verifyGivenInvalidAudioResourceWhenExecuteIsCalledThenItThrowsIllegalArgumentExceptionAndDoesNotCallGateway(thrown);
    }

    @Test
    @DisplayName("Given a null audio resource When execute is called Then it throws IllegalArgumentException and does not call the gateway")
    void givenNullAudioResourceWhenExecuteIsCalledThenItThrowsIllegalArgumentExceptionAndDoesNotCallGateway() {
        final var thrown = catchThrowable(() -> useCase.execute(null));

        verifyGivenInvalidAudioResourceWhenExecuteIsCalledThenItThrowsIllegalArgumentExceptionAndDoesNotCallGateway(thrown);
    }

    private void setupGivenNonEmptyAudioResourceWhenExecuteIsCalledThenItTranscribesChatsAndSynthesizesInOrder(
            final Resource audioResource) {
        when(voiceAssistantGateway.transcribe(audioResource)).thenReturn(USER_MESSAGE);
        when(voiceAssistantGateway.chat(USER_MESSAGE)).thenReturn(ASSISTANT_REPLY);
        when(voiceAssistantGateway.synthesize(ASSISTANT_REPLY)).thenReturn(AUDIO_BYTES);
    }

    private void verifyGivenNonEmptyAudioResourceWhenExecuteIsCalledThenItTranscribesChatsAndSynthesizesInOrder(
            final byte[] actual, final Resource audioResource) {
        final InOrder inOrder = inOrder(voiceAssistantGateway);
        inOrder.verify(voiceAssistantGateway).transcribe(audioResource);
        inOrder.verify(voiceAssistantGateway).chat(USER_MESSAGE);
        inOrder.verify(voiceAssistantGateway).synthesize(ASSISTANT_REPLY);
        assertThat(actual).isEqualTo(AUDIO_BYTES);
    }

    private void verifyGivenInvalidAudioResourceWhenExecuteIsCalledThenItThrowsIllegalArgumentExceptionAndDoesNotCallGateway(
            final Throwable actual) {
        assertThat(actual)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Audio file must not be empty");
        verifyNoInteractions(voiceAssistantGateway);
    }
}

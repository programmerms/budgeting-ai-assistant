package dio.budgeting.application;

import dio.budgeting.application.input.TextCommandInput;
import dio.budgeting.application.output.TextCommandOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static dio.budgeting.support.TestConstants.ASSISTANT_REPLY;
import static dio.budgeting.support.TestConstants.USER_MESSAGE;
import static dio.budgeting.support.input.TextCommandInputSupport.defaultTextCommandInput;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.instancio.Select.field;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExecuteTextCommandUseCaseTest {

    @Mock
    private VoiceAssistantGateway voiceAssistantGateway;

    @InjectMocks
    private ExecuteTextCommandUseCase useCase;

    @Test
    @DisplayName("Given a valid text command input When execute is called Then it delegates to the gateway and returns the reply")
    void givenValidTextCommandInputWhenExecuteIsCalledThenItDelegatesToGatewayAndReturnsReply() {
        final var input = defaultTextCommandInput().create();
        setupGivenValidTextCommandInputWhenExecuteIsCalledThenItDelegatesToGatewayAndReturnsReply();

        final var result = useCase.execute(input);

        verifyGivenValidTextCommandInputWhenExecuteIsCalledThenItDelegatesToGatewayAndReturnsReply(result);
    }

    @Test
    @DisplayName("Given a null text command input When execute is called Then it throws IllegalArgumentException")
    void givenNullTextCommandInputWhenExecuteIsCalledThenItThrowsIllegalArgumentException() {
        final var thrown = catchThrowable(() -> useCase.execute(null));

        verifyGivenNullTextCommandInputWhenExecuteIsCalledThenItThrowsIllegalArgumentException(thrown);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    @DisplayName("Given a blank message When execute is called Then it throws IllegalArgumentException")
    void givenBlankMessageWhenExecuteIsCalledThenItThrowsIllegalArgumentException(final String invalidMessage) {
        final var input = defaultTextCommandInput()
                .set(field(TextCommandInput::message), invalidMessage)
                .create();

        final var thrown = catchThrowable(() -> useCase.execute(input));

        verifyGivenBlankMessageWhenExecuteIsCalledThenItThrowsIllegalArgumentException(thrown);
    }

    private void setupGivenValidTextCommandInputWhenExecuteIsCalledThenItDelegatesToGatewayAndReturnsReply() {
        when(voiceAssistantGateway.chat(USER_MESSAGE)).thenReturn(ASSISTANT_REPLY);
    }

    private void verifyGivenValidTextCommandInputWhenExecuteIsCalledThenItDelegatesToGatewayAndReturnsReply(
            final TextCommandOutput actual) {
        verify(voiceAssistantGateway, times(1)).chat(USER_MESSAGE);
        assertThat(actual).isEqualTo(new TextCommandOutput(ASSISTANT_REPLY));
    }

    private void verifyGivenNullTextCommandInputWhenExecuteIsCalledThenItThrowsIllegalArgumentException(
            final Throwable actual) {
        assertThat(actual)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Text command input must not be null");
        verifyNoInteractions(voiceAssistantGateway);
    }

    private void verifyGivenBlankMessageWhenExecuteIsCalledThenItThrowsIllegalArgumentException(
            final Throwable actual) {
        assertThat(actual)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Message must not be blank");
        verifyNoInteractions(voiceAssistantGateway);
    }
}

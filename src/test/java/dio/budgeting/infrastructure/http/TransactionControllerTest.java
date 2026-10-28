package dio.budgeting.infrastructure.http;

import dio.budgeting.application.ExecuteTextCommandUseCase;
import dio.budgeting.application.ExecuteVoiceCommandUseCase;
import dio.budgeting.application.ListTransactionsByCategoryUseCase;
import dio.budgeting.application.ListTransactionsByPeriodUseCase;
import dio.budgeting.application.PersistTransactionUseCase;
import dio.budgeting.application.SummarizeTransactionsByCategoryUseCase;
import dio.budgeting.application.input.PersistTransactionInput;
import dio.budgeting.application.input.TextCommandInput;
import dio.budgeting.application.input.TransactionsPeriodInput;
import dio.budgeting.application.output.CategorySummaryOutput;
import dio.budgeting.application.output.TextCommandOutput;
import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.infrastructure.http.request.TextCommandRequest;
import dio.budgeting.infrastructure.http.request.TransactionRequest;
import dio.budgeting.infrastructure.http.response.CategorySummaryResponse;
import dio.budgeting.infrastructure.http.response.TextCommandResponse;
import dio.budgeting.infrastructure.http.response.TransactionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static dio.budgeting.support.TestConstants.AMOUNT_IN_REAIS;
import static dio.budgeting.support.TestConstants.ASSISTANT_REPLY;
import static dio.budgeting.support.TestConstants.AUDIO_BYTES;
import static dio.budgeting.support.TestConstants.CATEGORY;
import static dio.budgeting.support.TestConstants.PERIOD_FROM;
import static dio.budgeting.support.TestConstants.PERIOD_TO;
import static dio.budgeting.support.domain.TransactionSupport.defaultTransaction;
import static dio.budgeting.support.request.TextCommandRequestSupport.defaultTextCommandRequest;
import static dio.budgeting.support.request.TransactionRequestSupport.defaultTransactionRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private PersistTransactionUseCase persistTransactionUseCase;

    @Mock
    private ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase;

    @Mock
    private SummarizeTransactionsByCategoryUseCase summarizeTransactionsByCategoryUseCase;

    @Mock
    private ListTransactionsByPeriodUseCase listTransactionsByPeriodUseCase;

    @Mock
    private ExecuteVoiceCommandUseCase executeVoiceCommandUseCase;

    @Mock
    private ExecuteTextCommandUseCase executeTextCommandUseCase;

    @InjectMocks
    private TransactionController controller;

    @Test
    @DisplayName("Given a transaction request When createTransaction is called Then it delegates to PersistTransactionUseCase and returns the response")
    void givenTransactionRequestWhenCreateTransactionIsCalledThenItDelegatesToUseCaseAndReturnsResponse() {
        final var request = defaultTransactionRequest().create();
        final var output = TransactionOutput.from(defaultTransaction().create());
        setupGivenTransactionRequestWhenCreateTransactionIsCalledThenItDelegatesToUseCaseAndReturnsResponse(output);

        final var result = controller.createTransaction(request);

        verifyGivenTransactionRequestWhenCreateTransactionIsCalledThenItDelegatesToUseCaseAndReturnsResponse(
                result, request, output);
    }

    @Test
    @DisplayName("Given a category When summarizeTransactionsByCategory is called Then it delegates to SummarizeTransactionsByCategoryUseCase and returns the summary")
    void givenCategoryWhenSummarizeTransactionsByCategoryIsCalledThenItDelegatesToUseCaseAndReturnsSummary() {
        final var output = new CategorySummaryOutput(CATEGORY.name(), AMOUNT_IN_REAIS, 2L);
        setupGivenCategoryWhenSummarizeTransactionsByCategoryIsCalledThenItDelegatesToUseCaseAndReturnsSummary(output);

        final var result = controller.summarizeTransactionsByCategory(CATEGORY);

        verifyGivenCategoryWhenSummarizeTransactionsByCategoryIsCalledThenItDelegatesToUseCaseAndReturnsSummary(
                result, output);
    }

    @Test
    @DisplayName("Given a period When readTransactionsByPeriod is called Then it delegates to ListTransactionsByPeriodUseCase and returns responses")
    void givenPeriodWhenReadTransactionsByPeriodIsCalledThenItDelegatesToUseCaseAndReturnsResponses() {
        final var output = TransactionOutput.from(defaultTransaction().create());
        setupGivenPeriodWhenReadTransactionsByPeriodIsCalledThenItDelegatesToUseCaseAndReturnsResponses(output);

        final var result = controller.readTransactionsByPeriod(PERIOD_FROM, PERIOD_TO);

        verifyGivenPeriodWhenReadTransactionsByPeriodIsCalledThenItDelegatesToUseCaseAndReturnsResponses(result, output);
    }

    @Test
    @DisplayName("Given a category When readTransactions is called Then it delegates to ListTransactionsByCategoryUseCase and returns responses")
    void givenCategoryWhenReadTransactionsIsCalledThenItDelegatesToUseCaseAndReturnsResponses() {
        final var output = TransactionOutput.from(defaultTransaction().create());
        setupGivenCategoryWhenReadTransactionsIsCalledThenItDelegatesToUseCaseAndReturnsResponses(output);

        final var result = controller.readTransactions(CATEGORY);

        verifyGivenCategoryWhenReadTransactionsIsCalledThenItDelegatesToUseCaseAndReturnsResponses(result, output);
    }

    @Test
    @DisplayName("Given an audio file When transcribe is called Then it delegates to ExecuteVoiceCommandUseCase and returns audio.mp3 attachment")
    void givenAudioFileWhenTranscribeIsCalledThenItDelegatesToUseCaseAndReturnsAudioMp3Attachment() throws Exception {
        final var file = new MockMultipartFile("file", "command.m4a", "audio/m4a", AUDIO_BYTES);
        setupGivenAudioFileWhenTranscribeIsCalledThenItDelegatesToUseCaseAndReturnsAudioMp3Attachment();

        final var result = controller.transcribe(file);

        verifyGivenAudioFileWhenTranscribeIsCalledThenItDelegatesToUseCaseAndReturnsAudioMp3Attachment(result);
    }

    @Test
    @DisplayName("Given a text command request When executeTextCommand is called Then it delegates to ExecuteTextCommandUseCase and returns the reply")
    void givenTextCommandRequestWhenExecuteTextCommandIsCalledThenItDelegatesToUseCaseAndReturnsReply() {
        final var request = defaultTextCommandRequest().create();
        final var output = new TextCommandOutput(ASSISTANT_REPLY);
        setupGivenTextCommandRequestWhenExecuteTextCommandIsCalledThenItDelegatesToUseCaseAndReturnsReply(output);

        final var result = controller.executeTextCommand(request);

        verifyGivenTextCommandRequestWhenExecuteTextCommandIsCalledThenItDelegatesToUseCaseAndReturnsReply(
                result, request, output);
    }

    private void setupGivenTransactionRequestWhenCreateTransactionIsCalledThenItDelegatesToUseCaseAndReturnsResponse(
            final TransactionOutput output) {
        when(persistTransactionUseCase.execute(any(PersistTransactionInput.class))).thenReturn(output);
    }

    private void verifyGivenTransactionRequestWhenCreateTransactionIsCalledThenItDelegatesToUseCaseAndReturnsResponse(
            final TransactionResponse actual, final TransactionRequest request, final TransactionOutput expected) {
        verify(persistTransactionUseCase, times(1)).execute(request.toInput());
        assertThat(actual).isEqualTo(TransactionResponse.from(expected));
    }

    private void setupGivenCategoryWhenSummarizeTransactionsByCategoryIsCalledThenItDelegatesToUseCaseAndReturnsSummary(
            final CategorySummaryOutput output) {
        when(summarizeTransactionsByCategoryUseCase.execute(CATEGORY)).thenReturn(output);
    }

    private void verifyGivenCategoryWhenSummarizeTransactionsByCategoryIsCalledThenItDelegatesToUseCaseAndReturnsSummary(
            final CategorySummaryResponse actual, final CategorySummaryOutput expected) {
        verify(summarizeTransactionsByCategoryUseCase, times(1)).execute(CATEGORY);
        assertThat(actual).isEqualTo(CategorySummaryResponse.from(expected));
    }

    private void setupGivenPeriodWhenReadTransactionsByPeriodIsCalledThenItDelegatesToUseCaseAndReturnsResponses(
            final TransactionOutput output) {
        when(listTransactionsByPeriodUseCase.execute(new TransactionsPeriodInput(PERIOD_FROM, PERIOD_TO)))
                .thenReturn(List.of(output));
    }

    private void verifyGivenPeriodWhenReadTransactionsByPeriodIsCalledThenItDelegatesToUseCaseAndReturnsResponses(
            final List<TransactionResponse> actual, final TransactionOutput expected) {
        verify(listTransactionsByPeriodUseCase, times(1))
                .execute(new TransactionsPeriodInput(PERIOD_FROM, PERIOD_TO));
        assertThat(actual).containsExactly(TransactionResponse.from(expected));
    }

    private void setupGivenCategoryWhenReadTransactionsIsCalledThenItDelegatesToUseCaseAndReturnsResponses(
            final TransactionOutput output) {
        when(listTransactionsByCategoryUseCase.execute(CATEGORY)).thenReturn(List.of(output));
    }

    private void verifyGivenCategoryWhenReadTransactionsIsCalledThenItDelegatesToUseCaseAndReturnsResponses(
            final List<TransactionResponse> actual, final TransactionOutput expected) {
        verify(listTransactionsByCategoryUseCase, times(1)).execute(CATEGORY);
        assertThat(actual).containsExactly(TransactionResponse.from(expected));
    }

    private void setupGivenAudioFileWhenTranscribeIsCalledThenItDelegatesToUseCaseAndReturnsAudioMp3Attachment() {
        when(executeVoiceCommandUseCase.execute(any(Resource.class))).thenReturn(AUDIO_BYTES);
    }

    private void verifyGivenAudioFileWhenTranscribeIsCalledThenItDelegatesToUseCaseAndReturnsAudioMp3Attachment(
            final ResponseEntity<Resource> actual) throws Exception {
        verify(executeVoiceCommandUseCase, times(1)).execute(any(Resource.class));
        assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(actual.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"audio.mp3\"");
        assertThat(actual.getBody()).isNotNull();
        assertThat(actual.getBody().getContentAsByteArray()).isEqualTo(AUDIO_BYTES);
    }

    private void setupGivenTextCommandRequestWhenExecuteTextCommandIsCalledThenItDelegatesToUseCaseAndReturnsReply(
            final TextCommandOutput output) {
        when(executeTextCommandUseCase.execute(any(TextCommandInput.class))).thenReturn(output);
    }

    private void verifyGivenTextCommandRequestWhenExecuteTextCommandIsCalledThenItDelegatesToUseCaseAndReturnsReply(
            final TextCommandResponse actual, final TextCommandRequest request, final TextCommandOutput expected) {
        verify(executeTextCommandUseCase, times(1)).execute(request.toInput());
        assertThat(actual).isEqualTo(TextCommandResponse.from(expected));
    }
}

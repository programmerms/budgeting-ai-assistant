package dio.budgeting.infrastructure.ai;

import dio.budgeting.application.ListTransactionsByCategoryUseCase;
import dio.budgeting.application.ListTransactionsByPeriodUseCase;
import dio.budgeting.application.PersistTransactionUseCase;
import dio.budgeting.application.SummarizeTransactionsByCategoryUseCase;
import dio.budgeting.application.input.PersistTransactionInput;
import dio.budgeting.application.input.TransactionsPeriodInput;
import dio.budgeting.application.output.CategorySummaryOutput;
import dio.budgeting.application.output.TransactionOutput;
import dio.budgeting.infrastructure.ai.request.PersistTransactionToolRequest;
import dio.budgeting.infrastructure.ai.request.TransactionsPeriodToolRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static dio.budgeting.support.TestConstants.AMOUNT_IN_REAIS;
import static dio.budgeting.support.TestConstants.CATEGORY;
import static dio.budgeting.support.domain.TransactionSupport.defaultTransaction;
import static dio.budgeting.support.request.PersistTransactionToolRequestSupport.defaultPersistTransactionToolRequest;
import static dio.budgeting.support.request.TransactionsPeriodToolRequestSupport.defaultTransactionsPeriodToolRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionToolsTest {

    @Mock
    private PersistTransactionUseCase persistTransactionUseCase;

    @Mock
    private ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase;

    @Mock
    private SummarizeTransactionsByCategoryUseCase summarizeTransactionsByCategoryUseCase;

    @Mock
    private ListTransactionsByPeriodUseCase listTransactionsByPeriodUseCase;

    @InjectMocks
    private TransactionTools transactionTools;

    @Test
    @DisplayName("Given a tool request When persistTransaction is called Then the use case receives the converted input and its output is returned")
    void givenToolRequestWhenPersistTransactionIsCalledThenTheUseCaseReceivesTheConvertedInputAndItsOutputIsReturned() {
        final var request = defaultPersistTransactionToolRequest().create();
        final var output = TransactionOutput.from(defaultTransaction().create());
        setupGivenToolRequestWhenPersistTransactionIsCalledThenTheUseCaseReceivesTheConvertedInputAndItsOutputIsReturned(output);

        final var result = transactionTools.persistTransaction(request);

        verifyGivenToolRequestWhenPersistTransactionIsCalledThenTheUseCaseReceivesTheConvertedInputAndItsOutputIsReturned(
                result, request, output);
    }

    @Test
    @DisplayName("Given a category When listTransactionsByCategory is called Then the use case receives the category and its outputs are returned")
    void givenCategoryWhenListTransactionsByCategoryIsCalledThenTheUseCaseReceivesTheCategoryAndItsOutputsAreReturned() {
        final var outputs = List.of(TransactionOutput.from(defaultTransaction().create()));
        setupGivenCategoryWhenListTransactionsByCategoryIsCalledThenTheUseCaseReceivesTheCategoryAndItsOutputsAreReturned(outputs);

        final var result = transactionTools.listTransactionsByCategory(CATEGORY);

        verifyGivenCategoryWhenListTransactionsByCategoryIsCalledThenTheUseCaseReceivesTheCategoryAndItsOutputsAreReturned(
                result, outputs);
    }

    @Test
    @DisplayName("Given a category When summarizeTransactionsByCategory is called Then the use case receives the category and its summary is returned")
    void givenCategoryWhenSummarizeTransactionsByCategoryIsCalledThenTheUseCaseReceivesTheCategoryAndItsSummaryIsReturned() {
        final var summary = new CategorySummaryOutput(CATEGORY.name(), AMOUNT_IN_REAIS, 1L);
        setupGivenCategoryWhenSummarizeTransactionsByCategoryIsCalledThenTheUseCaseReceivesTheCategoryAndItsSummaryIsReturned(summary);

        final var result = transactionTools.summarizeTransactionsByCategory(CATEGORY);

        verifyGivenCategoryWhenSummarizeTransactionsByCategoryIsCalledThenTheUseCaseReceivesTheCategoryAndItsSummaryIsReturned(
                result, summary);
    }

    @Test
    @DisplayName("Given a period tool request When listTransactionsByPeriod is called Then the use case receives the converted input and its outputs are returned")
    void givenPeriodToolRequestWhenListTransactionsByPeriodIsCalledThenTheUseCaseReceivesTheConvertedInputAndItsOutputsAreReturned() {
        final var request = defaultTransactionsPeriodToolRequest().create();
        final var outputs = List.of(TransactionOutput.from(defaultTransaction().create()));
        setupGivenPeriodToolRequestWhenListTransactionsByPeriodIsCalledThenTheUseCaseReceivesTheConvertedInputAndItsOutputsAreReturned(outputs);

        final var result = transactionTools.listTransactionsByPeriod(request);

        verifyGivenPeriodToolRequestWhenListTransactionsByPeriodIsCalledThenTheUseCaseReceivesTheConvertedInputAndItsOutputsAreReturned(
                result, request, outputs);
    }

    private void setupGivenToolRequestWhenPersistTransactionIsCalledThenTheUseCaseReceivesTheConvertedInputAndItsOutputIsReturned(
            final TransactionOutput output) {
        when(persistTransactionUseCase.execute(any(PersistTransactionInput.class))).thenReturn(output);
    }

    private void verifyGivenToolRequestWhenPersistTransactionIsCalledThenTheUseCaseReceivesTheConvertedInputAndItsOutputIsReturned(
            final TransactionOutput actual, final PersistTransactionToolRequest request, final TransactionOutput expected) {
        final var captor = ArgumentCaptor.forClass(PersistTransactionInput.class);
        verify(persistTransactionUseCase, times(1)).execute(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new PersistTransactionInput(
                request.description(), request.amount(), request.category()));

        assertThat(actual).isSameAs(expected);
    }

    private void setupGivenCategoryWhenListTransactionsByCategoryIsCalledThenTheUseCaseReceivesTheCategoryAndItsOutputsAreReturned(
            final List<TransactionOutput> outputs) {
        when(listTransactionsByCategoryUseCase.execute(CATEGORY)).thenReturn(outputs);
    }

    private void verifyGivenCategoryWhenListTransactionsByCategoryIsCalledThenTheUseCaseReceivesTheCategoryAndItsOutputsAreReturned(
            final List<TransactionOutput> actual, final List<TransactionOutput> expected) {
        verify(listTransactionsByCategoryUseCase, times(1)).execute(CATEGORY);
        assertThat(actual).isSameAs(expected);
    }

    private void setupGivenCategoryWhenSummarizeTransactionsByCategoryIsCalledThenTheUseCaseReceivesTheCategoryAndItsSummaryIsReturned(
            final CategorySummaryOutput summary) {
        when(summarizeTransactionsByCategoryUseCase.execute(CATEGORY)).thenReturn(summary);
    }

    private void verifyGivenCategoryWhenSummarizeTransactionsByCategoryIsCalledThenTheUseCaseReceivesTheCategoryAndItsSummaryIsReturned(
            final CategorySummaryOutput actual, final CategorySummaryOutput expected) {
        verify(summarizeTransactionsByCategoryUseCase, times(1)).execute(CATEGORY);
        assertThat(actual).isSameAs(expected);
    }

    private void setupGivenPeriodToolRequestWhenListTransactionsByPeriodIsCalledThenTheUseCaseReceivesTheConvertedInputAndItsOutputsAreReturned(
            final List<TransactionOutput> outputs) {
        when(listTransactionsByPeriodUseCase.execute(any(TransactionsPeriodInput.class))).thenReturn(outputs);
    }

    private void verifyGivenPeriodToolRequestWhenListTransactionsByPeriodIsCalledThenTheUseCaseReceivesTheConvertedInputAndItsOutputsAreReturned(
            final List<TransactionOutput> actual, final TransactionsPeriodToolRequest request, final List<TransactionOutput> expected) {
        final var captor = ArgumentCaptor.forClass(TransactionsPeriodInput.class);
        verify(listTransactionsByPeriodUseCase, times(1)).execute(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new TransactionsPeriodInput(request.from(), request.to()));

        assertThat(actual).isSameAs(expected);
    }
}

package dio.budgeting.infrastructure.ai.request;

import dio.budgeting.application.input.PersistTransactionInput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dio.budgeting.support.request.PersistTransactionToolRequestSupport.defaultPersistTransactionToolRequest;
import static org.assertj.core.api.Assertions.assertThat;

class PersistTransactionToolRequestTest {

    @Test
    @DisplayName("Given a tool request When converted to input Then description, amount and category are kept")
    void givenToolRequestWhenConvertedToInputThenDescriptionAmountAndCategoryAreKept() {
        final var request = defaultPersistTransactionToolRequest().create();

        final var actual = request.toInput();

        verifyGivenToolRequestWhenConvertedToInputThenDescriptionAmountAndCategoryAreKept(actual, request);
    }

    private void verifyGivenToolRequestWhenConvertedToInputThenDescriptionAmountAndCategoryAreKept(
            final PersistTransactionInput actual, final PersistTransactionToolRequest expected) {
        assertThat(actual).isEqualTo(new PersistTransactionInput(
                expected.description(), expected.amount(), expected.category()));
    }
}

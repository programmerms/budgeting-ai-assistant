package dio.budgeting.infrastructure.ai;

import dio.budgeting.application.ListTransactionsByCategoryUseCase;
import dio.budgeting.application.ListTransactionsByPeriodUseCase;
import dio.budgeting.application.PersistTransactionUseCase;
import dio.budgeting.application.SummarizeTransactionsByCategoryUseCase;
import dio.budgeting.infrastructure.security.AuthenticatedOwnerProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TransactionToolsContractTest {

    private static final String PERSIST_TRANSACTION_SCHEMA = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "input": {
                  "type": "object",
                  "properties": {
                    "amount": {
                      "type": "integer",
                      "format": "int64",
                      "description": "Valor do gasto (em centavos)"
                    },
                    "category": {
                      "type": "string",
                      "enum": ["GROCERIES", "PHARMA", "AUTO", "RESTAURANT", "SHOPPING", "TRANSPORT", "HEALTH", "LEISURE", "OTHER"],
                      "description": "Categoria de uma transação"
                    },
                    "description": {
                      "type": "string",
                      "description": "Descrição do gasto"
                    }
                  },
                  "required": ["amount", "category", "description"]
                }
              },
              "required": ["input"],
              "additionalProperties": false
            }
            """;

    private static final String LIST_TRANSACTIONS_BY_CATEGORY_SCHEMA = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "category": {
                  "type": "string",
                  "enum": ["GROCERIES", "PHARMA", "AUTO", "RESTAURANT", "SHOPPING", "TRANSPORT", "HEALTH", "LEISURE", "OTHER"],
                  "description": "Categoria de uma transação"
                }
              },
              "required": ["category"],
              "additionalProperties": false
            }
            """;

    private static final String SUMMARIZE_TRANSACTIONS_BY_CATEGORY_SCHEMA = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "category": {
                  "type": "string",
                  "enum": ["GROCERIES", "PHARMA", "AUTO", "RESTAURANT", "SHOPPING", "TRANSPORT", "HEALTH", "LEISURE", "OTHER"],
                  "description": "Categoria de uma transação"
                }
              },
              "required": ["category"],
              "additionalProperties": false
            }
            """;

    private static final String LIST_TRANSACTIONS_BY_PERIOD_SCHEMA = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "input": {
                  "type": "object",
                  "properties": {
                    "from": {
                      "type": "string",
                      "format": "date",
                      "description": "Data inicial no formato YYYY-MM-DD"
                    },
                    "to": {
                      "type": "string",
                      "format": "date",
                      "description": "Data final no formato YYYY-MM-DD"
                    }
                  },
                  "required": ["from", "to"]
                }
              },
              "required": ["input"],
              "additionalProperties": false
            }
            """;

    @Mock
    private PersistTransactionUseCase persistTransactionUseCase;

    @Mock
    private ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase;

    @Mock
    private SummarizeTransactionsByCategoryUseCase summarizeTransactionsByCategoryUseCase;

    @Mock
    private ListTransactionsByPeriodUseCase listTransactionsByPeriodUseCase;

    @Mock
    private AuthenticatedOwnerProvider authenticatedOwnerProvider;

    @InjectMocks
    private TransactionTools transactionTools;

    @Test
    @DisplayName("Given the registered tools When reading the persist transaction definition Then name, description and schema are unchanged")
    void givenRegisteredToolsWhenReadingThePersistTransactionDefinitionThenNameDescriptionAndSchemaAreUnchanged() throws Exception {
        final var actual = findToolDefinition("persist-transaction");

        verifyGivenRegisteredToolsWhenReadingThePersistTransactionDefinitionThenNameDescriptionAndSchemaAreUnchanged(actual);
    }

    @Test
    @DisplayName("Given the registered tools When reading the list by category definition Then name, description and schema are unchanged")
    void givenRegisteredToolsWhenReadingTheListByCategoryDefinitionThenNameDescriptionAndSchemaAreUnchanged() throws Exception {
        final var actual = findToolDefinition("list-transactions-by-category");

        verifyGivenRegisteredToolsWhenReadingTheListByCategoryDefinitionThenNameDescriptionAndSchemaAreUnchanged(actual);
    }

    @Test
    @DisplayName("Given the registered tools When reading the summarize by category definition Then name, description and schema match")
    void givenRegisteredToolsWhenReadingTheSummarizeByCategoryDefinitionThenNameDescriptionAndSchemaMatch() throws Exception {
        final var actual = findToolDefinition("summarize-transactions-by-category");

        verifyGivenRegisteredToolsWhenReadingTheSummarizeByCategoryDefinitionThenNameDescriptionAndSchemaMatch(actual);
    }

    @Test
    @DisplayName("Given the registered tools When reading the list by period definition Then name, description and schema match")
    void givenRegisteredToolsWhenReadingTheListByPeriodDefinitionThenNameDescriptionAndSchemaMatch() throws Exception {
        final var actual = findToolDefinition("list-transactions-by-period");

        verifyGivenRegisteredToolsWhenReadingTheListByPeriodDefinitionThenNameDescriptionAndSchemaMatch(actual);
    }

    @Test
    @DisplayName("Given the registered tools When reading their schemas Then no owner parameter is exposed")
    void givenRegisteredToolsWhenReadingTheirSchemasThenNoOwnerParameterIsExposed() {
        final var actual = Arrays.stream(ToolCallbacks.from(transactionTools))
                .map(callback -> callback.getToolDefinition().inputSchema())
                .toList();

        verifyGivenRegisteredToolsWhenReadingTheirSchemasThenNoOwnerParameterIsExposed(actual);
    }

    private void verifyGivenRegisteredToolsWhenReadingThePersistTransactionDefinitionThenNameDescriptionAndSchemaAreUnchanged(
            final ToolDefinition actual) throws Exception {
        assertThat(actual.description()).isEqualTo("Persiste uma nova transação financeira");
        JSONAssert.assertEquals(PERSIST_TRANSACTION_SCHEMA, actual.inputSchema(), JSONCompareMode.STRICT);
    }

    private void verifyGivenRegisteredToolsWhenReadingTheListByCategoryDefinitionThenNameDescriptionAndSchemaAreUnchanged(
            final ToolDefinition actual) throws Exception {
        assertThat(actual.description()).isEqualTo("Lista transações financeiras por categoria");
        JSONAssert.assertEquals(LIST_TRANSACTIONS_BY_CATEGORY_SCHEMA, actual.inputSchema(), JSONCompareMode.STRICT);
    }

    private void verifyGivenRegisteredToolsWhenReadingTheSummarizeByCategoryDefinitionThenNameDescriptionAndSchemaMatch(
            final ToolDefinition actual) throws Exception {
        assertThat(actual.description()).isEqualTo("Resume o total gasto e a quantidade de transações de uma categoria");
        JSONAssert.assertEquals(SUMMARIZE_TRANSACTIONS_BY_CATEGORY_SCHEMA, actual.inputSchema(), JSONCompareMode.STRICT);
    }

    private void verifyGivenRegisteredToolsWhenReadingTheListByPeriodDefinitionThenNameDescriptionAndSchemaMatch(
            final ToolDefinition actual) throws Exception {
        assertThat(actual.description()).isEqualTo("Lista transações financeiras realizadas em um período de datas");
        JSONAssert.assertEquals(LIST_TRANSACTIONS_BY_PERIOD_SCHEMA, actual.inputSchema(), JSONCompareMode.STRICT);
    }

    private void verifyGivenRegisteredToolsWhenReadingTheirSchemasThenNoOwnerParameterIsExposed(
            final java.util.List<String> actual) {
        assertThat(actual).allSatisfy(schema -> assertThat(schema.toLowerCase()).doesNotContain("owner"));
    }

    private ToolDefinition findToolDefinition(final String name) {
        return Arrays.stream(ToolCallbacks.from(transactionTools))
                .map(callback -> callback.getToolDefinition())
                .filter(definition -> definition.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Tool not registered: " + name));
    }
}

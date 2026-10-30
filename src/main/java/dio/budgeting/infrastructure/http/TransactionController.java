package dio.budgeting.infrastructure.http;

import dio.budgeting.application.ExecuteTextCommandUseCase;
import dio.budgeting.application.ExecuteVoiceCommandUseCase;
import dio.budgeting.application.ListTransactionsByCategoryUseCase;
import dio.budgeting.application.ListTransactionsByPeriodUseCase;
import dio.budgeting.application.PersistTransactionUseCase;
import dio.budgeting.application.SummarizeTransactionsByCategoryUseCase;
import dio.budgeting.application.input.TransactionsPeriodInput;
import dio.budgeting.domain.Category;
import dio.budgeting.infrastructure.http.request.TextCommandRequest;
import dio.budgeting.infrastructure.http.request.TransactionRequest;
import dio.budgeting.infrastructure.http.response.CategorySummaryResponse;
import dio.budgeting.infrastructure.http.response.TextCommandResponse;
import dio.budgeting.infrastructure.http.response.TransactionResponse;
import dio.budgeting.infrastructure.security.AuthenticatedOwnerProvider;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {
    private final PersistTransactionUseCase persistTransactionUseCase;
    private final ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase;
    private final SummarizeTransactionsByCategoryUseCase summarizeTransactionsByCategoryUseCase;
    private final ListTransactionsByPeriodUseCase listTransactionsByPeriodUseCase;
    private final ExecuteVoiceCommandUseCase executeVoiceCommandUseCase;
    private final ExecuteTextCommandUseCase executeTextCommandUseCase;
    private final AuthenticatedOwnerProvider authenticatedOwnerProvider;

    public TransactionController(PersistTransactionUseCase persistTransactionUseCase,
                                 ListTransactionsByCategoryUseCase listTransactionsByCategoryUseCase,
                                 SummarizeTransactionsByCategoryUseCase summarizeTransactionsByCategoryUseCase,
                                 ListTransactionsByPeriodUseCase listTransactionsByPeriodUseCase,
                                 ExecuteVoiceCommandUseCase executeVoiceCommandUseCase,
                                 ExecuteTextCommandUseCase executeTextCommandUseCase,
                                 AuthenticatedOwnerProvider authenticatedOwnerProvider) {
        this.persistTransactionUseCase = persistTransactionUseCase;
        this.listTransactionsByCategoryUseCase = listTransactionsByCategoryUseCase;
        this.summarizeTransactionsByCategoryUseCase = summarizeTransactionsByCategoryUseCase;
        this.listTransactionsByPeriodUseCase = listTransactionsByPeriodUseCase;
        this.executeVoiceCommandUseCase = executeVoiceCommandUseCase;
        this.executeTextCommandUseCase = executeTextCommandUseCase;
        this.authenticatedOwnerProvider = authenticatedOwnerProvider;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse createTransaction(@RequestBody @Valid TransactionRequest request) {
        var transaction = persistTransactionUseCase.execute(
                authenticatedOwnerProvider.currentOwnerId(), request.toInput());
        return TransactionResponse.from(transaction);
    }

    @GetMapping("/summary")
    public CategorySummaryResponse summarizeTransactionsByCategory(@RequestParam Category category) {
        var summary = summarizeTransactionsByCategoryUseCase.execute(
                authenticatedOwnerProvider.currentOwnerId(), category);
        return CategorySummaryResponse.from(summary);
    }

    @GetMapping
    public List<TransactionResponse> readTransactionsByPeriod(@RequestParam LocalDate from,
                                                              @RequestParam LocalDate to) {
        return listTransactionsByPeriodUseCase.execute(
                        authenticatedOwnerProvider.currentOwnerId(), new TransactionsPeriodInput(from, to))
                .stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @GetMapping("/{category}")
    public List<TransactionResponse> readTransactions(@PathVariable Category category) {
        return listTransactionsByCategoryUseCase.execute(
                        authenticatedOwnerProvider.currentOwnerId(), category)
                .stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @PostMapping(value = "/ai", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "audio/mp3")
    ResponseEntity<Resource> transcribe(@RequestParam("file") MultipartFile file) {
        Assert.isTrue(!file.isEmpty(), "Audio file must not be empty");
        byte[] audio = executeVoiceCommandUseCase.execute(file.getResource());
        var resource = new ByteArrayResource(audio);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("audio.mp3")
                                .build()
                                .toString())
                .body(resource);
    }

    @PostMapping("/ai/text")
    public TextCommandResponse executeTextCommand(@RequestBody @Valid TextCommandRequest request) {
        var output = executeTextCommandUseCase.execute(request.toInput());
        return TextCommandResponse.from(output);
    }
}

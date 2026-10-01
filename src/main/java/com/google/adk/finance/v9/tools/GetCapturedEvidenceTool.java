package com.google.adk.finance.v9.tools;

import com.google.adk.finance.v9.evaluation.EvidenceRecord;
import com.google.adk.finance.v9.evaluation.EvidenceStoreV9;
import com.google.adk.tools.BaseTool;
import com.google.adk.tools.ToolContext;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import io.reactivex.rxjava3.core.Single;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Diagnostic tool allowing the user or agent to inspect empirical records in {@link EvidenceStoreV9}.
 * Useful for verifying captured facts directly from the ADK Web Dev UI chat.
 */
public class GetCapturedEvidenceTool extends BaseTool {
    public static final String TOOL_NAME = "get_captured_evidence";

    private final EvidenceStoreV9 evidenceStore;

    public GetCapturedEvidenceTool(EvidenceStoreV9 evidenceStore) {
        super(TOOL_NAME, "Inspects the empirical evidence records captured in EvidenceStoreV9 during this session (market quotes, P/E multiples, PnL, cost basis, holdings).");
        this.evidenceStore = evidenceStore;
    }

    @Override
    public Optional<FunctionDeclaration> declaration() {
        Schema tickerSchema = Schema.builder()
                .type(Type.Known.STRING)
                .description("Optional ticker symbol to filter captured evidence records (e.g. 'INFY.NS', 'RELIANCE'). Leave empty to inspect all records.")
                .build();

        Schema schema = Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(Map.of("ticker", tickerSchema))
                .build();

        return Optional.of(FunctionDeclaration.builder()
                .name(TOOL_NAME)
                .description(description())
                .parameters(schema)
                .build());
    }

    @Override
    public Single<Map<String, Object>> runAsync(Map<String, Object> arguments, ToolContext toolContext) {
        return Single.fromCallable(() -> {
            String ticker = arguments != null && arguments.containsKey("ticker")
                    ? String.valueOf(arguments.get("ticker")).trim()
                    : "";

            List<EvidenceRecord> records = ticker.isBlank()
                    ? evidenceStore.getAllRecords()
                    : evidenceStore.findRecordsByTicker(ticker);

            Map<String, Object> result = new HashMap<>();
            result.put("total_records", records.size());
            result.put("filter_ticker", ticker.isBlank() ? "ALL" : ticker);

            List<Map<String, Object>> formatted = records.stream().map(r -> {
                Map<String, Object> row = new HashMap<>();
                row.put("source", r.source());
                row.put("tool", r.tool());
                row.put("ticker", r.ticker());
                row.put("field", r.field());
                row.put("value", r.value());
                r.numericValue().ifPresent(nv -> row.put("numericValue", nv));
                row.put("sourceReference", r.sourceReference());
                row.put("timestamp", r.timestamp().toString());
                return row;
            }).toList();

            result.put("records", formatted);
            return result;
        });
    }
}

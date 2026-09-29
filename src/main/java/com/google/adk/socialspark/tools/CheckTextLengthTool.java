package com.google.adk.socialspark.tools;

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

public class CheckTextLengthTool extends BaseTool {
    public CheckTextLengthTool() {
        super("check_text_length", "Checks a draft's exact character count against a platform's limit.");
    }

    @Override
    public Optional<FunctionDeclaration> declaration() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("text", Schema.builder()
                .type(Type.Known.STRING)
                .description("The exact final draft text to measure.")
                .build());
        properties.put("limit", Schema.builder()
                .type(Type.Known.INTEGER)
                .description("The platform's character limit (e.g. 280 for X).")
                .build());

        Schema parameters = Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(properties)
                .required(List.of("text", "limit"))
                .build();

        return Optional.of(FunctionDeclaration.builder()
                .name("check_text_length")
                .description("Checks a draft's exact character count against a platform's limit.")
                .parameters(parameters)
                .build());
    }

    public static Map<String, Object> checkLength(String text, int limit) {
        int length = text != null ? text.length() : 0;
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("length", length);
        result.put("limit", limit);
        result.put("within_limit", length <= limit);
        result.put("over_by", Math.max(0, length - limit));
        return result;
    }

    @Override
    public Single<Map<String, Object>> runAsync(Map<String, Object> args, ToolContext toolContext) {
        String text = (String) args.getOrDefault("text", "");
        Number limitNum = (Number) args.getOrDefault("limit", 280);
        int limit = limitNum != null ? limitNum.intValue() : 280;
        return Single.just(checkLength(text, limit));
    }
}

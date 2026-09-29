package com.google.adk.socialspark.tools;

import com.google.adk.tools.BaseTool;
import com.google.adk.tools.ToolContext;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import io.reactivex.rxjava3.core.Single;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class UseProvidedImageUrlTool extends BaseTool {
    public UseProvidedImageUrlTool() {
        super("use_provided_image_url", "Registers a public image URL the user already has for the current post, instead of generating one.");
    }

    @Override
    public Optional<FunctionDeclaration> declaration() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("url", Schema.builder()
                .type(Type.Known.STRING)
                .description("A public http(s) URL to an image.")
                .build());

        Schema parameters = Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(properties)
                .required(List.of("url"))
                .build();

        return Optional.of(FunctionDeclaration.builder()
                .name("use_provided_image_url")
                .description("Registers a public image URL the user already has for the current post, instead of generating one.")
                .parameters(parameters)
                .build());
    }

    public static Map<String, Object> validateUrl(String url) {
        Map<String, Object> response = new HashMap<>();
        if (url == null || (!url.regionMatches(true, 0, "http://", 0, 7) && !url.regionMatches(true, 0, "https://", 0, 8))) {
            response.put("status", "error");
            response.put("error", "Not a valid http(s) URL: " + url);
            return response;
        }

        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod("HEAD");
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; SocialSparkBot/1.0)");
            conn.connect();

            String finalUrl = conn.getURL().toString();
            if (finalUrl.contains("accounts.google.com")) {
                response.put("status", "error");
                response.put("error", url + " redirects to a Google sign-in page - it's a console/viewer link, not a public URL.");
                return response;
            }

            int code = conn.getResponseCode();
            if (code >= 400 && code < 600) {
                response.put("status", "error");
                response.put("error", url + " returned HTTP " + code + " - it is not publicly fetchable.");
                return response;
            }

            response.put("status", "success");
            response.put("url", url);
            return response;
        } catch (Exception e) {
            response.put("status", "error");
            response.put("error", "Could not reach " + url + ": " + e.getMessage());
            return response;
        }
    }

    @Override
    public Single<Map<String, Object>> runAsync(Map<String, Object> args, ToolContext toolContext) {
        String url = (String) args.get("url");
        return Single.just(validateUrl(url));
    }
}

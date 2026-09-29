package com.google.adk.socialspark.tools;

import com.google.adk.socialspark.config.AppConfig;
import com.google.adk.tools.BaseTool;
import com.google.adk.tools.ToolContext;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import io.reactivex.rxjava3.core.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class UploadImageTool extends BaseTool {
    private static final Logger logger = LoggerFactory.getLogger(UploadImageTool.class);

    public UploadImageTool() {
        super("upload_image", "Uploads a local image to Cloud Storage and returns a fetchable URL.");
    }

    @Override
    public Optional<FunctionDeclaration> declaration() {
        Map<String, Schema> properties = new HashMap<>();
        properties.put("image_path", Schema.builder()
                .type(Type.Known.STRING)
                .description("Absolute path to a local image file.")
                .build());

        Schema parameters = Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(properties)
                .required(List.of("image_path"))
                .build();

        return Optional.of(FunctionDeclaration.builder()
                .name("upload_image")
                .description("Uploads a local image to Cloud Storage and returns a fetchable URL.")
                .parameters(parameters)
                .build());
    }

    public static Map<String, Object> upload(String imagePath) {
        Map<String, Object> result = new HashMap<>();
        if (imagePath == null || imagePath.isBlank()) {
            result.put("status", "error");
            result.put("error", "Missing required argument 'image_path'");
            return result;
        }

        if (AppConfig.DRY_RUN) {
            result.put("status", "success");
            result.put("dry_run", true);
            result.put("url", "https://storage.googleapis.com/dry-run-bucket/" + UUID.randomUUID().toString().substring(0, 12) + ".png");
            result.put("note", "DRY_RUN=true, nothing was uploaded.");
            return result;
        }

        String bucketName = AppConfig.GCS_BUCKET_NAME;
        if (bucketName.isBlank()) {
            result.put("status", "error");
            result.put("error", "GCS_BUCKET_NAME is not set.");
            return result;
        }

        try {
            File file = new File(imagePath);
            if (!file.exists()) {
                result.put("status", "error");
                result.put("error", "Image file not found: " + imagePath);
                return result;
            }

            String name = file.getName();
            String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "png";
            String blobName = "post-images/" + Instant.now().getEpochSecond() + "-" + UUID.randomUUID().toString().substring(0, 8) + "." + ext;

            Storage storage = StorageOptions.getDefaultInstance().getService();
            BlobId blobId = BlobId.of(bucketName, blobName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("image/" + ext).build();
            Blob blob = storage.create(blobInfo, Files.readAllBytes(file.toPath()));

            String url;
            if (AppConfig.GCS_PUBLIC_BUCKET) {
                url = "https://storage.googleapis.com/" + bucketName + "/" + blobName;
            } else {
                url = blob.signUrl(
                        AppConfig.SIGNED_URL_EXPIRY_HOURS,
                        TimeUnit.HOURS,
                        Storage.SignUrlOption.withV4Signature()
                ).toString();
            }

            result.put("status", "success");
            result.put("dry_run", false);
            result.put("url", url);
            return result;
        } catch (Exception e) {
            logger.error("Failed to upload image: {}", e.getMessage(), e);
            result.put("status", "error");
            result.put("error", e.getMessage() != null ? e.getMessage() : e.toString());
            return result;
        }
    }

    @Override
    public Single<Map<String, Object>> runAsync(Map<String, Object> args, ToolContext toolContext) {
        String imagePath = (String) args.get("image_path");
        return Single.just(upload(imagePath));
    }
}

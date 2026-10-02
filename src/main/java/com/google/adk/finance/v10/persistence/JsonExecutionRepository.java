package com.google.adk.finance.v10.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * File-based JSON execution repository persisting each agent run as an isolated structured JSON file.
 * <p>
 * Default storage path: {@code v10/data/executions/{executionId}.json}.
 */
public class JsonExecutionRepository implements ExecutionRepository {
    private static final Logger logger = LoggerFactory.getLogger(JsonExecutionRepository.class);
    public static final String DEFAULT_STORAGE_DIR = "v10/data/executions";

    private final Path storageDir;
    private final Gson gson;

    public JsonExecutionRepository() {
        this(Paths.get(DEFAULT_STORAGE_DIR));
    }

    public JsonExecutionRepository(Path storageDir) {
        this.storageDir = Objects.requireNonNull(storageDir, "storageDir must not be null");
        this.gson = createGson();
        ensureDirectory();
    }

    public Path getStorageDir() {
        return storageDir;
    }

    private static Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapter(Instant.class, (JsonSerializer<Instant>) (src, type, ctx) -> new JsonPrimitive(src.toString()))
                .registerTypeAdapter(Instant.class, (JsonDeserializer<Instant>) (json, type, ctx) -> Instant.parse(json.getAsString()))
                .registerTypeAdapterFactory(new OptionalTypeAdapterFactory())
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
    }

    public static class OptionalTypeAdapterFactory implements com.google.gson.TypeAdapterFactory {
        @Override
        @SuppressWarnings("unchecked")
        public <T> com.google.gson.TypeAdapter<T> create(Gson gson, com.google.gson.reflect.TypeToken<T> typeToken) {
            if (typeToken.getRawType() != Optional.class) {
                return null;
            }
            java.lang.reflect.Type type = typeToken.getType();
            java.lang.reflect.Type valueType = (type instanceof java.lang.reflect.ParameterizedType pt)
                    ? pt.getActualTypeArguments()[0]
                    : Object.class;
            com.google.gson.TypeAdapter<?> valueAdapter = gson.getAdapter(com.google.gson.reflect.TypeToken.get(valueType));
            return (com.google.gson.TypeAdapter<T>) new com.google.gson.TypeAdapter<Optional<?>>() {
                @Override
                public void write(com.google.gson.stream.JsonWriter out, Optional<?> value) throws IOException {
                    if (value == null || value.isEmpty()) {
                        out.nullValue();
                    } else {
                        ((com.google.gson.TypeAdapter<Object>) valueAdapter).write(out, value.get());
                    }
                }

                @Override
                public Optional<?> read(com.google.gson.stream.JsonReader in) throws IOException {
                    if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                        in.nextNull();
                        return Optional.empty();
                    }
                    Object val = valueAdapter.read(in);
                    return Optional.ofNullable(val);
                }
            };
        }
    }

    private synchronized void ensureDirectory() {
        try {
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
            }
        } catch (IOException e) {
            logger.error("Could not initialize execution storage directory '{}': {}", storageDir, e.getMessage());
            throw new RuntimeException("Could not initialize storage directory", e);
        }
    }

    @Override
    public synchronized void save(ExecutionRecord record) {
        Objects.requireNonNull(record, "record must not be null");
        ensureDirectory();
        Path filePath = storageDir.resolve(record.executionId() + ".json");
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            gson.toJson(record, writer);
            logger.debug("Persisted execution '{}' to '{}'", record.executionId(), filePath);
        } catch (IOException e) {
            logger.error("Failed to write execution record to '{}': {}", filePath, e.getMessage());
            throw new RuntimeException("Failed to persist execution record: " + record.executionId(), e);
        }
    }

    @Override
    public synchronized Optional<ExecutionRecord> findByExecutionId(String executionId) {
        if (executionId == null || executionId.isBlank()) return Optional.empty();
        Path filePath = storageDir.resolve(executionId + ".json");
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }

        try (FileReader reader = new FileReader(filePath.toFile())) {
            ExecutionRecord record = gson.fromJson(reader, ExecutionRecord.class);
            return Optional.ofNullable(record);
        } catch (IOException e) {
            logger.error("Error reading execution record '{}': {}", filePath, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public synchronized List<ExecutionRecord> findAll() {
        if (!Files.exists(storageDir)) {
            return List.of();
        }

        List<ExecutionRecord> list = new ArrayList<>();
        try (Stream<Path> stream = Files.list(storageDir)) {
            List<Path> files = stream
                    .filter(p -> p.toString().endsWith(".json"))
                    .toList();

            for (Path p : files) {
                try (FileReader reader = new FileReader(p.toFile())) {
                    ExecutionRecord record = gson.fromJson(reader, ExecutionRecord.class);
                    if (record != null) {
                        list.add(record);
                    }
                } catch (Exception e) {
                    logger.warn("Skipping corrupt execution record at '{}': {}", p, e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.error("Error scanning storage directory '{}': {}", storageDir, e.getMessage());
        }

        // Sort descending by startedAt
        return list.stream()
                .sorted(Comparator.comparing(ExecutionRecord::startedAt).reversed())
                .toList();
    }

    @Override
    public synchronized List<ExecutionRecord> findRecentExecutions(int limit) {
        if (limit <= 0) return List.of();
        return findAll().stream()
                .limit(limit)
                .toList();
    }

    @Override
    public synchronized List<ExecutionRecord> findByStatus(String status) {
        if (status == null || status.isBlank()) return List.of();
        return findAll().stream()
                .filter(r -> status.equalsIgnoreCase(r.status()))
                .toList();
    }

    @Override
    public synchronized List<ExecutionRecord> findExecutionsWithEvaluationFailures() {
        return findAll().stream()
                .filter(ExecutionRecord::hasEvaluationFailures)
                .toList();
    }

    @Override
    public synchronized List<ExecutionRecord> findExecutionsWithGuardrailEvents() {
        return findAll().stream()
                .filter(ExecutionRecord::hasGuardrailEvents)
                .toList();
    }

    @Override
    public synchronized int count() {
        if (!Files.exists(storageDir)) return 0;
        try (Stream<Path> stream = Files.list(storageDir)) {
            return (int) stream.filter(p -> p.toString().endsWith(".json")).count();
        } catch (IOException e) {
            return 0;
        }
    }

    @Override
    public synchronized void clear() {
        if (!Files.exists(storageDir)) return;
        try (Stream<Path> stream = Files.list(storageDir)) {
            stream.filter(p -> p.toString().endsWith(".json"))
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {}
                    });
        } catch (IOException e) {
            logger.error("Error clearing execution records at '{}': {}", storageDir, e.getMessage());
        }
    }
}

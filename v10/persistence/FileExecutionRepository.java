package com.google.adk.finance.v10.persistence;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * File-based implementation of {@link ExecutionRepository}.
 * Specializes {@link JsonExecutionRepository} for file system execution storage.
 */
public class FileExecutionRepository extends JsonExecutionRepository {

    public FileExecutionRepository() {
        super();
    }

    public FileExecutionRepository(Path storageDir) {
        super(storageDir);
    }

    public FileExecutionRepository(String dirPath) {
        super(Paths.get(dirPath));
    }
}

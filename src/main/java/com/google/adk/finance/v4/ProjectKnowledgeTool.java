package com.google.adk.finance.v4;

import java.nio.file.Path;

/**
 * Backward-compatibility subclass for ProjectKnowledgeTool.
 *
 * @deprecated Moved to {@link com.google.adk.finance.tools.ProjectKnowledgeTool}.
 */
@Deprecated
public class ProjectKnowledgeTool extends com.google.adk.finance.tools.ProjectKnowledgeTool {
    public ProjectKnowledgeTool() {
        super();
    }

    public ProjectKnowledgeTool(Path knowledgeBaseDir) {
        super(knowledgeBaseDir);
    }
}

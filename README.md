# Social Spark: Java Backend (Google ADK)

A production-grade, multi-agent AI social media post generator, researcher, image creator, human approval gate, and publisher rebuilt in **Java 21** using the official **[Google Agent Development Kit (ADK) for Java](https://adk.dev/)** (`com.google.adk:google-adk:1.4.0` and `com.google.adk:google-adk-a2a:1.4.0`).

This project provides 100% architectural and behavioral parity with the reference Python implementation (`ai-devcamp-labs`), complete with type safety, durable SQLite persistence, dynamic markdown skills, and native integration with the **AG-UI Protocol** (`@ag-ui/client` and CopilotKit Next.js frontend).

---

## Architecture Overview

```
                                  +-----------------------------+
                                  |   CopilotKit Next.js UI     |
                                  |     (http://localhost:3000) |
                                  +--------------+--------------+
                                                 |
                   AG-UI Protocol (HTTP POST / SSE Streams)
                   Static Images (/outputs/{file}), Posts (/api/posts)
                                                 |
                                                 v
                     +-------------------------------------------------------+
                     |             Javalin 6 Web Server (Port 8000)          |
                     |  - /api/adk (SSE AG-UI Protocol Bridge)               |
                     |  - /agents/state (Session State & Message Sync)       |
                     |  - /api/posts (SQLite Durable Post History)           |
                     |  - /outputs (Gallery Static Image Serving)            |
                     +---------------------------+---------------------------+
                                                 |
                     +---------------------------v---------------------------+
                     |         Root Orchestrator: SocialPosterAgent          |
                     |             (Model: gemini-flash-latest)              |
                     |  Callbacks: initStageCallback, delayBufferPostCallback|
                     |             trackStageAndSavePostCallback             |
                     +-----+---------------------+---------------------+-----+
                           |                     |                     |
              +------------+         +-----------+          +----------+---------+
              |                      |                      |                    |
              v                      v                      v                    v
     +-----------------+    +-----------------+    +-----------------+  +-----------------+
     |  research_agent |    |   draft_agent   |    |  memory_agent   |  |  MCP Toolsets   |
     |  (gemini-flash) |    |  (gemini-flash) |    |  (Remote A2A)   |  | - LinkedIn stdio|
     |                 |    |                 |    |                 |  | - Buffer HTTP   |
     | - GoogleSearch  |    | - SkillToolset  |    | - User tone &   |  |                 |
     |   Tool (isolated|    | - generate_image|    |   style history |  | Confirmation    |
     |   as per ADK)   |    | - upload_image  |    +-----------------+  | Gate on posting |
     +-----------------+    | - check_length  |                         +-----------------+
                            +-----------------+
                                     |
                          +----------v----------+
                          |   SQLite Storage    |
                          |  (social_spark.db)  |
                          +---------------------+
```

---

## Key Features

- **Google ADK Java Native Core**: Built directly against official Google ADK Java packages (`com.google.adk:google-adk:1.4.0`, `google-adk-a2a:1.4.0`).
- **Specialist Multi-Agent System**:
  - `social_poster`: Root orchestrator coordinating research, drafting, user approvals, and publishing.
  - `research_agent`: Isolated web search using `GoogleSearchTool.INSTANCE` (strictly isolated as required by Gemini Agent Platform).
  - `draft_agent`: Loads Agent Skills (`brand-voice`, `platform-style`, `post-formatter`, `poster-style`) via `SkillToolset` and `LocalSkillSource`.
  - `memory_agent`: Remote Agent-to-Agent (A2A) integration for querying user post history and brand voice.
- **Multimodal Image Generation**: Directly calls `gemini-3.1-flash-image` with automated fallbacks, image extraction, and local staging in `gallery/`.
- **Model Context Protocol (MCP)**:
  - **LinkedIn MCP**: Subprocess execution with auto-detection of `uv`/python with `fastmcp` and `httpx`.
  - **Buffer MCP**: Remote Streamable HTTP MCP connection to `https://mcp.buffer.com/mcp` with automated review delay scheduling.
  - **Human Confirmation Gate**: All publishing tools require explicit user approval.
- **Embedded Durable SQLite**: WAL-mode SQLite database `social_spark.db` preserving post history.
- **AG-UI Protocol Engine**: Full Server-Sent Events (SSE) streaming bridging ADK `Flowable<Event>` to `@ag-ui/client` and CopilotKit.

---

## Project Structure

```
ai-agents-google-adk-java/
├── build.gradle.kts           # Gradle configuration with Java 21 toolchain
├── pom.xml                    # Maven configuration for alternate builds
├── settings.gradle.kts        # Project settings
├── gradlew.bat / gradlew      # Gradle wrapper scripts
├── run-backend.bat / .sh      # Fast startup scripts
├── .env.example               # Environment variables template
├── skills/                    # Agent Skills (brand-voice, platform-style, etc.)
├── mcp/                       # Local MCP servers (linkedin_server.py)
└── src/
    ├── main/
    │   ├── java/com/google/adk/socialspark/
    │   │   ├── Application.java               # Main entrypoint & Javalin web server
    │   │   ├── config/AppConfig.java          # Environment & model configuration
    │   │   ├── db/
    │   │   │   ├── PostRecord.java            # Post DTO
    │   │   │   └── PostRepository.java        # SQLite repository (WAL mode)
    │   │   ├── tools/
    │   │   │   ├── CheckTextLengthTool.java   # Character counter tool
    │   │   │   ├── GenerateImageTool.java     # Gemini multimodal image generation
    │   │   │   ├── UploadImageTool.java       # GCS signed/public URL uploader
    │   │   │   └── UseProvidedImageUrlTool.java# Image URL validator
    │   │   ├── agents/
    │   │   │   ├── ResearchAgentFactory.java  # Isolated search specialist
    │   │   │   ├── DraftAgentFactory.java     # Skill-based drafting specialist
    │   │   │   ├── MemoryAgentFactory.java    # Remote A2A memory agent
    │   │   │   ├── PostingToolsetsFactory.java# LinkedIn & Buffer MCP toolsets
    │   │   │   └── SocialPosterAgentFactory.java# Orchestrator & callbacks
    │   │   └── agui/
    │   │       ├── AgUiModels.java            # AG-UI protocol DTOs
    │   │       ├── SessionStore.java          # Thread session storage
    │   │       └── AgUiEventTranslator.java   # Reactive ADK Event -> SSE translator
    │   └── resources/logback.xml              # Logging configuration
    └── test/
        └── java/com/google/adk/socialspark/   # Unit & Integration test suite
```

---

## Prerequisites

- **Java JDK 21+** (e.g. Eclipse Temurin or Oracle JDK 21)
- **uv** (recommended for LinkedIn MCP execution) or **Python 3.10+**
- **Google GenAI API Key** or **Vertex AI Project**:
  - `GOOGLE_API_KEY` / `GEMINI_API_KEY`, OR
  - `GOOGLE_GENAI_USE_VERTEXAI=TRUE` with `GOOGLE_CLOUD_PROJECT`

---

## Configuration (`.env`)

Copy `.env.example` to `.env` and configure your settings:

```ini
# Google GenAI / Vertex AI
GEMINI_API_KEY=your-api-key-here
# OR Vertex AI:
# GOOGLE_GENAI_USE_VERTEXAI=TRUE
# GOOGLE_CLOUD_PROJECT=your-gcp-project-id
# GOOGLE_CLOUD_LOCATION=us-central1

# Models
RESEARCH_MODEL=gemini-flash-latest
DRAFT_MODEL=gemini-flash-latest
ORCHESTRATOR_MODEL=gemini-flash-latest
IMAGE_MODEL_ID=gemini-3.1-flash-image

# Server & Execution
PORT=8000
DRY_RUN=true
BACKEND_PUBLIC_ORIGIN=http://localhost:8000
```

---

## Build & Run

### 1. Run Tests
```bash
.\gradlew.bat test       # Windows
./gradlew test           # macOS/Linux
```

### 2. Start the Server
```bash
.\run-backend.bat        # Windows
./run-backend.sh         # macOS/Linux
```
Or directly with Gradle:
```bash
.\gradlew.bat run
```

The server starts on port `8000` with the following active endpoints:
- `GET /healthz` - Health probe (`{"status": "ok"}`)
- `GET /api/posts` - Historical post records for `PostGallery.tsx`
- `POST /agents/state` - AG-UI session state rehydration
- `POST /api/adk` - AG-UI Server-Sent Events (SSE) execution stream
- `GET /outputs/{filename}` - Gallery static image preview handler

### 3. Connect Next.js Frontend
In a separate terminal, start the Next.js frontend (e.g. from `ai-devcamp-labs/frontend`):
```bash
cd C:\NCDocs\AG_Experiments\GDG_LondonProject\ai-devcamp-labs\frontend
npm run dev
```
Open `http://localhost:3000` to chat with the agent, inspect research, review image drafts, and approve posts.

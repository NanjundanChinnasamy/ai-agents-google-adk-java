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
├── run-backend.bat / .sh      # Javalin AG-UI server launcher (Port 8000)
├── run-adk-web.bat / .sh      # Official Google ADK Web Dev UI launcher (Port 8080)
├── test-agent.bat / .sh       # Direct CLI agent test console
├── .env.example               # Environment variables template
├── skills/                    # Agent Skills (brand-voice, platform-style, etc.)
├── mcp/                       # Local MCP servers (linkedin_server.py)
└── src/
    ├── main/
    │   ├── java/com/google/adk/socialspark/
    │   │   ├── Application.java               # Main entrypoint & Javalin web server (Port 8000)
    │   │   ├── AdkDevUiApplication.java       # Official Google ADK Web Dev UI (Port 8080)
    │   │   ├── AgentConsoleRunner.java        # Interactive CLI runner & one-shot agent tester
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
  - `GOOGLE_GENAI_USE_VERTEXAI=TRUE` with `GOOGLE_CLOUD_PROJECT` (e.g. in `us-central1`)

---

## Configuration (`.env`)

Copy `.env.example` to `.env` and configure your settings:

```ini
# --- Option A: Google AI Studio (API Key) ---
GEMINI_API_KEY=your-api-key-here

# --- Option B: Google Cloud Vertex AI ---
GOOGLE_GENAI_USE_VERTEXAI=TRUE
GOOGLE_CLOUD_PROJECT=your-gcp-project-id
GOOGLE_CLOUD_LOCATION=us-central1

# Models (ADK GoogleSearchTool requires gemini-2.x or gemini-3.x)
RESEARCH_MODEL=gemini-2.5-flash
DRAFT_MODEL=gemini-2.5-flash
ORCHESTRATOR_MODEL=gemini-2.5-flash
IMAGE_MODEL_ID=gemini-3.1-flash-image

# Server & Execution
PORT=8000
DRY_RUN=true
BACKEND_PUBLIC_ORIGIN=http://localhost:8000
```

---

## Testing & Execution Methods

You can test and run the agents through three different methods depending on your needs:

---

### Method 1: Official Google ADK Developer Web UI (Recommended Visual Tool)

The project includes the official **Google ADK Developer Web UI** powered by `com.google.adk:google-adk-dev`. It provides an interactive DAG visualization of the agent hierarchy, real-time token streaming, tool call inspection, and session management.

#### 1. Start the Dev UI Server
```bash
# Windows
.\run-adk-web.bat

# macOS / Linux
./run-adk-web.sh

# Or directly with Gradle:
.\gradlew.bat runDevUi --console=plain
```

#### 2. Open in Your Browser
Navigate directly to:
👉 **`http://localhost:8080/dev-ui`**

#### 3. Features & How to Use
- **Agent Switcher (Top Header)**:
  - `social_poster`: Test the root multi-agent orchestrator coordinating research, drafting, and MCP publishing.
  - `draft_agent`: Test the copywriting specialist in isolation with its Agent Skills.
  - `research_agent`: Test the Google Search grounding specialist.
- **Visual DAG Graph**: Displays the live execution tree connecting the orchestrator to its sub-agents (`research_agent`, `draft_agent`) and registered tools.
- **Tool Inspection**: Click on any executed tool chip (e.g. `google_search`, `read_skill_content`) in the chat to view the exact function call parameters, input arguments, and model output in the left inspector drawer.
- **Session Management**: Create new sessions via `+ New Session` or inspect past event steps sequentially (`Event 1 of N`, `Request`, `Response`).

---

### Method 2: Interactive Terminal Console (Fast Direct CLI Testing)

Test the agents directly from your terminal without launching any web browser or frontend.

#### 1. Start the Interactive CLI
```bash
# Windows
.\test-agent.bat

# macOS / Linux
./test-agent.sh

# Or directly with Gradle:
.\gradlew.bat runAgent --console=plain -q
```

#### 2. Interactive Console Commands
Once inside the prompt (`User > `):
- **Chat directly with the Root Agent (`social_poster`)**:
  ```text
  User > Write a post about Java 21 Virtual Threads
  ```
  Watch the orchestrator delegate to `research_agent`, invoke Google Search, transition stages (`[Stage changed -> RESEARCH]`, `[Stage changed -> DRAFT]`), and stream the finalized post.
- **Test `draft_agent` in isolation**:
  ```text
  User > draft_only Write a tweet about modern Java features
  ```
  Bypasses research and runs only the drafting specialist with local skill loading.
- **Inspect Session State**:
  ```text
  User > state
  ```
  Prints current in-memory session variables (such as `pipeline_stage`, `post_idea`, `post_draft`).
- **Exit**:
  ```text
  User > exit
  ```

#### 3. One-Shot Command Execution
You can also run one-off prompts directly without entering the interactive loop:
```bash
# Test draft agent directly
.\test-agent.bat "draft_only Write a one-liner about microservices"

# Test full orchestrator
.\test-agent.bat "Write an announcement about Java 21 release"
```

---

### Method 3: Full AG-UI Server & CopilotKit Frontend

Run the full production setup with the Javalin backend server and Next.js CopilotKit UI.

#### 1. Start the Java Backend (Port 8000)
```bash
# Windows
.\run-backend.bat

# macOS / Linux
./run-backend.sh

# Or directly with Gradle:
.\gradlew.bat run
```

Endpoints available on `http://localhost:8000`:
- `GET /healthz` - Health probe (`{"status": "ok"}`)
- `GET /api/posts` - Historical post records for `PostGallery.tsx`
- `POST /agents/state` - AG-UI session state rehydration
- `POST /api/adk` - AG-UI Server-Sent Events (SSE) execution stream
- `GET /outputs/{filename}` - Gallery static image preview handler

#### 2. Start the Next.js Frontend (Port 3000)
In a separate terminal, start the Next.js frontend (e.g. from `ai-devcamp-labs/frontend`):
```bash
cd <LOCAL-SYSTEM-PATH>\ai-devcamp-labs\frontend
npm run dev
```
Open **`http://localhost:3000`** to interact with the full web application.

---

### 4. Running Unit & Integration Tests
```bash
# Windows
.\gradlew.bat test

# macOS / Linux
./gradlew test
```


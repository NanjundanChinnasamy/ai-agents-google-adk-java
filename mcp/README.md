# Model Context Protocol (MCP) Servers

This directory contains Model Context Protocol (MCP) servers integrated with the Google ADK multi-agent architecture.

---

## 1. Yahoo Finance MCP Server (Java)

Production-grade, standalone MCP server written in **Java 21** using the official Model Context Protocol Java SDK (`io.modelcontextprotocol.sdk:mcp`) and Google ADK tooling foundations. It serves real-time stock fundamentals, historical corporate actions, financial statements, and analyst recommendations over `stdio` transport.

### Available Tools

| Tool | Parameters | Description |
|---|---|---|
| **`get_stock_info`** | `ticker` *(String, required)* | Retrieves comprehensive valuation, trading metrics, P/E, EPS, beta, enterprise value, margins, returns, balance sheet highlights, and company profile. Supports US class-share auto-normalization (e.g. `BRK.B` -> `BRK-B`). |
| **`get_stock_actions`** | `ticker` *(String, required)* | Retrieves historical dividends and stock split events with execution dates over 5 years. |
| **`get_financial_statement`** | `ticker` *(String, required)*,<br>`financial_type` *(String, required: `income_stmt`, `quarterly_income_stmt`, `balance_sheet`, `quarterly_balance_sheet`, `cashflow`, `quarterly_cashflow`)* | Retrieves annual or quarterly income statement, balance sheet, or cash flow statement line items. |
| **`get_recommendations`** | `ticker` *(String, required)*,<br>`recommendation_type` *(String, required: `recommendations` or `upgrades_downgrades`)*,<br>`months_back` *(Integer, optional, default: 12)* | Retrieves consensus analyst rating trends or deduplicated firm-level upgrades/downgrades history within the specified lookback window. |

### Building the Executable Server JAR

From the repository root:

```bash
# Build the self-contained executable JAR to mcp/yahoo-finance-mcp.jar
./gradlew buildYahooFinanceMcpJar
```

### Running & Testing the Server

#### Option A: Direct Script Launchers (stdio)
- **Windows**:
  ```cmd
  mcp\yahoo_finance_server.bat
  ```
- **Linux / macOS**:
  ```bash
  chmod +x mcp/yahoo_finance_server.sh
  mcp/yahoo_finance_server.sh
  ```

#### Option B: Direct Gradle Execution
```bash
./gradlew runYahooFinanceMcp
```

#### Option C: Interactive Inspection via MCP Inspector
```bash
npx @modelcontextprotocol/inspector mcp/yahoo_finance_server.bat
```

### Client Configuration

#### 1. Claude Desktop (`claude_desktop_config.json`)
```json
{
  "mcpServers": {
    "yahoo-finance": {
      "command": "java",
      "args": [
        "-jar",
        "C:\\NCDocs\\AG_Experiments\\GDG_LondonProject\\ai-agents-google-adk-java\\mcp\\yahoo-finance-mcp.jar"
      ]
    }
  }
}
```

#### 2. Google Agent Development Kit (ADK) Java
```java
ServerParameters params = ServerParameters.builder("java")
        .args(List.of("-jar", new File("mcp", "yahoo-finance-mcp.jar").getAbsolutePath()))
        .build();

McpToolset yahooFinanceToolset = new McpToolset(
        params,
        objectMapper,
        List.of("get_stock_info", "get_stock_actions", "get_financial_statement", "get_recommendations")
);
```

---

## 2. LinkedIn MCP Server (Python)

FastMCP stdio server exposing `get_profile()` and `create_post(text, image_path?)`.
With `DRY_RUN=true` (the default) it logs the payload and returns a fake post URL —
nothing touches LinkedIn. Set `DRY_RUN=false` only for the single happy-path
verification, then flip it back.

```bash
# standalone smoke test (from the repo root)
DRY_RUN=true uv run python mcp/linkedin_server.py   # then speak MCP over stdio, or:
npx @modelcontextprotocol/inspector uv run python mcp/linkedin_server.py
```

### LinkedIn Developer app setup

1. **Create the app**: https://www.linkedin.com/developers/apps → *Create app*.
   You need a LinkedIn *company page* to associate (create a dummy one if needed).
2. **Add products** (Products tab, both are instant self-serve approval):
   - **Share on LinkedIn** → grants `w_member_social`
   - **Sign In with LinkedIn using OpenID Connect** → grants `openid`, `profile`
3. **Auth tab**: note *Client ID* and *Client Secret*; add a redirect URL, e.g.
   `http://localhost:3000/callback` (it never needs to serve anything).

### Getting a 3-legged OAuth token

1. Open in a browser (one line, fill in CLIENT_ID):

   ```
   https://www.linkedin.com/oauth/v2/authorization?response_type=code&client_id=CLIENT_ID&redirect_uri=http%3A%2F%2Flocalhost%3A3000%2Fcallback&scope=openid%20profile%20w_member_social
   ```

2. Approve; you land on `localhost:3000/callback?code=...` (page won't load —
   fine). Copy the `code` from the URL bar. **It expires in ~30 minutes.**

3. Exchange it:

   ```bash
   curl -X POST https://www.linkedin.com/oauth/v2/accessToken \
     -d grant_type=authorization_code \
     -d code=THE_CODE \
     -d client_id=CLIENT_ID \
     -d client_secret=CLIENT_SECRET \
     -d redirect_uri=http://localhost:3000/callback
   ```

4. The response's `access_token` (valid ~60 days) goes in your `.env`:

   ```
   LINKEDIN_ACCESS_TOKEN=...
   DRY_RUN=true
   ```

### Verify the happy path ONCE

```bash
DRY_RUN=false LINKEDIN_ACCESS_TOKEN=... uv run python -c "
import importlib.util as u
s = u.spec_from_file_location('linkedin_server', 'mcp/linkedin_server.py')
m = u.module_from_spec(s); s.loader.exec_module(m)
print(m.create_post.fn('Testing my ADK DevCamp posting pipeline. If you can read this, it worked.'))
"
```

Then set `DRY_RUN=true` and leave it forever.

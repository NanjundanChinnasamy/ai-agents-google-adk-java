plugins {
    java
    application
}

group = "com.google.adk.socialspark"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    // Google ADK Java & Official Dev UI
    implementation("com.google.adk:google-adk:1.4.0")
    implementation("com.google.adk:google-adk-a2a:1.4.0")
    implementation("com.google.adk:google-adk-dev:1.4.0")

    // Google GenAI & Cloud Storage
    implementation("com.google.genai:google-genai:1.44.0")
    implementation("com.google.cloud:google-cloud-storage:2.63.0")

    // Model Context Protocol (MCP)
    implementation("io.modelcontextprotocol.sdk:mcp:1.1.2")

    // Web Server: Javalin 6.x
    implementation("io.javalin:javalin:6.3.0")

    // JSON & Serialization
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")

    // Environment & Persistence
    implementation("io.github.cdimascio:dotenv-java:3.2.0")
    implementation("org.xerial:sqlite-jdbc:3.47.2.0")

    // Reactive Streams & Concurrency
    implementation("io.reactivex.rxjava3:rxjava:3.1.12")
    implementation("io.projectreactor:reactor-core:3.7.0")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.16")
    implementation("org.slf4j:slf4j-api:2.0.16")

    // Testing
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.27.3")
}

application {
    mainClass.set("com.google.adk.socialspark.Application")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("runAgent") {
    group = "application"
    description = "Run interactive direct CLI tester for the ADK agents"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.google.adk.socialspark.AgentConsoleRunner")
    standardInput = System.`in`
}

tasks.register<JavaExec>("runDevUi") {
    group = "application"
    description = "Run the official Google ADK Web Dev UI"
    // Exclude Jetty and Javalin jars to avoid Servlet 5 vs Servlet 6 collision with Tomcat 11
    classpath = sourceSets["main"].runtimeClasspath.filter { file ->
        !file.name.contains("jetty") && !file.name.contains("javalin")
    }
    mainClass.set("com.google.adk.socialspark.AdkDevUiApplication")
}

tasks.register<JavaExec>("runFinanceV1") {
    group = "application"
    description = "Run interactive direct CLI tester for Finance Agent v1"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.google.adk.finance.v1.FinanceConsoleV1")
    standardInput = System.`in`
}

tasks.register<JavaExec>("runFinanceV2") {
    group = "application"
    description = "Run interactive direct CLI tester for Finance Agent v2"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.google.adk.finance.v2.FinanceConsoleV2")
    standardInput = System.`in`
}

tasks.register<JavaExec>("runFinanceV3") {
    group = "application"
    description = "Run interactive direct CLI tester for Finance Agent v3"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.google.adk.finance.v3.FinanceConsoleV3")
    standardInput = System.`in`
}

tasks.register<JavaExec>("runFinanceV4") {
    group = "application"
    description = "Run interactive direct CLI tester for Finance Advisor v4 (Skills & Grounding)"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.google.adk.finance.v4.FinanceConsoleV4")
    standardInput = System.`in`
}

tasks.register<JavaExec>("runFinanceV5") {
    group = "application"
    description = "Run interactive direct CLI tester for Finance Advisor v5 (Yahoo Finance MCP + Search + Skills)"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.google.adk.finance.v5.FinanceConsoleV5")
    standardInput = System.`in`
}

tasks.register<JavaExec>("runYahooFinanceMcp") {
    group = "application"
    description = "Run Yahoo Finance MCP Server over stdio transport"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.google.adk.mcp.yahoofinance.YahooFinanceMcpServer")
    standardInput = System.`in`
}

tasks.register<Jar>("buildYahooFinanceMcpJar") {
    group = "build"
    description = "Packages the Yahoo Finance MCP Server into a self-contained executable JAR in mcp/"
    archiveFileName.set("yahoo-finance-mcp.jar")
    destinationDirectory.set(file("mcp"))
    isZip64 = true
    manifest {
        attributes["Main-Class"] = "com.google.adk.mcp.yahoofinance.YahooFinanceMcpServer"
    }
    from(sourceSets["main"].output)
    dependsOn(configurations.runtimeClasspath)
    val mcpDependencies = listOf("mcp", "jackson", "reactor", "reactive", "slf4j", "logback", "json-schema", "itu", "snakeyaml")
    from({
        configurations.runtimeClasspath.get()
            .filter { jar -> jar.name.endsWith(".jar") && mcpDependencies.any { jar.name.contains(it, ignoreCase = true) } }
            .map { zipTree(it) }
    }) {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }
}

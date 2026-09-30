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
    classpath = sourceSets["main"].runtimeClasspath
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

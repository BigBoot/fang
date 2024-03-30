
import io.gitlab.arturbosch.detekt.Detekt

plugins {
    application

    kotlin("jvm") version "1.7.20"
    kotlin("kapt") version "1.7.20"

    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("io.gitlab.arturbosch.detekt").version("1.21.0")
    id("com.github.ben-manes.versions") version "0.42.0"
    id("se.patrikerdes.use-latest-versions") version "0.2.18"
}

repositories {
    mavenCentral()
}

group = "de.bigboot.ggtools"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("de.bigboot.ggtools.fang.MainKt")
}

dependencies {

    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    // kotlinx-coroutines & reactor extension
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4-native-mt")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.6.4-native-mt")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4-native-mt")

    // Exposed
    implementation("org.jetbrains.exposed:exposed-core:0.39.2")
    implementation("org.jetbrains.exposed:exposed-dao:0.39.2")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.39.2")
    implementation("org.jetbrains.exposed:exposed-java-time:0.39.2")

    // Databases
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("mysql:mysql-connector-java:8.0.30")
    // implementation("org.postgresql:postgresql:42.5.0")
    // implementation("com.impossibl.pgjdbc-ng:pgjdbc-ng:0.8.3")
    // implementation("org.xerial:sqlite-jdbc:3.30.1")
    implementation("com.h2database:h2:2.1.214")
    // implementation("com.microsoft.sqlserver:mssql-jdbc:6.4.0.jre7")

    // Koin
    implementation("io.insert-koin:koin-core:3.2.2")

    // Discord4J
    implementation("com.discord4j:discord4j-core:3.2.6")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")

    // Moshi
    implementation("com.squareup.moshi:moshi:1.14.0")
    implementation("com.squareup.moshi:moshi-kotlin-codegen:1.14.0")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.14.0")

    // Toml
    implementation("org.tomlj:tomlj:1.0.0")

    // Logging
    implementation("org.tinylog:tinylog-api-kotlin:2.5.0")
    implementation("org.tinylog:tinylog-impl:2.5.0")
    implementation("org.tinylog:slf4j-tinylog:2.5.0")

    // Flyway
    implementation("org.flywaydb:flyway-core:9.4.0")
    implementation("org.flywaydb:flyway-mysql:9.4.0")

    // Scrimage
    implementation("com.sksamuel.scrimage:scrimage-core:4.0.32")

    // CopyDown
    implementation("io.github.furstenheim:copy_down:1.1")

    // Zip4J
    implementation("net.lingala.zip4j:zip4j:2.11.5")

    // JEmoji
    implementation("net.fellbaum:jemoji:1.4.0")

    // JUnit
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.1")

    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.22.0-RC1")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks {
    compileKotlin {
        kotlinOptions.freeCompilerArgs = listOf(
            "-Xopt-in=kotlin.time.ExperimentalTime",
            "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }
    compileTestKotlin {
        kotlinOptions.freeCompilerArgs = listOf(
            "-Xopt-in=kotlin.time.ExperimentalTime",
            "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }
}

tasks.withType<Detekt> {
    setSource(files(projectDir))
    include("**/*.kt")
    include("**/*.kts")
    exclude("**/resources/**")
    exclude("**/build/**")

    config.setFrom(files("detekt.yml"))

    reports {
        html {
            required.set(true)
            outputLocation.set(file("build/reports/detekt.html"))
        }
        txt {
            required.set(true)
        }
        xml {
            required.set(true)
        }
    }
}

tasks.register<Detekt>("detektApply") {
    autoCorrect = true
}

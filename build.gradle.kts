
import io.gitlab.arturbosch.detekt.Detekt

plugins {
    application

    kotlin("jvm") version "1.4.0"
    kotlin("kapt") version "1.4.0"

    id("com.github.johnrengelman.shadow") version "6.0.0"
    id("io.gitlab.arturbosch.detekt").version("1.9.0")
    id("com.github.ben-manes.versions") version "0.29.0"
    id("se.patrikerdes.use-latest-versions") version "0.2.14"
}

repositories {
    mavenCentral()
    jcenter()
    maven("https://dl.bintray.com/arrow-kt/arrow-kt/")
}

group = "de.bigboot.ggtools"
version = "1.0-SNAPSHOT"

application {
    mainClassName = "de.bigboot.ggtools.fang.MainKt"
}

dependencies {

    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    // kotlinx-coroutines & reactor extension
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.3.9")

    // Exposed
    implementation("org.jetbrains.exposed:exposed-core:0.27.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.27.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.27.1")

    // Databases
    implementation("mysql:mysql-connector-java:8.0.21")
    implementation("org.postgresql:postgresql:42.2.16")
    // implementation("com.impossibl.pgjdbc-ng:pgjdbc-ng:0.8.3")
    // implementation("org.xerial:sqlite-jdbc:3.30.1")
    implementation("com.h2database:h2:1.4.200")
    // implementation("com.microsoft.sqlserver:mssql-jdbc:6.4.0.jre7")

    // Koin
    implementation("org.koin:koin-core:2.1.5")

    // Discord4J
    implementation("com.discord4j:discord4j-core:3.1.0")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")

    // Moshi
    implementation("com.squareup.moshi:moshi-kotlin-codegen:1.10.0")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.10.0")

    // Toml
    implementation("org.tomlj:tomlj:1.0.0")

    // Logging
    implementation("org.tinylog:tinylog-api-kotlin:2.2.0-M3")
    implementation("org.tinylog:tinylog-impl:2.2.0-M3")
    implementation("org.tinylog:slf4j-tinylog:2.2.0-M3")

    // Flyway
    implementation("org.flywaydb:flyway-core:6.5.5")

    // JUnit
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0-RC1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0-RC1")

    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.12.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
        kotlinOptions.freeCompilerArgs = listOf(
            "-Xopt-in=kotlin.time.ExperimentalTime",
            "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
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
            enabled = true
            destination = file("build/reports/detekt.html")
        }
        txt {
            enabled = false
        }
        xml {
            enabled = false
        }
    }
}

tasks.register<Detekt>("detektApply") {
    autoCorrect = true
}

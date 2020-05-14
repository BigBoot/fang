
import io.gitlab.arturbosch.detekt.Detekt

plugins {
    application

    kotlin("jvm") version "1.3.72"
    kotlin("kapt") version "1.3.72"

    id("com.github.johnrengelman.shadow") version "5.2.0"
    id("io.gitlab.arturbosch.detekt").version("1.9.0")
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

    // kotlinx-coroutines & reactor extension
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.3.5")

    // Exposed
    implementation("org.jetbrains.exposed:exposed-core:0.21.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.21.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.21.1")

    // Databases
    implementation("mysql:mysql-connector-java:8.0.20")
    implementation("org.postgresql:postgresql:42.2.2")
    // implementation("com.impossibl.pgjdbc-ng:pgjdbc-ng:0.8.3")
    // implementation("org.xerial:sqlite-jdbc:3.30.1")
    implementation("com.h2database:h2:1.4.199")
    // implementation("com.microsoft.sqlserver:mssql-jdbc:6.4.0.jre7")

    // Koin
    implementation("org.koin:koin-core:2.1.5")

    // Discord4J
    implementation("com.discord4j:discord4j-core:3.1.0.M2")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.8.1")
    implementation("com.squareup.retrofit2:converter-moshi:2.8.1")

    // Moshi
    implementation("com.squareup.moshi:moshi-kotlin-codegen:1.9.2")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.9.2")

    // Toml
    implementation("org.tomlj:tomlj:1.0.0")

    // Logging
    implementation("org.tinylog:tinylog-api-kotlin:2.1.2")
    implementation("org.tinylog:tinylog-impl:2.1.2")
    implementation("org.tinylog:slf4j-tinylog:2.1.2")

    // JUnit
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.3.1")

    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.9.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
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

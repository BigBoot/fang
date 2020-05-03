plugins {
    application

    kotlin("jvm") version "1.3.72"
    kotlin("kapt") version "1.3.72"

    id("com.github.johnrengelman.shadow") version "5.2.0"
}

group = "de.bigboot.ggtools"
version = "1.0-SNAPSHOT"

application {
    mainClassName = "de.bigboot.ggtools.fang.MainKt"
}

repositories {
    mavenCentral()
    jcenter()
    maven("https://dl.bintray.com/arrow-kt/arrow-kt/")
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
    implementation("mysql:mysql-connector-java:8.0.20")

    // Discord4J
    implementation ("com.discord4j:discord4j-core:3.1.0.M2")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.8.1")
    implementation("com.squareup.retrofit2:converter-moshi:2.8.1")

    // Moshi
    implementation("com.squareup.moshi:moshi-kotlin-codegen:1.9.2")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.9.2")

    // Toml
    implementation("org.tomlj:tomlj:1.0.0")

    // JUnit
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.1" )
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.3.1" )
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
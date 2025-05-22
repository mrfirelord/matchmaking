plugins {
    kotlin("jvm") version "2.1.20"
    id("com.google.protobuf") version "0.9.5"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.20"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.netty:netty-all:4.1.108.Final")
    implementation("com.google.protobuf:protobuf-java:4.31.0")

    testImplementation(kotlin("test"))
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.31.0"
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
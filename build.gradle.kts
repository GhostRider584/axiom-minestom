plugins {
    id("java")
    id("com.gradleup.shadow") version "9.1.0"
    id("com.gradle.plugin-publish") version "2.0.0"
}

group = "fr.ghostrider584"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    api("net.minestom:minestom:2025.09.13-1.21.8")
    api("ch.qos.logback:logback-classic:1.5.16")
    api("org.slf4j:slf4j-api:2.0.16")
    api("org.joml:joml:1.10.8")
}

tasks.shadowJar {
    mergeServiceFiles()
    archiveClassifier.set("")
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}
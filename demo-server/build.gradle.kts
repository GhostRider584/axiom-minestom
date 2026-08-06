plugins {
    id("java")
}

group = "fr.ghostrider584"
version = "0.0.4"

dependencies {
    implementation(project(":"))
    implementation("net.minestom:minestom:2026.05.17c-26.1.1")
    implementation("ch.qos.logback:logback-classic:1.5.16")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25)) // Minestom has a minimum Java version of 25
    }
}

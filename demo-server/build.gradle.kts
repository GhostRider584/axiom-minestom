plugins {
    id("java")
}

group = "fr.ghostrider584"
version = "0.0.3"

dependencies {
    implementation(project(":"))
    implementation(libs.minestom)
    implementation(libs.logging.logback)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25)) // Minestom has a minimum Java version of 25
    }
}
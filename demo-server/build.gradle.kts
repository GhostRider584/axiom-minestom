plugins {
    id("java")
}

group = "fr.ghostrider584"
version = rootProject.version

dependencies {
    implementation(project(":"))
    implementation(libs.minestom)
    implementation("ch.qos.logback:logback-classic:1.5.16")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25)) // Minestom has a minimum Java version of 25
    }
}

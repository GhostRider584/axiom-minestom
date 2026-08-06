plugins {
    id("smolder.publishing-conventions")
}

dependencies {
    compileOnly(libs.minestom)
    api(libs.logging.slf4j)
    api(libs.zstd)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25)) // Minestom has a minimum Java version of 25
    }
}
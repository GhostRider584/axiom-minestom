plugins {
    id("smolder.publishing-conventions")
}

dependencies {
    compileOnly("net.minestom:minestom:2025.09.13-1.21.8")
    api("org.slf4j:slf4j-api:2.0.16")
    api("org.joml:joml:1.10.8")
    api("com.github.luben:zstd-jni:1.5.7-4")
}

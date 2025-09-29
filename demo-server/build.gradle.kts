plugins {
    id("java")
}

group = "fr.ghostrider584"
version = "0.0.2"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("net.minestom:minestom:2025.09.13-1.21.8")
    implementation("fr.ghostrider584:axiom-minestom:0.0.2")
    implementation("ch.qos.logback:logback-classic:1.5.16")
}

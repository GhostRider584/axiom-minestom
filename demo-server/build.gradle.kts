plugins {
    id("java")
}

group = "fr.ghostrider584"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":"))
}

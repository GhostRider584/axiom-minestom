plugins {
    id("java")
    application
}

group = "fr.ghostrider584"
version = rootProject.version

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

application {
    mainClass.set("fr.ghostrider584.axiom.demo.TestServer")
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED") // zstd-jni loads a native library
}

tasks.named<JavaExec>("run") {
    workingDir = layout.projectDirectory.dir("run").asFile
    doFirst { workingDir.mkdirs() }
}

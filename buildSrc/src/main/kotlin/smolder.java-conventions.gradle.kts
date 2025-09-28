plugins {
    id("java")
    id("java-library")
}

var smolderUsername = project.property("smolderUsername") as String?
var smolderPassword = project.property("smolderPassword") as String?
val publicRepository: String by project

repositories {
    mavenLocal()
    mavenCentral()

    maven(publicRepository) {
        name = "smolderPublic"
        credentials {
            username = smolderUsername
            password = smolderPassword
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
plugins {
    id("java")
    id("java-library")
}

var smolderUsername: String? = findProperty("smolderUsername") as String?
var smolderPassword: String? = findProperty("smolderPassword") as String?
val publicRepository: String by project

repositories {
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

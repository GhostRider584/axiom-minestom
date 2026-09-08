plugins {
    id("smolder.java-conventions")
    `maven-publish`
    signing
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks {
    javadoc {
        isFailOnError = false
    }
}

val smolderUsername = providers.environmentVariable("SMOLDER_MAVEN_USERNAME")
    .orElse(providers.gradleProperty("smolderUsername")).orNull
val smolderPassword = providers.environmentVariable("SMOLDER_MAVEN_TOKEN")
    .orElse(providers.gradleProperty("smolderPassword")).orNull
val publicRepository: String by project

publishing {
    repositories {
        maven {
            url = uri(publicRepository)
            credentials {
                username = smolderUsername
                password = smolderPassword
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name.set("${project.group}:${project.name}")
                description.set(project.description)
                packaging = "jar"
            }
        }
    }
}

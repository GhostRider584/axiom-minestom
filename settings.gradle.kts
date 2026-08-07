rootProject.name = "axiom-minestom"

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
//        maven(url = "https://central.sonatype.com/repository/maven-snapshots/") {
//            content {
//                includeModule("net.minestom", "minestom")
//            }
//        }
        mavenCentral()
    }
}

include("demo-server")
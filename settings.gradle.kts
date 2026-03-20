plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "LootPool"

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io/")
        }
        maven {
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }
        maven {
            url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        }
        maven {
            url = uri("https://git.bigteam.pw/api/v4/groups/35/-/packages/maven")
            name = "GitLab"
            credentials(HttpHeaderCredentials::class) {
                name = "Deploy-Token"
                value = providers.gradleProperty("gitLabPrivateToken").orNull
            }
            authentication {
                create("header", HttpHeaderAuthentication::class)
            }
        }
        maven {
            url = uri("https://mymavenrepo.com/repo/SjKIru68icwwmC0qOtV7/")
        }
    }
}
include("api")
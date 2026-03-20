plugins {
    id("java-library")
    id("maven-publish")
}

java {
    withSourcesJar()
    toolchain.languageVersion.set(JavaLanguageVersion.of(16))
}

dependencies {
    compileOnly(libs.paperApi)
}

publishing {
    repositories {
        val repoUrl = findProperty("myMavenRepoWriteUrl").toString()
        if (repoUrl.isNotEmpty()) {
            maven {
                url = uri(repoUrl)

                credentials {
                    username = findProperty("myMavenRepoWriteUsername").toString()
                    password = findProperty("myMavenRepoWritePassword").toString()
                }
            }
        }
        maven {
            url = uri("https://git.bigteam.pw/api/v4/projects/43/packages/maven")
            credentials(HttpHeaderCredentials::class) {
                name = "Deploy-Token"
                value = findProperty("gitLabPrivateToken") as String?
            }
            authentication {
                create("header", HttpHeaderAuthentication::class)
            }
        }
    }

    publications {
        create<MavenPublication>("maven") {
            groupId = findProperty("group").toString()
            artifactId = "LootPool-api"
            version = findProperty("version").toString()

            from(components["java"])
        }
    }
}
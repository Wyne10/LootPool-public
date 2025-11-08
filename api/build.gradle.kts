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
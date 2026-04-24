import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory

plugins {
    `kotlin-dsl`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

fun ProviderFactory.requiredEnv(name: String): Provider<String> {
    return environmentVariable(name).orElse(provider { error("$name must be set") })
}

subprojects {
    group = providers.gradleProperty("pluginGroup").get()
    version = providers.gradleProperty("pluginVersion")
        .orElse("1.0.0-SNAPSHOT")
        .get()

    plugins.withId("maven-publish") {
        val repoUser = providers.requiredEnv("ARTIFACTS_REPO_USER")
        val repoToken = providers.requiredEnv("ARTIFACTS_REPO_TOKEN")

        configure<PublishingExtension> {
            repositories {
                maven {
                    name = "BuildArtifacts"
                    url = uri("https://pkgs.dev.azure.com/jamarston/762ffd9e-ca64-466d-84e9-7a0e42e5d89a/_packaging/BuildArtifacts/maven/v1")
                    credentials {
                        username = repoUser.orNull
                        password = repoToken.orNull
                    }
                    authentication {
                        create<BasicAuthentication>("basic")
                    }
                }
            }
        }
    }
}

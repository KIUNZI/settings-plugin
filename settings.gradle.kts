pluginManagement {
    repositories {
        maven {
            fun ProviderFactory.requiredEnv(name: String): Provider<String> {
                return environmentVariable(name).orElse(provider { error("$name must be set") })
            }

            val repoUser = providers.requiredEnv("ARTIFACTS_REPO_USER")
            val repoToken = providers.requiredEnv("ARTIFACTS_REPO_TOKEN")

            name = "BuildArtifacts"
            url = uri("https://pkgs.dev.azure.com/jamarston/762ffd9e-ca64-466d-84e9-7a0e42e5d89a/_packaging/BuildArtifacts/maven/v1")
            credentials {
                username = repoUser.get()
                password = repoToken.get()
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "settings-plugin"

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        maven {
            fun ProviderFactory.requiredEnv(name: String): Provider<String> {
                return environmentVariable(name).orElse(provider { error("$name must be set") })
            }

            val repoUser = providers.requiredEnv("ARTIFACTS_REPO_USER")
            val repoToken = providers.requiredEnv("ARTIFACTS_REPO_TOKEN")

            name = "ExternalDependencies"
            url = uri("https://pkgs.dev.azure.com/jamarston/762ffd9e-ca64-466d-84e9-7a0e42e5d89a/_packaging/ExternalDependencies/maven/v1")
            credentials {
                username = repoUser.get()
                password = repoToken.get()
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
        maven {
            fun ProviderFactory.requiredEnv(name: String): Provider<String> {
                return environmentVariable(name).orElse(provider { error("$name must be set") })
            }

            val repoUser = providers.requiredEnv("ARTIFACTS_REPO_USER")
            val repoToken = providers.requiredEnv("ARTIFACTS_REPO_TOKEN")

            name = "BuildArtifacts"
            url = uri("https://pkgs.dev.azure.com/jamarston/762ffd9e-ca64-466d-84e9-7a0e42e5d89a/_packaging/BuildArtifacts/maven/v1")
            credentials {
                username = repoUser.get()
                password = repoToken.get()
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}

include(
    "settings-plugin",
    "conventions-support"
)
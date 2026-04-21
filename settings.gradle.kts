pluginManagement {
    repositories {
        maven {
            fun ProviderFactory.configString(
                prop: String,
                allowNull: Boolean = false): Provider<String> {
                val envVarUnderscore = prop.replace(".", "_")
                val envVarUppercase = envVarUnderscore.uppercase()
                val property = gradleProperty(prop)
                    .orElse(systemProperty(prop))
                    .orElse(environmentVariable(prop))
                    .orElse(environmentVariable(envVarUnderscore))
                    .orElse(environmentVariable(envVarUppercase))
                if(allowNull) {
                    return property
                }
                return property.orElse(provider { error("$prop must be set") })
            }
            val repoUrl = providers.configString("artifacts.repo.url")
            val repoUser = providers.configString("artifacts.repo.user", true)
            val repoToken = providers.configString("artifacts.repo.token", true)
            name = "Artifacts"
            url = uri(repoUrl.get())
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

rootProject.name = "kiunzi-settings-plugin"

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        maven {
            fun ProviderFactory.configString(
                prop: String,
                allowNull: Boolean = false): Provider<String> {
                val envVarUnderscore = prop.replace(".", "_")
                val envVarUppercase = envVarUnderscore.uppercase()
                val property = gradleProperty(prop)
                    .orElse(systemProperty(prop))
                    .orElse(environmentVariable(prop))
                    .orElse(environmentVariable(envVarUnderscore))
                    .orElse(environmentVariable(envVarUppercase))
                if(allowNull) {
                    return property
                }
                return property.orElse(provider { error("$prop must be set") })
            }
            val repoUrl = providers.configString("artifacts.repo.url")
            val repoUser = providers.configString("artifacts.repo.user", true)
            val repoToken = providers.configString("artifacts.repo.token", true)
            name = "Artifacts"
            url = uri(repoUrl.get())
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
package uk.co.jasonmarston.build.settings

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class ArtifactsSettingsPluginFunctionalTest {
    @field:TempDir
    lateinit var testProjectDir: File

    @Test
    fun `plugin applies without error`() {
        val settingsFile = File(testProjectDir, "settings.gradle.kts")
        settingsFile.writeText("""
            pluginManagement {
                repositories {
                    maven {
                        name = "BuildArtifacts"
                        url = uri("https://pkgs.dev.azure.com/jamarston/762ffd9e-ca64-466d-84e9-7a0e42e5d89a/_packaging/BuildArtifacts/maven/v1")
                        credentials {
                            username = "dummy"
                            password = "dummy"
                        }
                        authentication {
                            create<org.gradle.authentication.http.BasicAuthentication>("basic")
                        }
                    }
                }
            }
            plugins {
                id("uk.co.jasonmarston.standards.settings")
            }
        """.trimIndent())

        val result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withPluginClasspath()
            .withArguments("tasks")
            .withEnvironment(mapOf(
                "ARTIFACTS_REPO_USER" to "dummy",
                "ARTIFACTS_REPO_TOKEN" to "dummy"
            ))
            .build()

        assertTrue(result.output.contains("BUILD SUCCESSFUL"))
    }
}

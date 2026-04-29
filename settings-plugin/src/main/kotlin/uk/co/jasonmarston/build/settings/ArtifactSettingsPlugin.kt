package uk.co.jasonmarston.build.settings

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.artifacts.repositories.PasswordCredentials
import org.gradle.api.initialization.Settings
import org.gradle.api.initialization.resolve.RepositoriesMode
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.authentication.http.BasicAuthentication
import uk.co.jasonmarston.build.utility.requiredEnv

import java.net.URI

/**
 * Plugin for configuring artifact and dependency repositories in Gradle settings.
 *
 * <p>This plugin enforces repository policies, injects publishing repositories, and ensures
 * consistent configuration for all projects in a build. It is designed to work with Azure Artifacts
 * feeds for both build artifacts and external dependencies.</p>
 *
 * @author Jason Marston
 */
@Suppress("unused")
class ArtifactsSettingsPlugin : Plugin<Settings> {
    /**
     * Applies the plugin to the given [Settings] object.
     *
     * @param settings the Gradle [Settings] to configure
     */
    override fun apply(settings: Settings) {
        val repositoriesConfig = RepositoriesConfig.from(settings.providers)

        settings.pluginManagement(Action { pluginManagement ->
            pluginManagement.repositories(Action { repositories ->
                validateBootstrapRepositories(
                    repositories = repositories,
                    repositoriesConfig = repositoriesConfig
                )
            })
        })

        settings.gradle.settingsEvaluated(Action { evaluatedSettings ->
            evaluatedSettings.dependencyResolutionManagement(Action { dependencyResolution ->
                @Suppress("UnstableApiUsage")
                dependencyResolution.repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                @Suppress("UnstableApiUsage")
                dependencyResolution.repositories(Action { repositories ->
                    validateDependencyRepositories(repositories, repositoriesConfig)
                    forceDependencyRepositories(repositories, repositoriesConfig)
                })
            })
        })

        settings.gradle.beforeProject(Action { project ->
            enforceSnapshotChangingModulesPolicy(project)
            enforcePublishRepositories(project, repositoriesConfig)
        })

        settings.gradle.projectsEvaluated(Action { gradle ->
            gradle.rootProject.allprojects { project ->
                if (project.plugins.hasPlugin("maven-publish")) {
                    validatePublishingRepositories(project, repositoriesConfig)
                }
            }
        })
    }

    /**
     * Enforces that changing modules are not cached for snapshot versions.
     *
     * @param project the [Project] to configure
     */
    private fun enforceSnapshotChangingModulesPolicy(project: Project) {
        project.configurations.configureEach { configuration ->
            if (!configuration.isCanBeResolved) return@configureEach

            // Evaluate at resolution time so late-applied version plugins are respected.
            configuration.incoming.beforeResolve {
                if (isSnapshotVersion(project.version.toString())) {
                    configuration.resolutionStrategy.cacheChangingModulesFor(0, "seconds")
                }
            }
        }
    }

    /**
     * Determines if the given version string is a snapshot version.
     *
     * @param version the version string
     * @return true if the version ends with "-SNAPSHOT", false otherwise
     */
    private fun isSnapshotVersion(version: String): Boolean {
        return version.endsWith("-SNAPSHOT", ignoreCase = true)
    }

    /**
     * Enforces publishing repository configuration for projects with the maven-publish plugin.
     *
     * @param project the [Project] to configure
     * @param repositoriesConfig the [RepositoriesConfig] to use
     */
    private fun enforcePublishRepositories(
        project: Project,
        repositoriesConfig: RepositoriesConfig
    ) {
        project.plugins.withId("maven-publish") {
            project.extensions.configure(PublishingExtension::class.java, Action { publishing ->
                val existingRepositories = publishing.repositories.toList()
                existingRepositories.forEach { publishing.repositories.remove(it) }
                configureRepository(
                    repositories = publishing.repositories,
                    repoName = "BuildArtifacts",
                    repoUrl = repositoriesConfig.buildArtifactsUrl,
                    repositoryCredentials = repositoriesConfig
                )
            })
        }
    }

    /**
     * Validates that only the expected publishing repository is declared.
     *
     * @param project the [Project] to check
     * @param repositoriesConfig the [RepositoriesConfig] to use
     * @throws GradleException if extra repositories are found
     */
    private fun validatePublishingRepositories(
        project: Project,
        repositoriesConfig: RepositoriesConfig
    ) {
        val publishing = project.extensions.findByType(PublishingExtension::class.java) ?: return
        val declaredRepositories = publishing.repositories.toList()
        val expectedUrl = repositoriesConfig.buildArtifactsUrl

        // Check for any repositories OTHER than the automatically injected BuildArtifacts
        val invalidRepositories = declaredRepositories.mapNotNull { repo ->
            when (repo) {
                is MavenArtifactRepository -> {
                    val repoUrl = repo.url.toString()
                    if (repo.name == "BuildArtifacts" && sameUrl(repoUrl, expectedUrl)) {
                        null  // This is our auto-injected repo, it's OK
                    } else {
                        "'${repo.name}' (${repo.url})"
                    }
                }
                else -> "'${repo.name}' (${repo.javaClass.simpleName})"
            }
        }

        if (invalidRepositories.isNotEmpty()) {
            throw GradleException(
                "Projects using the ArtifactsSettingsPlugin must not declare additional publishing repositories. " +
                    "The plugin automatically configures publishing to BuildArtifacts. " +
                    "Remove the 'publishing { repositories { ... } }' block from project '${project.path}'. " +
                    "Found extra repositories: ${invalidRepositories.joinToString()}"
            )
        }
    }

    /**
     * Validates that no repositories are declared in dependencyResolutionManagement.
     *
     * @param repositories the [RepositoryHandler] to check
     * @param repositoriesConfig the [RepositoriesConfig] to use
     * @throws GradleException if repositories are found
     */
    private fun validateDependencyRepositories(
        repositories: RepositoryHandler,
        repositoriesConfig: RepositoriesConfig
    ) {
        val declaredRepositories = repositories.toList()
        if (declaredRepositories.isNotEmpty()) {
            val repositoryList = declaredRepositories.joinToString { 
                when (it) {
                    is MavenArtifactRepository -> "'${it.name}' (${it.url})"
                    else -> "'${it.name}' (${it.javaClass.simpleName})"
                }
            }
            throw GradleException(
                "Projects using the ArtifactsSettingsPlugin must not declare repositories in dependencyResolutionManagement. " +
                    "The plugin automatically configures both BuildArtifacts and ExternalDependencies repositories. " +
                    "Remove the 'dependencyResolutionManagement { repositories { ... } }' block from your settings.gradle.kts. " +
                    "Found: $repositoryList"
            )
        }
    }

    /**
     * Validates that exactly one bootstrap repository is declared in pluginManagement.
     *
     * @param repositories the [RepositoryHandler] to check
     * @param repositoriesConfig the [RepositoriesConfig] to use
     * @throws GradleException if the repository is missing or incorrect
     */
    private fun validateBootstrapRepositories(
        repositories: RepositoryHandler,
        repositoriesConfig: RepositoriesConfig
    ) {
        val expectedUrl = repositoriesConfig.buildArtifactsUrl
        val declaredRepositories = repositories.toList()

        if (declaredRepositories.size != 1) {
            val declared = declaredRepositories.joinToString { repository ->
                when (repository) {
                    is MavenArtifactRepository -> repository.url.toString()
                    else -> repository.name.ifBlank { repository.javaClass.simpleName }
                }
            }
            throw GradleException(
                "Exactly one repository must be declared in pluginManagement and it must be '$expectedUrl'. " +
                    "Found ${declaredRepositories.size}: $declared"
            )
        }

        val bootstrapRepository = declaredRepositories.single()
        if (bootstrapRepository !is MavenArtifactRepository) {
            throw GradleException(
                "pluginManagement repository must be Maven and point to '$expectedUrl'. " +
                    "Found: ${bootstrapRepository.name.ifBlank { bootstrapRepository.javaClass.simpleName }}"
            )
        }

        val actualUrl = bootstrapRepository.url.toString()
        if (!sameUrl(actualUrl, expectedUrl)) {
            throw GradleException(
                "pluginManagement repository must be '$expectedUrl' but was '$actualUrl'"
            )
        }
    }

    /**
     * Forces the dependency repositories to the expected configuration.
     *
     * @param repositories the [RepositoryHandler] to configure
     * @param repositoriesConfig the [RepositoriesConfig] to use
     */
    private fun forceDependencyRepositories(
        repositories: RepositoryHandler,
        repositoriesConfig: RepositoriesConfig
    ) {
        val existingRepositories = repositories.toList()
        existingRepositories.forEach { repositories.remove(it) }
        configureRepository(repositories, "ExternalDependencies", repositoriesConfig.externalDependenciesUrl, repositoriesConfig)
        configureRepository(repositories, "BuildArtifacts", repositoriesConfig.buildArtifactsUrl, repositoriesConfig)
    }

    /**
     * Configures a Maven repository with the given parameters.
     *
     * @param repositories the [RepositoryHandler] to add to
     * @param repoName the name of the repository
     * @param repoUrl the URL of the repository
     * @param repositoryCredentials the credentials to use
     * @param configureContent optional content filter
     */
    private fun configureRepository(
        repositories: RepositoryHandler,
        repoName: String,
        repoUrl: String,
        repositoryCredentials: RepositoriesConfig,
        configureContent: Action<org.gradle.api.artifacts.repositories.RepositoryContentDescriptor>? = null
    ) {
        repositories.maven(Action { repo ->
            repo.name = repoName
            repo.url = URI.create(repoUrl)
            repo.credentials(PasswordCredentials::class.java, Action { credentials ->
                credentials.username = repositoryCredentials.user.get()
                credentials.password = repositoryCredentials.token.get()
            })
            repo.authentication(Action { authentications ->
                authentications.create("basic", BasicAuthentication::class.java)
            })
            if (configureContent != null) {
                repo.content(configureContent)
            }
        })
    }

    /**
     * Configuration for artifact and dependency repositories.
     *
     * @property buildArtifactsUrl the URL for build artifacts
     * @property externalDependenciesUrl the URL for external dependencies
     * @property user the credentials user provider
     * @property token the credentials token provider
     */
    private data class RepositoriesConfig(
        val buildArtifactsUrl: String,
        val externalDependenciesUrl: String,
        val user: Provider<String>,
        val token: Provider<String>
    ) {
        companion object {
            private const val BUILD_ARTIFACTS_URL = "https://pkgs.dev.azure.com/jamarston/762ffd9e-ca64-466d-84e9-7a0e42e5d89a/_packaging/BuildArtifacts/maven/v1"
            private const val EXTERNAL_DEPENDENCIES_URL = "https://pkgs.dev.azure.com/jamarston/762ffd9e-ca64-466d-84e9-7a0e42e5d89a/_packaging/ExternalDependencies/maven/v1"

            /**
             * Creates a [RepositoriesConfig] from the given [ProviderFactory].
             *
             * @param providers the [ProviderFactory] to use
             * @return a new [RepositoriesConfig]
             */
            fun from(providers: ProviderFactory) = RepositoriesConfig(
                buildArtifactsUrl = BUILD_ARTIFACTS_URL,
                externalDependenciesUrl = EXTERNAL_DEPENDENCIES_URL,
                user = providers.requiredEnv("ARTIFACTS_REPO_USER"),
                token = providers.requiredEnv("ARTIFACTS_REPO_TOKEN")
            )
        }
    }

    /**
     * Compares two URLs for equality, ignoring trailing slashes.
     *
     * @param left the first URL
     * @param right the second URL
     * @return true if the URLs are equal ignoring trailing slashes
     */
    private fun sameUrl(left: String, right: String): Boolean {
        return left.trimEnd('/') == right.trimEnd('/')
    }

}

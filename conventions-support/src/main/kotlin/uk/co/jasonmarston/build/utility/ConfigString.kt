package uk.co.jasonmarston.build.utility

import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory

/**
 * Resolves a configuration string from Gradle properties, system properties, or environment variables.
 *
 * <p>Resolution order:
 * <ol>
 *   <li>Gradle property [prop]</li>
 *   <li>System property [prop]</li>
 *   <li>Environment variable [prop]</li>
 *   <li>Environment variable [prop] with dots replaced by underscores</li>
 *   <li>Environment variable [prop] with dots replaced by underscores and uppercased</li>
 * </ol>
 *
 * @param prop the property or environment variable name to resolve
 * @param allowNull if true, returns a provider that may be missing; if false, throws if not found
 * @return a [Provider] for the resolved value
 * @throws IllegalStateException if [allowNull] is false and no value is found
 */
@Suppress("unused")
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

/**
 * Returns a [Provider] for a required environment variable.
 *
 * @param name the environment variable name
 * @return a [Provider] for the environment variable value
 * @throws IllegalStateException if the environment variable is not set
 */
fun ProviderFactory.requiredEnv(name: String): Provider<String> {
    return environmentVariable(name).orElse(provider { error("$name must be set") })
}

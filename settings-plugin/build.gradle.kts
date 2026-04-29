plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `maven-publish`
}


dependencies {
    implementation(project(":conventions-support"))
    testImplementation(kotlin("test"))
}


kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

gradlePlugin {
    plugins {
        create("artifactsSettings") {
            id = "uk.co.jasonmarston.standards.settings"
            implementationClass = "uk.co.jasonmarston.build.settings.ArtifactsSettingsPlugin"
        }
    }
}

tasks.test {
    useJUnitPlatform()
}


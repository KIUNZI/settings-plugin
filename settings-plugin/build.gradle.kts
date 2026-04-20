plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

dependencies {
    implementation(project(":conventions-support"))
}

kotlin {
    jvmToolchain(17)
}

gradlePlugin {
    plugins {
        create("artifactsSettings") {
            id = "uk.co.jasonmarston.standards.settings"
            implementationClass = "uk.co.jasonmarston.build.settings.ArtifactsSettingsPlugin"
        }
    }
}

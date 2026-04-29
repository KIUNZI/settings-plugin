import org.gradle.api.publish.maven.MavenPublication

plugins {
    kotlin("jvm")
    `java-library`
    `maven-publish`
    id("org.jetbrains.dokka") version "1.9.20"
}


dependencies {
    implementation(gradleApi())
    testImplementation(kotlin("test"))
    dokkaPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.9.20")
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    withSourcesJar()
}

tasks.register<Jar>("dokkaHtmlJar") {
    group = "documentation"
    archiveClassifier.set("javadoc")
    from(tasks.dokkaHtml)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks.named("dokkaHtmlJar"))
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

// Workaround: Dokka does not support Gradle configuration cache yet
// See: https://github.com/Kotlin/dokka/issues/1217
if (project.tasks.findByName("dokkaHtml") != null) {
    tasks.named("dokkaHtml", org.jetbrains.dokka.gradle.DokkaTask::class) {
        @Suppress("DEPRECATION")
        outputDirectory.set(buildDir.resolve("dokka"))
        notCompatibleWithConfigurationCache("Dokka does not support configuration cache yet")
    }
}

sourceSets {
    main {
        java.srcDirs("src/main/kotlin")
    }
    test {
        java.srcDirs("src/test/kotlin")
    }
}

import org.gradle.api.publish.maven.MavenPublication

plugins {
    kotlin("jvm")
    `java-library`
    `maven-publish`
}


dependencies {
    implementation(gradleApi())
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}

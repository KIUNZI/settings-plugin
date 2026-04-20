plugins {
    kotlin("jvm")
    `java-library`
}

group = "uk.co.jasonmarston.kiunzi"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(gradleApi())
}

kotlin {
    jvmToolchain(17)
}

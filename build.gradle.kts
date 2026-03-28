plugins {
    kotlin("jvm") version "2.3.10"
    `java-library`
    `maven-publish`
}

group = "com.aadith"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            groupId = project.group.toString()
            artifactId = "sample-kai-fuzzer"
            version = project.version.toString()
        }

        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/AJThePro99/sample-kotlin-library")
                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

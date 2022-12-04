plugins {
    kotlin("jvm") version "1.7.22"
    `java-library`
    `maven-publish`

    id("org.jmailen.kotlinter") version "3.12.0"
    id("org.jetbrains.dokka") version "1.7.20"
}

group = "com.lapanthere"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("reflect"))

    val flinkVersion = "1.16.0"
    implementation("org.apache.flink:flink-java:$flinkVersion")

    testImplementation("org.apache.flink:flink-test-utils:$flinkVersion")
    testImplementation("org.apache.flink:flink-core:$flinkVersion:tests")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.vintage:junit-vintage-engine")
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

kotlin {
    explicitApi()
}

kotlinter {
    disabledRules = arrayOf("import-ordering")
}

publishing {
    repositories {
        maven {
            name = "Github"
            url = uri("https://maven.pkg.github.com/cyberdelia/flink-kotlin")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }

    publications {
        create<MavenPublication>("default") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
            pom {
                name.set("flink-kotlin")
                description.set("Kotlin extensions for Apache Flink")
                url.set("https://github.com/cyberdelia/flink-kotlin")
                scm {
                    connection.set("scm:git:git://github.com/cyberdelia/flink-kotlin.git")
                    developerConnection.set("scm:git:ssh://github.com/cyberdelia/flink-kotlin.git")
                    url.set("https://github.com/cyberdelia/flink-kotlin")
                }
            }
        }
    }
}

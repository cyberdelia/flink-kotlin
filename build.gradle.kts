import java.net.HttpURLConnection
import java.net.URI
import java.util.Base64

plugins {
    kotlin("jvm") version "2.3.10"
    `java-library`
    `maven-publish`
    signing

    id("org.jmailen.kotlinter") version "5.4.2"
    id("org.jetbrains.dokka") version "2.1.0"
}

group = "com.lapanthere"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("reflect"))

    val flinkVersion = "1.20.3"
    implementation("org.apache.flink:flink-java:$flinkVersion")

    testImplementation("org.apache.flink:flink-test-utils:$flinkVersion")
    testImplementation("org.apache.flink:flink-core:$flinkVersion:tests")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.vintage:junit-vintage-engine")
}

tasks.named<Test>("test") {
    useJUnitPlatform()

    maxHeapSize = "1G"

    testLogging {
        events("passed")
    }
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

tasks.register<Jar>("javadocJar") {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    archiveClassifier.set("javadoc")
    from(tasks.named("dokkaGenerate"))
}

tasks.withType<PublishToMavenRepository> {
    if (name == "Github") {
        return@withType
    }

    val namespace = "com.lapanthere"
    val url = URI("https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/$namespace").toURL()
    val token = Base64.getEncoder().encodeToString("${System.getenv("OSSRH_USERNAME")}:${System.getenv("OSSRH_PASSWORD")}".toByteArray())

    doLast("closeOssrhRepository") {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("User-Agent", "Gradle/${project.gradle.gradleVersion}")
        }

        if (connection.responseCode != 200) {
            throw GradleException("Failed to close staging repository: ${connection.responseCode} ${connection.responseMessage}")
        }

        connection.disconnect()
    }
}

dokka {
    moduleName.set("flink-kotlin")
    dokkaSourceSets.main {
        sourceLink {
            localDirectory.set(file("src/main/kotlin"))
            remoteUrl("https://github.com/cyberdelia/flink-kotlin")
        }
    }
}

kotlin {
    explicitApi()
}

publishing {
    repositories {
        if (System.getenv("GITHUB_ACTOR") != null && System.getenv("GITHUB_TOKEN") != null) {
            maven {
                name = "Github"
                url = uri("https://maven.pkg.github.com/cyberdelia/flink-kotlin")
                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        }
        if (System.getenv("OSSRH_USERNAME") != null && System.getenv("OSSRH_PASSWORD") != null) {
            maven {
                name = "OSSRH"
                url = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
                credentials {
                    username = System.getenv("OSSRH_USERNAME")
                    password = System.getenv("OSSRH_PASSWORD")
                }
            }
        }
    }

    publications {
        create<MavenPublication>("default") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
            pom {
                name.set("flink-kotlin")
                description.set("Kotlin extensions for Apache Flink")
                url.set("https://github.com/cyberdelia/flink-kotlin")
                scm {
                    connection.set("scm:git:git://github.com/cyberdelia/flink-kotlin.git")
                    developerConnection.set("scm:git:ssh://github.com/cyberdelia/flink-kotlin.git")
                    url.set("https://github.com/cyberdelia/flink-kotlin")
                }
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("http://www.opensource.org/licenses/mit-license.php")
                    }
                }
                developers {
                    developer {
                        id.set("cyberdelia")
                        name.set("Timothée Peignier")
                        email.set("tim@lapanthere.com")
                        organization.set("La Panthère")
                        organizationUrl.set("https://lapanthere.com")
                    }
                }
            }
        }
    }
}

signing {
    useInMemoryPgpKeys(
        System.getenv("GPG_PRIVATE_KEY"),
        System.getenv("GPG_PASSPHRASE")
    )
    sign(publishing.publications)
}

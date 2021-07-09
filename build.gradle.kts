import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    id("io.gitlab.arturbosch.detekt") version "1.5.1"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("org.jetbrains.kotlin.jvm") version "1.5.10"
    id("org.springframework.boot") version "2.5.0"
    id("maven-publish")
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "java-test-fixtures")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "maven-publish")


    group = "ch.loewenfels.issuetrackingsync"
    version = "2.0-SNAPSHOT"

    java {
        withSourcesJar()
        sourceCompatibility = JavaVersion.VERSION_11
    }

    repositories {
        mavenCentral()
        maven { url = uri("https://packages.atlassian.com/mvn/maven-external/") }
    }

    dependencies {
        implementation(kotlin("stdlib"))

        testImplementation("org.hamcrest:hamcrest:2.2")
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
        testImplementation("org.springframework.boot:spring-boot-starter-test") {
            exclude(module = "junit-vintage-engine")
        }
        testImplementation("org.mockito:mockito-core:3.10.0")
        testImplementation("org.springframework.security:spring-security-test")

        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.2")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "11"
        }
    }

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                artifactId = project.name
                from(components["java"])
                versionMapping {
                    usage("java-api") {
                        fromResolutionOf("runtimeClasspath")
                    }
                    usage("java-runtime") {
                        fromResolutionResult()
                    }
                }
                pom {
                    name.set("Issue Tracking Sync")
                    scm {
                        connection.set("scm:git:https://github.com/loewenfels/issue-tracking-sync.git")
                        developerConnection.set("scm:git:https://github.com/loewenfels/issue-tracking-sync.git")
                        url.set("https://github.com/loewenfels/issue-tracking-sync")
                    }
                }
            }
        }
    }
}

dependencies {
    implementation(project(":framework"))
    implementation(project(":jira-client"))
}

detekt {
    failFast = true
    config = files("$projectDir/detekt-config.yml")

    reports {
        html.enabled = true // observe findings in your browser with structure and code snippets
        xml.enabled = true // checkstyle like format mainly for integrations like Jenkins, Sonar etc.
    }
}

tasks.withType<BootRun> {
    main = ("ch.loewenfels.issuetrackingsync.app.IssueTrackingSyncApp")
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar> {
    mainClassName = "ch.loewenfels.issuetrackingsync.app.IssueTrackingSyncApp"
}


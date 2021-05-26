import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

group = "ch.loewenfels.issuetrackingsync"
version = "1.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
    maven { url = uri("https://packages.atlassian.com/mvn/maven-external/") }
}

dependencies {
    implementation(project(":framework"))
    implementation(kotlin("stdlib"))
    implementation("com.atlassian.jira:jira-rest-java-client-core:5.2.2")
    implementation("com.atlassian.renderer:atlassian-renderer:8.0.5") {
        exclude("javax.activation:activation:1.0.2")
    }
    implementation("io.atlassian.fugue:fugue:4.7.2")
    implementation("javax.activation:activation:1.1")
    implementation("org.jsoup:jsoup:1.13.1")
    implementation("org.springframework.boot:spring-boot-starter-web")

    testImplementation(project(":test-utils"))
    testImplementation("org.hamcrest:hamcrest:2.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testImplementation("org.mockito:mockito-core:3.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}
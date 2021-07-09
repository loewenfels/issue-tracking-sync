import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot")
}

dependencies {
    testFixturesImplementation(project(":framework"))

    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-activemq")
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-security")
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "junit-vintage-engine")
    }
}


tasks.getByName<BootJar>("bootJar") {
    enabled = false
}

tasks.getByName<Jar>("jar") {
    enabled = true
    classifier = ""
}
dependencies {
    implementation(project(":framework"))
    implementation(project(":jira-client"))
    implementation(project(":rtc-client"))

    implementation(fileTree(mapOf("dir" to "../../libs", "include" to listOf("*.jar"))))
    implementation("com.atlassian.jira:jira-rest-java-client-core:5.2.2")
    implementation("joda-time:joda-time:2.10.10")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation(testFixtures(project(":test-utils")))
}


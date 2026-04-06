import com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

buildscript {
    dependencies {
        classpath("org.openapitools:openapi-generator-gradle-plugin:7.11.0")
    }
}
plugins {
    java
    id("application")
    id("org.springframework.boot") version "3.4.13"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.openapi.generator") version "7.11.0"
    id("com.google.cloud.tools.jib") version "3.4.5"
    id("com.diffplug.spotless") version "6.25.0" apply true
    id("net.ltgt.errorprone") version "5.1.0"
    id("com.avast.gradle.docker-compose") version "0.17.21"
}

application {
    mainClass = "cm.domeni.authentis_users.AuthentisUsersApplication"
}

group = "cm.domeni.authentis-users"
version = "0.0.1-SNAPSHOT"
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}
repositories {
    mavenLocal()
    mavenCentral()
}
configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}
val kapitaPlatformVersion =
    providers
        .gradleProperty("kapitaPlatformVersion")
        .orElse(providers.environmentVariable("KAPITA_PLATFORM_VERSION"))
        .orElse("0.1.1-SNAPSHOT")
        .get()
val springCloudVersion = "2024.0.3"
val testContainerVersion = "1.20.4"
val mapstructVersion = "1.6.3"
val cucumberVersion = "7.20.1"
val lombokVersion = "1.18.42"
val errorProneVersion = "2.48.0"
val nullAwayVersion = "0.13.1"
tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("--enable-preview", "-Dnet.bytebuddy.experimental=true")
}

val nexusMavenPublicUrl =
    providers
        .gradleProperty("nexusMavenPublicUrl")
        .orElse(providers.environmentVariable("NEXUS_MAVEN_PUBLIC_URL"))
        .orElse(providers.environmentVariable("NEXUS_MAVEN_URL"))
        .orElse(providers.environmentVariable("NEXUS_MAVEN_RELEASES_URL"))
        .orElse(providers.environmentVariable("NEXUS_MAVEN_SNAPSHOTS_URL"))
        .orNull

val nexusUsername =
    providers
        .gradleProperty("nexusUsername")
        .orElse(providers.environmentVariable("NEXUS_CREDENTIALS_USR"))
        .orNull

val nexusPassword =
    providers
        .gradleProperty("nexusPassword")
        .orElse(providers.environmentVariable("NEXUS_CREDENTIALS_PSW"))
        .orNull
repositories {
    mavenLocal()
    mavenCentral()
    if (!nexusMavenPublicUrl.isNullOrBlank()) {
        maven {
            url = uri(nexusMavenPublicUrl)
            isAllowInsecureProtocol = nexusMavenPublicUrl.startsWith("http://")
            credentials {
                username = nexusUsername ?: ""
                password = nexusPassword ?: ""
            }
        }
    }
}
dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
    }
}
dependencies {
    implementation(platform("com.domeni.kapita:kapita-platform-bom:$kapitaPlatformVersion"))
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.0.0")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.domeni.kapita:kapita-jpa-eclipselink-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.cloud:spring-cloud-starter")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-bootstrap")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.liquibase:liquibase-core")
    implementation("com.domeni.kapita:kapita-kafka-outbox-starter")

    // Security
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.keycloak:keycloak-admin-client:24.0.4")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:3.1.0")
    testImplementation("org.springframework.security:spring-security-test")

    // kafka
    testImplementation("org.testcontainers:kafka:$testContainerVersion")
    testImplementation("org.springframework.kafka:spring-kafka-test")

    // DB
    implementation("org.postgresql:postgresql")
    testImplementation("org.testcontainers:postgresql:$testContainerVersion")
    implementation("org.jspecify:jspecify:1.0.0")
    errorprone("com.google.errorprone:error_prone_core:$errorProneVersion")
    errorprone("com.uber.nullaway:nullaway:$nullAwayVersion")
    // OPENAPI
    implementation("io.swagger:swagger-annotations:1.6.11")
    implementation("org.openapitools:jackson-databind-nullable:0.2.6")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.1.0")
    testImplementation("org.springframework.boot:spring-boot-docker-compose")
    testImplementation("org.testcontainers:testcontainers:$testContainerVersion")
    // Lombok
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")
    testCompileOnly("org.projectlombok:lombok:$lombokVersion")
    testAnnotationProcessor("org.projectlombok:lombok:$lombokVersion")

    // Mapstruct
    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")
    testAnnotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.junit.platform:junit-platform-suite:1.11.4")
    testImplementation("org.wiremock:wiremock-standalone:3.13.1")
    testImplementation("io.cucumber:cucumber-java:$cucumberVersion")
    testImplementation("io.cucumber:cucumber-spring:$cucumberVersion")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:$cucumberVersion")
    testRuntimeOnly("com.h2database:h2")

    testImplementation("io.rest-assured:rest-assured:5.3.2")
    testImplementation("io.rest-assured:spring-mock-mvc:5.4.0")
    implementation("org.awaitility:awaitility:4.2.0")
    implementation("com.google.guava:guava:33.2.1-jre")
}

tasks.named<JavaCompile>("compileJava") {
    options.compilerArgs.add("--enable-preview")
    options.errorprone {
        excludedPaths.set(".*/build/generated/sources/.*")
        check("NullAway", CheckSeverity.ERROR)
        option("NullAway:AnnotatedPackages", "cm.lao.marketplace")
        option("NullAway:JSpecifyMode", true)
        option("NullAway:TreatGeneratedAsUnannotated", true)
        option("NullAway:CheckOptionalEmptiness", true)
        option("NullAway:SuggestSuppressions", true)
        option(
            "NullAway:ExcludedClasses",
            "cm.lao.marketplace.SpringBootTemplateApplication,cm.lao.marketplace.config",
        )
        option(
            "NullAway:ExcludedClassAnnotations",
            "jakarta.persistence.Converter,org.mapstruct.Mapper,org.springframework.boot.test.context.SpringBootTest,org.junit.jupiter.api.Test,org.junit.jupiter.api.extension.ExtendWith,org.junit.jupiter.api.BeforeEach,org.mockito.Mock,org.springframework.context.annotation.Configuration,org.springframework.boot.autoconfigure.SpringBootApplication",
        )
        option("NullAway:CustomNullableAnnotations", "org.springframework.lang.Nullable,jakarta.annotation.Nullable")
        option("NullAway:CustomNonnullAnnotations", "org.springframework.lang.NonNull,jakarta.annotation.Nonnull,lombok.NonNull")
        option("NullAway:HandleTestAssertionLibraries", true)
        option(
            "NullAway:ExcludedFieldAnnotations",
            "org.springframework.beans.factory.annotation.Autowired,lombok.NonNull,jakarta.inject.Inject",
        )
        option(
            "NullAway:ExcludedClasses",
            listOf(
                "cm.domeni.authentis_user.AuthentisUsersApplication",
                "cm.domeni.authentis_user.config",
                "cm.domeni.authentis_user.api.**",
                "cm.domeni.authentis_user.dto.**",
                "**.*Test",
            ).joinToString(","),
        )
    }
}

tasks.named<JavaCompile>("compileTestJava") {
    options.compilerArgs.add("--enable-preview")
    options.errorprone.enabled.set(false)
}

tasks.test {
    useJUnitPlatform {
        excludeTags("e2e", "data")
    }
    filter {
        excludeTestsMatching("cm.domeni.authentis_users.e2e.*")
    }
}

tasks.register<Test>("dataTest") {
    ignoreFailures = true
    val testSourceSet = sourceSets.named("test").get()
    testClassesDirs = testSourceSet.output.classesDirs
    classpath = testSourceSet.runtimeClasspath
    dependsOn("assemble", "testClasses")
    useJUnitPlatform {
        includeTags("data")
    }
}

tasks.register<Test>("e2eTest") {
    val testSourceSet = sourceSets.named("test").get()
    testClassesDirs = testSourceSet.output.classesDirs
    classpath = testSourceSet.runtimeClasspath
    dependsOn("assemble", "testClasses")
    useJUnitPlatform {
        includeTags("e2e")
    }
    filter {
        includeTestsMatching("cm.domeni.authentis_users.e2e.CucumberE2ETest")
    }
}

interface InjectedExecOps {
    @get:Inject val execOps: ExecOperations
}

tasks.named<GenerateTask>("openApiGenerate") {
    generatorName.set("spring")
    templateDir.set("$rootDir/openapi/templates/spring-boot")
    inputSpec.set("$rootDir/openapi/main.yaml")
    outputDir.set(
        layout.buildDirectory
            .dir("generated/sources/openapi")
            .get()
            .asFile.path,
    )
    apiPackage.set("cm.domeni.authentis_users.api")
    modelPackage.set("cm.domeni.authentis_users.dto")
    configOptions.set(
        mapOf(
            "dateLibrary" to "java8-localdatetime",
            "library" to "spring-boot",
            "interfaceOnly" to "true",
            "useTags" to "true",
            "skipDefaultInterface" to "true",
            "useSpringBoot3" to "true",
        ),
    )
    typeMappings.set(
        mapOf(
            "time" to "java.time.LocalTime",
        ),
    )
    val generatedSourceCodeDir = file(outputDir.get() + "/src/main/java/cm/domeni/authentis-users")
    doFirst {
        generatedSourceCodeDir.deleteRecursively()
    }
    onlyIf {
        val generatedLastModified = generatedSourceCodeDir.lastModified()
        val templateLastModified = templateDir.orNull?.let { file(it).lastModified() } ?: 0L
        !generatedSourceCodeDir.exists() ||
            file(inputSpec.get()).lastModified() > generatedLastModified ||
            templateLastModified > generatedLastModified
    }
}

tasks.register<GenerateTask>("mainDomainEventsOpenApiGenerate") {
    generatorName.set("spring")
    templateDir.set("$rootDir/openapi/templates/spring-boot")
    inputSpec.set("$rootDir/openapi/domain-event.yaml")
    outputDir.set(
        layout.buildDirectory
            .dir("generated/sources/openapi")
            .get()
            .asFile.path,
    )
    modelPackage.set("cm.domeni.authentis_users.event.dto")
    configOptions.set(
        mapOf(
            "dateLibrary" to "java8-localdatetime",
            "library" to "spring-boot",
            "interfaceOnly" to "true",
            "useTags" to "true",
            "skipDefaultInterface" to "true",
            "useSpringBoot3" to "true",
        ),
    )
    typeMappings.set(
        mapOf(
            "time" to "java.time.LocalTime",
        ),
    )
    val generatedSourceCodeDir =
        file(outputDir.get() + "/src/main/java/cm/domeni/authentis_users/event/dto")

    doFirst {
        generatedSourceCodeDir.deleteRecursively()
    }
    outputs.upToDateWhen { false }
}

tasks.compileJava.get().dependsOn(
    tasks["openApiGenerate"],
    tasks["mainDomainEventsOpenApiGenerate"],
)

sourceSets.main
    .get()
    .java
    .srcDir(
        layout.buildDirectory
            .dir("generated/sources/openapi/src/main/java")
            .get()
            .asFile.path,
    )

jib {
    val imageNamePrefix = System.getenv("NEXUS_DOCKER_REGISTRY_URL") ?: ""
    val nexusUsername = System.getenv("NEXUS_CREDENTIALS_USR") ?: ""
    val nexusPassword = System.getenv("NEXUS_CREDENTIALS_PSW") ?: ""
    from {
        image = "eclipse-temurin:25-jdk"
    }
    to {
        image = "$imageNamePrefix/${project.name}"
        tags = setOf("${project.version}")
        auth {
            username = nexusUsername
            password = nexusPassword
        }
    }
    container {
        creationTime = "USE_CURRENT_TIMESTAMP"
        jvmFlags = listOf("--enable-preview")
    }
}

spotless {
    java {
        targetExclude("build/**")
        toggleOffOn()
        googleJavaFormat("1.25.2")
            .reflowLongStrings()
            .formatJavadoc(true)
            .reorderImports(true)
            .groupArtifact("com.google.googlejavaformat:google-java-format")
    }
    kotlin {
        targetExclude("build/**")
        target("**/*.kts")
        ktlint("1.5.0")
    }
    format("xml", {
        targetExclude("build/**")
        target("src/**/*.xml")
        eclipseWtp(EclipseWtpFormatterStep.XML)
    })

    yaml {
        targetExclude("build/**")
        target("src/*/resources/**/*.yaml", "src/*/resources/**/*.yml", "openapi/main.yaml")
        targetExclude("src/test/resources/docker-compose.yml")
        jackson()
            .feature("ORDER_MAP_ENTRIES_BY_KEYS", true)
    }
    gherkin {
        targetExclude("build/**")
        target("src/test/resources/**/*.feature")
        gherkinUtils()
            .version("9.0.0")
    }
}
dockerCompose {
    useComposeFiles.set(listOf("docker-compose.yml"))
    stopContainers.set(true)
    removeVolumes.set(false)
    waitForTcpPorts.set(true)
}

tasks.named("run") {
    dependsOn("composeUp")
    finalizedBy("composeDown")
}

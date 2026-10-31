import org.jetbrains.kotlin.gradle.dsl.JvmTarget

group = "de.simpletactics"
plugins {
    java
    `maven-publish`

    // Kotlin
    kotlin("jvm")
    kotlin("plugin.spring")

    id("org.flywaydb.flyway")
    id("org.springframework.boot")
    id("com.gorylenko.gradle-git-properties")
    id("com.github.ben-manes.versions")
    id("org.openapi.generator")
}

apply(plugin = "io.spring.dependency-management")
apply(plugin = "org.springframework.boot")

// Set before the publishing block below reads it: in a Kotlin DSL script,
// script order is evaluation order.
if (project.hasProperty("projVersion")) {
    project.version = project.property("projVersion") as String
} else {
    project.version = "0.0.1-SNAPSHOT"
}

val nexusSnapshotUrl: String by project
val nexusUrl: String by project
val nexusUser: String by project
val nexusPassword: String by project

repositories {
    mavenCentral()
    maven {
        name = "nexus"
        url = uri(nexusUrl)
        credentials {
            username = nexusUser
            password = nexusPassword
        }
    }
    maven {
        name = "nexus"
        url = uri(nexusSnapshotUrl)
        credentials {
            username = nexusUser
            password = nexusPassword
        }
    }
}

// ---------------------------------------------------------------- OpenAPI
//
// wiki-api.yaml is the contract, not a by-product: the request/response models
// and the controller interfaces below are generated from it, and sita-backend
// generates its client from the very same file (published to Nexus by the
// `specJar` publication). Hand-writing the DTOs on either side is what let them
// drift before.

val wikiSpec = layout.projectDirectory.file("src/main/resources/openapi/wiki-api.yaml")
val serverGenDir = layout.buildDirectory.dir("generated/openapi")

openApiGenerate {
    generatorName.set("kotlin-spring")
    inputSpec.set(wikiSpec.asFile.path)
    outputDir.set(serverGenDir.get().asFile.path)
    apiPackage.set("de.simpletactics.wiki.api")
    modelPackage.set("de.simpletactics.wiki.api.dto")
    configOptions.set(
        mapOf(
            // Interfaces only - the existing controllers implement them, so the
            // generator never owns the behaviour, just the signatures.
            "interfaceOnly" to "true",
            "useSpringBoot3" to "true",
            "useTags" to "true",
            "useBeanValidation" to "true",
            "serializationLibrary" to "jackson",
            "documentationProvider" to "none",
            "enumPropertyNaming" to "UPPERCASE",
        )
    )
}

openApiValidate {
    inputSpec.set(wikiSpec.asFile.path)
    recommend.set(true)
}

sourceSets.main {
    kotlin.srcDir(serverGenDir.map { it.dir("src/main/kotlin") })
}

tasks.compileKotlin { dependsOn(tasks.openApiGenerate) }

/**
 * The contract, published on its own so sita-backend can generate a client from
 * the exact file this service was built against. Deliberately not the
 * application jar: consumers need the spec, not our code - shipping the code is
 * what the old wiki-lib did, and is what this change exists to undo.
 */
val specJar by tasks.registering(Jar::class) {
    archiveBaseName.set("wiki-api-spec")
    from(wikiSpec) { into("openapi") }
}

publishing {
    publications {
        create<MavenPublication>("spec") {
            groupId = "de.simpletactics"
            artifactId = "wiki-api-spec"
            version = project.version.toString()
            artifact(specJar)
        }
    }
    repositories {
        maven {
            name = "nexus"
            // Chosen explicitly rather than sniffed from the version string. A dev
            // build is versioned 1.4.0-g1a2b3c4, which contains no "SNAPSHOT" and
            // would otherwise land immutably in the releases repository.
            url = when (val repo = (findProperty("nexusRepo") as String?) ?: "snapshots") {
                "releases" -> uri(nexusUrl)
                "snapshots" -> uri(nexusSnapshotUrl)
                else -> error("nexusRepo must be 'releases' or 'snapshots', got '$repo'.")
            }
            credentials {
                username = nexusUser
                password = nexusPassword
            }
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21

    withSourcesJar()
    //withJavadocJar()
}

// withSourcesJar() packages the generated sources too, so it has to wait for them.
tasks.named("sourcesJar") { dependsOn(tasks.openApiGenerate) }

dependencies {
    // Spring boot
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webflux")


    // Spring Security (version managed by the Spring Boot BOM)
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Source: https://mvnrepository.com/artifact/com.fasterxml.jackson.module/jackson-module-kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Prometheus
    implementation("io.micrometer:micrometer-registry-prometheus:1.14.1")

    // Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")

    // Database
    val flyway: String by project
    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core:$flyway")
    implementation("org.flywaydb:flyway-database-postgresql:$flyway")

    // Testing
    // Spring boot testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    testImplementation("org.testcontainers:testcontainers:2.0.3")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("io.mockk:mockk:1.13.16")
    testImplementation("com.ninja-squad:springmockk:4.0.2")

    val kotestVersion: String by project
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest.extensions:kotest-extensions-spring:1.1.3")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.bootJar {
    exclude("**/application-secrets.*", "**/application-local.*", "**/img")

    // Stable name: build/libs also holds the plain and sources jars, and a
    // glob in the Dockerfile would pick among them by chance.
    archiveFileName.set("wiki-service.jar")
}

tasks.compileTestKotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.compileKotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}

tasks.wrapper {
    val versionGradle: String by project
    gradleVersion = versionGradle
}

tasks.register("bootRunLocal") {
    group = "application"
    description = "Runs the Spring Boot application with the local profile"
    doFirst {
        tasks.bootRun.configure {
            systemProperty("spring.profiles.active", "local,secrets")
        }
    }
    finalizedBy("bootRun")
}

tasks.register<Exec>("initializeDatabase") {
    group = "database"
    description = "Local database initialization"

    commandLine("bash", "init/init_db.sh")
}

tasks.register<Test>("unit-test") {
    group = "test"
    description = "Runs tests that need no database"

    // Custom Test tasks have to be told where the tests are; relying on the
    // convention is deprecated and stops working in Gradle 9.
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath

    filter {
        includeTestsMatching("*Test")
        excludeTestsMatching("*IT")
    }
}

tasks.register<Test>("integration-test") {
    group = "test"
    description = "Runs the Testcontainers-backed tests (Docker required)"

    // Custom Test tasks have to be told where the tests are; relying on the
    // convention is deprecated and stops working in Gradle 9.
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath

    filter {
        includeTestsMatching("*IT")
    }
}

buildscript {
    val flyway: String by project
    dependencies {
        classpath("org.postgresql:postgresql:42.7.4")
        classpath("org.flywaydb:flyway-database-postgresql:$flyway")
    }
}

/**
 * Only for ad-hoc `./gradlew flywayMigrate` against the local compose database;
 * the application runs Flyway itself on startup. Override with -P properties.
 */
flyway {
    url = (project.findProperty("flywayUrl") as String?) ?: "jdbc:postgresql://localhost:5433/wiki"
    user = (project.findProperty("flywayUser") as String?) ?: "postgres"
    password = (project.findProperty("flywayPassword") as String?) ?: "db200"
    cleanDisabled = true
    baselineOnMigrate = true
    schemas = arrayOf("wiki")
    defaultSchema = "wiki"
}

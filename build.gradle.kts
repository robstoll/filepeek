import com.jfrog.bintray.gradle.BintrayExtension
import info.solidsoft.gradle.pitest.PitestPluginExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val junit5Version = "5.5.0"
val junitPlatformVersion = "1.5.0"
val kotlinVersion = "1.3.41"

plugins {
    java
    kotlin("jvm") version "1.3.41"
    id("com.github.ben-manes.versions") version "0.21.0"
    `maven-publish`
    id("com.jfrog.bintray") version "1.8.4"
    id("info.solidsoft.pitest") version "1.4.0"

}

group = "com.christophsturm"
version = "0.1.1"

buildscript {
    configurations.maybeCreate("pitest")
    dependencies {
        "pitest"("org.pitest:pitest-junit5-plugin:0.9")
    }
}


repositories {
    //    maven { setUrl("http://dl.bintray.com/kotlin/kotlin-eap") }
    jcenter()
    mavenCentral()
}

dependencies {
    compile(kotlin("stdlib-jdk8", kotlinVersion))
    compile(kotlin("reflect", kotlinVersion))
    testImplementation("io.strikt:strikt-core:0.21.1")
    testImplementation("dev.minutest:minutest:1.7.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit5Version")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:$junitPlatformVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit5Version")

}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
    withType<Test> {
        useJUnitPlatform {
            includeEngines("junit-jupiter")
        }
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
    create<Jar>("sourceJar") {
        from(sourceSets.main.get().allSource)
        archiveClassifier.set("sources")
    }
}

artifacts {
    add("archives", tasks["jar"])
    add("archives", tasks["sourceJar"])
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks["sourceJar"])
            groupId = project.group as String
            artifactId = "filepeek"
            version = project.version as String
        }
    }
}

// BINTRAY_API_KEY= ... ./gradlew clean check publish bintrayUpload
bintray {
    user = "christophsturm"
    key = System.getenv("BINTRAY_API_KEY")
    publish = true
    setPublications("mavenJava")
    pkg(delegateClosureOf<BintrayExtension.PackageConfig> {
        repo = "maven"
        name = "filepeek"
        version(delegateClosureOf<BintrayExtension.VersionConfig> {
            name = project.version as String
        })
    })
}

plugins.withId("info.solidsoft.pitest") {
    configure<PitestPluginExtension> {
        jvmArgs = listOf("-Xmx512m")
        testPlugin = "junit5"
        avoidCallsTo = setOf("kotlin.jvm.internal")
        mutators = setOf("NEW_DEFAULTS")
        targetClasses = setOf("filepeek.*")  //by default "${project.group}.*"
        targetTests = setOf("filepeek.*", "filepeektest.*")
        pitestVersion = "1.4.9"
        threads = System.getenv("PITEST_THREADS")?.toInt() ?: Runtime.getRuntime().availableProcessors()
        outputFormats = setOf("XML", "HTML")
    }
}

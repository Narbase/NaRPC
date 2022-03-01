buildscript {

    repositories {
        mavenCentral()
        jcenter()
        maven { url = uri("https://plugins.gradle.org/m2/") }
    }

    dependencies {
        classpath(kotlin("gradle-plugin", version = "1.4.32"))
    }
}
val kotlinVersion = "1.4.32"
val ktorVersion = "1.5.4"

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlin.plugin.serialization")
    `maven-publish`
    signing
}
//group = "com.narbase.narpc"


repositories {
    mavenCentral()
    jcenter()
}

//sourceCompatibility = 1.8
//targetCompatibility = 1.8

//compileKotlin {
//    kotlinOptions.jvmTarget = "1.8"
//}
//compileTestKotlin {
//    kotlinOptions.jvmTarget = "1.8"
//}

val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)
val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
    dependsOn(dokkaHtml)
    archiveClassifier.set("javadoc")
    from(dokkaHtml.outputDirectory)
}

dependencies {
    api(project(":narpc-common"))
    implementation(platform("org.jetbrains.kotlin:kotlin-bom")) // Align versions of all Kotlin components
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8") // Use the Kotlin JDK 8 standard library.
    testImplementation("org.jetbrains.kotlin:kotlin-test") // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit") // Use the Kotlin JUnit integration.
    testImplementation(project(":narpc-client"))
    testImplementation("io.ktor:ktor-server-netty:$ktorVersion")
    testImplementation("io.ktor:ktor-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-gson:$ktorVersion")
//    implementation("io.ktor:ktor-serialization:$ktor_version")
//    implementation "org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.0"
}


afterEvaluate {
    publishing {
        publications.withType<MavenPublication> {
            artifact(javadocJar.get())
            pom {
                val projectGitUrl = "https://github.com/Narbase/NaRPC"
                name.set("NaRPC")
                description.set("Remote Procedure Call in Kotlin")
                url.set(projectGitUrl)
                inceptionYear.set("2021")
                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("islam")
                        name.set("Islam Abdalla")
                        email.set("islam@narbase.com")
                        organization.set("Narbase Technologies")
                    }
                    developer {
                        id.set("hind")
                        name.set("Hind Abulmaali")
                        email.set("hind@narbase.com")
                        organization.set("Narbase Technologies")
                    }
                    developer {
                        id.set("ayman")
                        name.set("Ayman Hassan")
                        email.set("ayman.hassan@narbase.com")
                        organization.set("Narbase Technologies")
                    }
                }
                issueManagement {
                    system.set("GitHub")
                    url.set("$projectGitUrl/issues")
                }
                scm {
                    connection.set("scm:git:$projectGitUrl")
                    developerConnection.set("scm:git:$projectGitUrl")
                    url.set(projectGitUrl)
                }
            }
            the<SigningExtension>().sign(this)
        }
        repositories {
            maven {
                name = "sonatypeStaging"
                url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = project.findProperty("SONATYPE_USERNAME") as? String
                    password = project.findProperty("NEXUS_PASSWORD") as? String
                }
            }
        }
    }
}

signing {
    useGpgCmd()
}


// To build and publish: ./gradlew clean build publish -Psigning.gnupg.keyName=<KeyId>
// Then manually go to https://oss.sonatype.org, close the staging repo and release

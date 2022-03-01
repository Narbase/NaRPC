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

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.dokka")
    `maven-publish`
    signing
}

repositories {
    mavenCentral()
    jcenter()
}

kotlin {
    jvm()
    js(BOTH) {
        browser {}
    }

}


val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)
val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
    dependsOn(dokkaHtml)
    archiveClassifier.set("javadoc")
    from(dokkaHtml.outputDirectory)
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

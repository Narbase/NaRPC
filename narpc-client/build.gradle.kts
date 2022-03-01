plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.dokka")
    `maven-publish`
    signing
}

val ktorVersion = "1.5.4"
val coroutinesVersion = "1.4.3"

repositories {
    jcenter()
}


kotlin {
    jvm()
    js(BOTH) {
        browser {
            testTask {
                useKarma {
                    useFirefox()
                }
            }
        }
    }


    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":narpc-common"))
                implementation(kotlin("stdlib-common"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation(kotlin("reflect"))
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-apache:$ktorVersion")
                implementation("io.ktor:ktor-client-serialization-jvm:$ktorVersion")
                implementation("io.ktor:ktor-client-gson:$ktorVersion")
//                implementation "org.jetbrains.kotlin:kotlin-reflect-jvm:1.3.72"
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$coroutinesVersion")
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }

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

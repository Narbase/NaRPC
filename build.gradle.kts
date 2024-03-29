plugins {
    id("org.jetbrains.kotlin.multiplatform") version "1.4.32" apply false
    id("org.jetbrains.kotlin.jvm") version "1.4.32" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.4.32" apply false
    id("org.jetbrains.dokka") version "1.4.32" apply false

    `maven-publish`
    signing
}

allprojects {
    group = "com.narbase.narpc"
    version = "0.1.12"
}

/*   task sourcesJar(type: Jar, dependsOn: classes) {
       archiveClassifier = 'sources'
       from sourceSets.main.allSource
   }

   task javadocJar(type: Jar, dependsOn: javadoc) {
       archiveClassifier = 'javadoc'
       from javadoc.destinationDir
   }

   artifacts {
       archives jar
       archives sourcesJar
       archives javadocJar
   }

   publishing {
       repositories {
           maven {
               def releaseRepo = "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
               def snapshotRepo = "https://oss.sonatype.org/content/repositories/snapshots/"
               url = isReleaseVersion ? releaseRepo : snapshotRepo
               credentials {
                   username = project.findProperty("SONATYPE_USERNAME")
                   password = project.findProperty("NEXUS_PASSWORD")
               }
           }
       }

       publications {
           mavenJava(MavenPublication) {

               artifact sourcesJar
               artifact javadocJar
               pom {
                   from components.java
                   name = 'NaRPC'
                   description = 'Remote Procedure Call in Kotlin'
                   url = 'https://github.com/Narbase/NaRPC'

                   scm {
                       connection = 'scm:git:git://github.com/Narbase/NaRPC.git'
                       developerConnection = 'scm:git:ssh://git@github.com/Narbase/NaRPC.git'
                       url = 'https://github.com/Narbase/NaRPC/'
                   }

                   licenses {
                       license {
                           name = 'MIT License'
                           url = 'http://www.opensource.org/licenses/mit-license.php'
                           distribution = 'repo'
                       }
                   }

                   developers {
                       developer {
                           name = "Ayman Hassan"
                           organization = "Narbase Technologies"
                           email = "ayman.hassan@narbase.com"
                       }
                       developer {
                           name = "Islam Abdalla"
                           organization = "Narbase Technologies"
                           email = "islam@narbase.com"
                       }
                   }
               }
           }
       }
   }

//    signing {
//        sign publishing.publications.mavenJava
//    }

   signing {
       sign configurations.archives
   }
*/


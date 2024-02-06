buildscript {
    val kotlinVersion: String by System.getProperties()

    repositories {
        mavenCentral()
    }

    dependencies {
        classpath ("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}

plugins {
    val kotlinVersion: String by System.getProperties()

    id ("org.jetbrains.kotlin.multiplatform") version kotlinVersion
    id ("org.jetbrains.dokka"               ) version "1.8.20"
    id ("maven-publish"                     )
    signing
}

repositories {
    mavenCentral()
}

kotlin {
    val releaseBuild = project.hasProperty("release")

    jvm().compilations.all {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
//    js {
//        browser {
//            testTask {
//                enabled = false
//            }
//        }
//    }.compilations.all {
//        kotlinOptions {
//            moduleKind = "umd"
//            sourceMap  = !releaseBuild
//            if (sourceMap) {
//                sourceMapEmbedSources = "always"
//            }
//        }
//    }

    val junitVersion: String by project

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
        watchosX64(),
        watchosArm64(),
        watchosSimulatorArm64(),
        macosX64(),
        macosArm64(),
        tvosX64(),
        tvosArm64(),
        tvosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "measured"
            isStatic = true
        }

        val isMacOS = System.getProperty("os.name") == "Mac OS X"
        val osVersion = System.getProperty("os.version").toDoubleOrNull() ?: 0.0

        // Use this flag if using MacOS 14 or newer
        if (isMacOS && osVersion >= 14.0) {
            it.compilations.all {
                compilerOptions.configure {
                    freeCompilerArgs.add("-linker-options")
                    freeCompilerArgs.add("-ld64")
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        jvm().compilations["test"].defaultSourceSet {
            dependencies {
                implementation("org.junit.jupiter:junit-jupiter:$junitVersion")
                implementation(kotlin("test-junit"))
            }
        }

//        js().compilations["test"].defaultSourceSet {
//            dependencies {
//                implementation(kotlin("test-js"))
//            }
//        }
    }
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

publishing {
    publications.withType<MavenPublication>().apply {
        all {
            artifact(javadocJar.get())
            pom {
                name.set       ("Measured"                           )
                description.set("Units of measure for Kotlin"        )
                url.set        ("https://github.com/nacular/measured")
                licenses {
                    license {
                        name.set("MIT"                                )
                        url.set ("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set  ("pusolito"     )
                        name.set("Nicholas Eddy")
                    }
                }
                scm {
                    url.set                ("https://github.com/nacular/measured.git"      )
                    connection.set         ("scm:git:git://github.com/nacular/measured.git")
                    developerConnection.set("scm:git:git://github.com/nacular/measured.git")
                }
            }
        }
    }

    repositories {
        maven {
            val releaseBuild = project.hasProperty("release")

            url = uri(when {
                releaseBuild -> "https://oss.sonatype.org/service/local/staging/deploy/maven2"
                else         -> "https://oss.sonatype.org/content/repositories/snapshots"
            })

            credentials {
                username = findProperty("suser")?.toString()
                password = findProperty("spwd" )?.toString()
            }
        }
    }
}

signing {
    setRequired({
        project.hasProperty("release") && gradle.taskGraph.hasTask("publish")
    })
//    useGpgCmd()
    sign(publishing.publications)
}

rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin> {
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>().download    = false
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>().nodeVersion = "16.0.0"
}

rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().disableGranularWorkspaces()
}
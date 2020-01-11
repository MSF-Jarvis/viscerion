/*
 * Copyright © 2017-2020 WireGuard LLC.
 * Copyright © 2018-2020 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import dev.msfjarvis.buildsrc.configureAndroid
import dev.msfjarvis.buildsrc.configureSpotless
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.github.ben-manes.versions") version "0.27.0"
    id("io.gitlab.arturbosch.detekt") version "1.3.1"
}

buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.5.0")
        classpath("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.3.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.61")
        classpath("com.diffplug.spotless:spotless-plugin-gradle:3.27.0")
    }
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    repositories {
        google()
        jcenter()
    }

    /*
    pluginManager.withPlugin("kotlin-android") {
        dependencies {
            implementation(Libs.Kotlin.stdlib8)
        }
    }


    pluginManager.withPlugin("kotlin-kapt") {
        kapt {
            useBuildCache = true
            // https://github.com/google/dagger/issues/1449#issuecomment-495404186
            javacOptions {
                option +"-source", "8"
                option "-target", "8"
            }
        }
    }
     */

    if (name == "app") {
        apply(plugin = "com.android.application")
    } else {
        apply(plugin = "com.android.library")
    }


    /*
    android {
        configureAndroid(this)
    }
    */

    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-Xlint:unchecked")
        options.isDeprecation = true
    }

    tasks.withType<KotlinCompile>().all {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }

    /*
    detekt {
        config = rootProject.files("detekt-config.yml")
        baseline = project.file("detekt-baseline.xml")
        parallel = true
    }
    */

    afterEvaluate {
        tasks.getByName(":check") {
            dependsOn(getTasksByName(":detekt", false))
        }
    }
}

project(":app") {
    repositories {
        maven(url = "https://jitpack.io")
    }
}

tasks {
    wrapper {
        distributionType = Wrapper.DistributionType.ALL
    }
}

tasks.register<Copy>("installHook") {
    from("ci")
    into(rootProject.file(".git/hooks/"))
    rename("pre-push-recommended.sh", "pre-push")
    fileMode = 755
}

configureSpotless()
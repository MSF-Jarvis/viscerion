/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

buildscript {
    repositories {
        google()
        jcenter()
        maven(url = "https://storage.googleapis.com/r8-releases/raw")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.5.0-beta03")
        classpath(kotlin("gradle-plugin", "1.3.31"))
        classpath("com.diffplug.spotless:spotless-plugin-gradle:3.23.0")
        classpath("com.android.tools:r8:1.5.42")
    }
}

plugins {
    id("com.github.ben-manes.versions") version "0.21.0"
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}

tasks {
    named<DependencyUpdatesTask>("dependencyUpdates") {
        checkForGradleUpdate = true
        outputFormatter = "json"
        outputDir = "build/dependencyUpdates"
        reportfileName = "report"
    }
    named<Wrapper>("wrapper") {
        gradleVersion = "5.5-rc-1"
        distributionType = Wrapper.DistributionType.ALL
    }
}

configureSpotless()

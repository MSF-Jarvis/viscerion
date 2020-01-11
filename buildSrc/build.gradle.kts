/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
  `kotlin-dsl`
}

repositories {
  google()
  maven("https://plugins.gradle.org/m2/")
  jcenter()
}

kotlinDslPluginOptions {
  experimentalWarning.set(false)
}

dependencies {
  implementation("com.diffplug.spotless:spotless-plugin-gradle:3.27.0")
  implementation("com.android.tools.build:gradle:3.5.0")
}

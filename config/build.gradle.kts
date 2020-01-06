/*
 * Copyright © 2017-2020 WireGuard LLC.
 * Copyright © 2018-2020 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import dev.msfjarvis.buildsrc.Libs

plugins {
  id("kotlin-android")
}

dependencies {
    api(project(":crypto"))
    implementation(Libs.ThirdParty.threetenabp)
    testImplementation(Libs.Testing.junit)
    testImplementation(project(path = ":test-resources", configuration = "testRes"))
}

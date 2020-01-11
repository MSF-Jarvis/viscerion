/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.msfjarvis.buildsrc

import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

private val kotlinLicenseHeader = """/*
 * Copyright © 2017-2020 WireGuard LLC.
 * Copyright © 2018-2020 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
""".trimIndent()

fun Project.configureSpotless() {
    apply<SpotlessPlugin>()

    configure<SpotlessExtension> {
        format("xml") {
            target("**/res/**/*.xml")
            indentWithSpaces(4)
            trimTrailingWhitespace()
            endWithNewline()
        }

        java {
            target(
                "app/src/main/java/com/wireguard/android/util/ObservableSortedKeyedArrayList.java",
                "app/src/main/java/com/wireguard/android/util/ObservableKeyedArrayList.java",
                "app/src/main/java/com/wireguard/android/util/KotlinCompanions.java"
            )
            trimTrailingWhitespace()
            @Suppress("INACCESSIBLE_TYPE")
            licenseHeader(kotlinLicenseHeader)
            removeUnusedImports()
            googleJavaFormat().aosp()
            endWithNewline()
        }

        kotlinGradle {
            target("**/*.gradle.kts", "*.gradle.kts")
            ktlint().userData(mapOf("indent_size" to "2", "continuation_indent_size" to "2"))
            @Suppress("INACCESSIBLE_TYPE")
            licenseHeader(kotlinLicenseHeader, "import|tasks|apply|plugins|include|buildscript|configurations")
            trimTrailingWhitespace()
            indentWithSpaces()
            endWithNewline()
        }

        kotlin {
            target("**/src/**/*.kt", "buildSrc/**/*.kt")
            ktlint().userData(mapOf("indent_size" to "4", "continuation_indent_size" to "8"))
            @Suppress("INACCESSIBLE_TYPE")
            licenseHeader(kotlinLicenseHeader, "import|package|class|object|@file")
            trimTrailingWhitespace()
            indentWithSpaces()
            endWithNewline()
        }
    }
}

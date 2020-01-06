/*
 * Copyright © 2017-2020 WireGuard LLC.
 * Copyright © 2018-2020 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import dev.msfjarvis.buildsrc.ProjectConfig

android {
    buildTypes {
        named("release") {
            externalNativeBuild {
                cmake {
                    arguments += "-DANDROID_PACKAGE_NAME=${ProjectConfig.packageName}"
                }
            }
        }
        named("debug") {
            externalNativeBuild {
                cmake {
                    arguments += "-DANDROID_PACKAGE_NAME=${ProjectConfig.packageName}.debug"
                }
            }
        }
    }

    externalNativeBuild {
        cmake {
            setPath("tools/CMakeLists.txt")
        }
    }
}

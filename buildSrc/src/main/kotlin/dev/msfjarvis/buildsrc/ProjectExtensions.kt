package dev.msfjarvis.buildsrc

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project

fun Project.configureAndroid(ext: BaseExtension) {
    if (ext is LibraryExtension) {
        ext.apply {
            libraryVariants.all {
                generateBuildConfigProvider.configure { enabled = false }
            }
        }
    }
    ext.apply {
        compileSdkVersion(ProjectConfig.compileSdk)
        buildToolsVersion = ProjectConfig.buildTools
        defaultConfig {
            minSdkVersion(ProjectConfig.minSdk)
            targetSdkVersion(ProjectConfig.targetSdk)
            versionCode = ProjectConfig.versionCode
            versionName = ProjectConfig.versionName
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
    }
}

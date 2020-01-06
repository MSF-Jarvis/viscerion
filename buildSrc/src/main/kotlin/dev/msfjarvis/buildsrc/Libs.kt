package dev.msfjarvis.buildsrc

object Libs {
    object Plugins {
        const val android = "com.android.tools.build:gradle:3.5.0"
        const val kotlin = "org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.61"
        const val detekt = "io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.3.1"
        const val spotless = "com.diffplug.spotless:spotless-plugin-gradle:3.27.0"
    }

    object Kotlin {
        const val stdlib8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.3.61"
    }

    object AndroidX {
        const val annotation = "androidx.annotation:annotation:1.1.0"
        const val appcompat = "androidx.appcompat:appcompat:1.2.0-alpha01"
        const val biometric = "androidx.biometric:biometric:1.0.1"
        const val constraint_layout = "androidx.constraintlayout:constraintlayout:2.0.0-beta4"
        const val coreKtx = "androidx.core:core-ktx:1.2.0-rc01"
        const val fragmentKtx = "androidx.fragment:fragment-ktx:1.2.0-rc05"
        const val material = "com.google.android.material:material:1.2.0-alpha03"
        const val preference = "androidx.preference:preference:1.1.0"
        const val recyclerview = "androidx.recyclerview:recyclerview:1.0.0"
        const val sliceBuilders = "androidx.slice:slice-builders:1.1.0-alpha01"
        const val sliceCore = "androidx.slice:slice-core:1.1.0-alpha01"
        const val sliceBuildersKtx = "androidx.slice:slice-builders-ktx:1.0.0-alpha07"
        const val workKtx = "androidx.work:work-runtime-ktx:2.3.0-rc01"
    }

    object ThirdParty {
        const val barcode = "com.kroegerama:barcode-kaiteki:1.1.1"
        const val dagger = "com.google.dagger:dagger:2.25.4"
        const val daggerCompiler = "com.google.dagger:dagger-compiler:2.25.4"
        const val leakCanary = "com.squareup.leakcanary:leakcanary-android:2.1"
        const val retrofuture = "net.sourceforge.streamsupport:android-retrofuture:1.7.1"
        const val threetenabp = "com.jakewharton.threetenabp:threetenabp:1.2.1"
        const val timber = "com.jakewharton.timber:timber:4.7.1"
    }

    object Testing {
        const val junit = "junit:junit:4.13"
    }
}

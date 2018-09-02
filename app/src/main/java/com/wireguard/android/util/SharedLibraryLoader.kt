/*
 * Copyright © 2018 Jason A. Donenfeld <Jason@zx2c4.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipFile

object SharedLibraryLoader {
    private val TAG = "WireGuard/" + SharedLibraryLoader::class.java.simpleName

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    fun loadSharedLibrary(context: Context, libName: String) {
        var noAbiException: Throwable
        try {
            System.loadLibrary(libName)
            return
        } catch (e: UnsatisfiedLinkError) {
            Log.d(TAG, "Failed to load library normally, so attempting to extract from apk", e)
            noAbiException = e
        }

        val zipFile: ZipFile
        try {
            zipFile = ZipFile(File(context.applicationInfo.sourceDir), ZipFile.OPEN_READ)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        val mappedLibName = System.mapLibraryName(libName)
        for (abi in Build.SUPPORTED_ABIS) {
            val libZipPath = "lib" + File.separatorChar + abi + File.separatorChar + mappedLibName
            val zipEntry = zipFile.getEntry(libZipPath) ?: continue
            var f: File? = null
            try {
                f = File.createTempFile("lib", ".so", context.cacheDir)
                Log.d(TAG, "Extracting apk:/$libZipPath to ${f!!.absolutePath} and loading")
                FileOutputStream(f).use { out ->
                    zipFile.getInputStream(zipEntry).use { inputStream ->
                        inputStream.copyTo(out)
                    }
                }
                System.load(f.absolutePath)
                return
            } catch (e: Exception) {
                Log.d(TAG, "Failed to load library apk:/$libZipPath", e)
                noAbiException = e
            } finally {
                f?.delete()
            }
        }
        if (noAbiException is RuntimeException)
            throw noAbiException
        throw RuntimeException(noAbiException)
    }
}

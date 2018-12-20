/*
 * Copyright © 2017-2018 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.util

import android.content.Context
import android.system.OsConstants
import com.wireguard.android.Application
import com.wireguard.android.BuildConfig
import com.wireguard.android.util.RootShell.NoRootException
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Helper to install WireGuard tools to the system partition.
 */

class ToolsInstaller(context: Context) {

    private val localBinaryDir: File = File(context.cacheDir, "bin")
    private val nativeLibraryDir: File = File(context.applicationInfo.nativeLibraryDir)
    private var areToolsAvailable: Boolean? = null
    private var installAsMagiskModule: Boolean? = null

    init {
        Timber.tag(TAG)
    }

    @Throws(NoRootException::class)
    fun areInstalled(): Int {
        if (INSTALL_DIR == null)
            return ERROR
        val script = StringBuilder()
        for (names in EXECUTABLES) {
            script.append(
                String.format(
                    "cmp -s '%s' '%s' && ",
                    File(nativeLibraryDir, names[0]),
                    File(INSTALL_DIR, names[1])
                )
            )
        }
        script.append("exit ").append(OsConstants.EALREADY).append(';')
        return try {
            val ret = Application.rootShell.run(null, script.toString())
            if (ret == OsConstants.EALREADY) {
                if (willInstallAsMagiskModule()) YES or MAGISK else YES or SYSTEM
            } else {
                if (willInstallAsMagiskModule()) NO or MAGISK else NO or SYSTEM
            }
        } catch (ignored: IOException) {
            ERROR
        }
    }

    @Throws(FileNotFoundException::class, NoRootException::class)
    @Synchronized
    fun ensureToolsAvailable() {
        if (areToolsAvailable == null) {
            val ret = symlink()
            areToolsAvailable = when (ret) {
                OsConstants.EALREADY -> {
                    Timber.d("Tools were already symlinked into our private binary dir")
                    true
                }
                OsConstants.EXIT_SUCCESS -> {
                    Timber.d("Tools are now symlinked into our private binary dir")
                    true
                }
                else -> {
                    Timber.e("For some reason, wg and wg-quick are not available at all")
                    false
                }
            }
        }
        if (areToolsAvailable == false)
            throw FileNotFoundException("Required tools unavailable")
    }

    @Synchronized
    private fun willInstallAsMagiskModule(): Boolean {
        val magiskDirectory = getMagiskDirectory()
        if (installAsMagiskModule == null) {
            installAsMagiskModule = try {
                Application.rootShell.run(
                    null,
                    "[ -d $magiskDirectory/mirror -a -d $magiskDirectory/img -a ! -f /cache/.disable_magisk ]"
                ) == OsConstants.EXIT_SUCCESS
            } catch (ignored: Exception) {
                false
            }
        }
        return installAsMagiskModule == true
    }

    @Throws(NoRootException::class)
    private fun installSystem(): Int {
        if (INSTALL_DIR == null)
            return OsConstants.ENOENT
        val script = StringBuilder("set -ex; ")
        script.append("trap 'mount -o ro,remount /system' EXIT; mount -o rw,remount /system; ")
        for (names in EXECUTABLES) {
            val destination = File(INSTALL_DIR, names[1])
            script.append(
                String.format(
                    "cp '%s' '%s'; chmod 755 '%s'; restorecon '%s' || true; ",
                    File(nativeLibraryDir, names[0]), destination, destination, destination
                )
            )
        }
        return try {
            if (Application.rootShell.run(null, script.toString()) == 0) YES or SYSTEM else ERROR
        } catch (ignored: IOException) {
            ERROR
        }
    }

    @Throws(NoRootException::class)
    private fun installMagisk(): Int {
        val script = StringBuilder("set -ex; ")
        val magiskDirectory = "${getMagiskDirectory()}/img/wireguard"

        script.append("trap 'rm -rf $magiskDirectory' INT TERM EXIT; ")
        script.append(
            String.format(
                "rm -rf $magiskDirectory/; mkdir -p $magiskDirectory/%s; ",
                INSTALL_DIR
            )
        )
        script.append(
            String.format(
                "printf 'name=WireGuard Command Line Tools\nversion=%s\nversionCode=%s\nauthor=zx2c4\ndescription=Command line tools for WireGuard\nminMagisk=1500\n' > $magiskDirectory/module.prop; ",
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE
            )
        )
        script.append("touch $magiskDirectory/auto_mount; ")
        for (names in EXECUTABLES) {
            val destination = File("$magiskDirectory/$INSTALL_DIR", names[1])
            script.append(
                String.format(
                    "cp '%s' '%s'; chmod 755 '%s'; restorecon '%s' || true; ",
                    File(nativeLibraryDir, names[0]), destination, destination, destination
                )
            )
        }
        script.append("trap - INT TERM EXIT;")

        return try {
            if (Application.rootShell.run(null, script.toString()) == 0) YES or MAGISK else ERROR
        } catch (ignored: IOException) {
            ERROR
        }
    }

    @Throws(NoRootException::class)
    fun install(): Int {
        return if (willInstallAsMagiskModule()) installMagisk() else installSystem()
    }

    @Throws(NoRootException::class)
    fun symlink(): Int {
        val script = StringBuilder("set -x; ")
        for (names in EXECUTABLES) {
            script.append(
                String.format(
                    "test '%s' -ef '%s' && ",
                    File(nativeLibraryDir, names[0]),
                    File(localBinaryDir, names[1])
                )
            )
        }
        script.append("exit ").append(OsConstants.EALREADY).append("; set -e; ")

        for (names in EXECUTABLES) {
            script.append(
                String.format(
                    "ln -fns '%s' '%s'; ",
                    File(nativeLibraryDir, names[0]),
                    File(localBinaryDir, names[1])
                )
            )
        }
        script.append("exit ").append(OsConstants.EXIT_SUCCESS).append(';')

        return try {
            Application.rootShell.run(null, script.toString())
        } catch (ignored: IOException) {
            OsConstants.EXIT_FAILURE
        }
    }

    companion object {
        const val ERROR = 0x0
        const val YES = 0x1
        const val NO = 0x2
        const val MAGISK = 0x4
        const val SYSTEM = 0x8

        private val EXECUTABLES = arrayOf(arrayOf("libwg.so", "wg"), arrayOf("libwg-quick.so", "wg-quick"))
        private val INSTALL_DIRS = arrayOf(File("/system/xbin"), File("/system/bin"))
        private val INSTALL_DIR = installDir
        private val TAG = "WireGuard/" + ToolsInstaller::class.java.simpleName

        private val installDir: File?
            get() {
                val path = System.getenv("PATH") ?: return INSTALL_DIRS[0]
                val paths = path.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toList()
                for (dir in INSTALL_DIRS) {
                    if (paths.contains(dir.path) && dir.isDirectory)
                        return dir
                }
                return null
            }
        private fun getMagiskDirectory(): String {
                val output = ArrayList<String>()
                Application.rootShell.run(output, "su --version | cut -d ':' -f 1")
                val magiskVer = output[0]
                return when {
                    magiskVer.startsWith("18.") -> "/sbin/.magisk"
                    else -> "/sbin/.core"
                }
            }
    }
}

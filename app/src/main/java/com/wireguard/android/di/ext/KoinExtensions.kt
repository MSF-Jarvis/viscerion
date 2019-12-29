/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.di.ext

import android.content.Context
import com.wireguard.android.util.ApplicationPreferences
import org.koin.core.KoinComponent
import org.koin.core.get

fun KoinComponent.getContext() = get<Context>()
fun KoinComponent.getPrefs() = get<ApplicationPreferences>()

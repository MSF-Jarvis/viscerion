/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.di

import androidx.fragment.app.FragmentActivity

interface InjectorProvider {
    val component: AppComponent
}

val FragmentActivity.injector get() = (application as InjectorProvider).component

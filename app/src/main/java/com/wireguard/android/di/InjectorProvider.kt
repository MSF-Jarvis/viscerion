/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.di

import android.content.ContentProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.preference.Preference

interface InjectorProvider {
    val component: AppComponent
}

val FragmentActivity.injector get() = (application as InjectorProvider).component
val Fragment.injector get() = (requireContext().applicationContext as InjectorProvider).component
val Preference.injector get() = (context.applicationContext as InjectorProvider).component
val ContentProvider.injector get() = (context?.applicationContext as InjectorProvider).component

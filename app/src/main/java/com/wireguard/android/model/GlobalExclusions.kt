package com.wireguard.android.model

import com.wireguard.android.Application

class GlobalExclusions {
    init {
        exclusions = Application.getSharedPreferences().getString("global_exclusions", "") as String
    }
    companion object {
        var exclusions: String = ""
        set(value) {
            Application.getSharedPreferences().edit().putString("global_exclusions", value).apply()
            field = value
        }
    }
}
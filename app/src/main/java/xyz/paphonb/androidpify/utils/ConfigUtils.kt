/*
 * Copyright (C) 2018 paphonb@xda
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.paphonb.androidpify.utils

import android.content.SharedPreferences
import de.robv.android.xposed.XSharedPreferences
import xyz.paphonb.androidpify.MainHook
import java.io.File

object ConfigUtils {

    val prefs by lazy { XSharedPreferences(File("/data/user_de/0/${MainHook.PACKAGE_OWN}/shared_prefs/${MainHook.PACKAGE_OWN}_preferences.xml")).apply { makeWorldReadable() } }
    val notifications get() = notificationsInternal!!
    val settings get() = settingsInternal!!
    val misc get() = miscInternal!!

    fun reload() {
        prefs.reload()
        loadConfig()
    }

    private fun loadConfig() {
        notificationsInternal = NotificationConfig(prefs)
        settingsInternal = SettingsConfig(prefs)
        miscInternal = MiscConfig(prefs)
    }

    private var notificationsInternal: NotificationConfig? = null
    private var settingsInternal: SettingsConfig? = null
    private var miscInternal: MiscConfig? = null

    class NotificationConfig(prefs: SharedPreferences) {
        val enableLeftClock = prefs.getBoolean(PreferencesList.enableLeftClock, true)
        val forceDarkTheme = prefs.getBoolean(PreferencesList.forceDarkTheme, false)
        val changePullDown = prefs.getBoolean(PreferencesList.changePullDown, true)
        val circleTileBackground = prefs.getBoolean(PreferencesList.circleTileBackground, true)
        val qsVerticalScroll = prefs.getBoolean(PreferencesList.qsVerticalScroll, false)
        val swapQsAndBrightness = prefs.getBoolean(PreferencesList.swapQsAndBrightness, false)
        val statusBarHeight = prefs.getInt(PreferencesList.statusBarHeight, 24)
    }

    class SettingsConfig(prefs: SharedPreferences) {
        val changeSettingsTheme = prefs.getBoolean(PreferencesList.changeSettingsTheme, true)
    }

    class MiscConfig(prefs: SharedPreferences) {
        val newTransitions = prefs.getBoolean(PreferencesList.newTransitions, true)
        val googleSans = prefs.getBoolean(PreferencesList.googleSans, true)
        val proxyOverview = prefs.getBoolean(PreferencesList.proxyOverview, true)
        val proxyOverviewPackage = prefs.getString(PreferencesList.proxyOverviewPackage, "com.google.android.apps.nexuslauncher")!!
    }
}
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

object PreferencesList {

    val prefLevel by lazy {
        HashMap<String, Int>().apply {
            put(enableLeftClock, LEVEL_SYSTEM_UI)
            put(forceDarkTheme, LEVEL_SYSTEM_UI)
            put(changePullDown, LEVEL_SYSTEM_UI)
            put(circleTileBackground, LEVEL_SYSTEM_UI)
            put(qsVerticalScroll, LEVEL_SYSTEM_UI)
            put(statusBarHeight, LEVEL_ANDROID)
        }
    }

    const val enableLeftClock = "enable_left_clock"
    const val forceDarkTheme = "force_dark_theme"
    const val changePullDown = "change_pull_down"
    const val circleTileBackground = "circle_tile_background"
    const val qsVerticalScroll = "qs_vertical_scroll"
    const val statusBarHeight = "status_bar_height"

    const val changeSettingsTheme = "change_settings_theme"

    const val LEVEL_SYSTEM_UI = 1 shl 0
    const val LEVEL_ANDROID = 1 shl 1
}

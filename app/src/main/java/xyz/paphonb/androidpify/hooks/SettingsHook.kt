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

package xyz.paphonb.androidpify.hooks

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.graphics.drawable.LayerDrawable
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_LayoutInflated
import de.robv.android.xposed.callbacks.XC_LoadPackage
import xyz.paphonb.androidpify.MainHook
import xyz.paphonb.androidpify.R
import xyz.paphonb.androidpify.utils.ConfigUtils
import xyz.paphonb.androidpify.utils.ResourceUtils
import xyz.paphonb.androidpify.utils.getColorAttr

object SettingsHook : IXposedHookLoadPackage, IXposedHookInitPackageResources {

    val iconMap by lazy { HashMap<String, Int>().apply {
        put("com.android.settings:drawable/ic_settings_network", R.drawable.ic_homepage_network)
        put("com.android.settings:drawable/ic_devices_other", R.drawable.ic_homepage_devices_other)
        put("com.android.settings:drawable/ic_apps", R.drawable.ic_homepage_apps)
        put("com.android.settings:drawable/ic_settings_battery", R.drawable.ic_homepage_battery)
        put("com.android.settings:drawable/ic_settings_display", R.drawable.ic_homepage_display)
        put("com.android.settings:drawable/ic_settings_sound", R.drawable.ic_homepage_sound)
        put("com.android.settings:drawable/ic_settings_storage", R.drawable.ic_homepage_storage)
        put("com.android.settings:drawable/ic_settings_security", R.drawable.ic_homepage_security)
        put("com.android.settings:drawable/ic_settings_accounts", R.drawable.ic_homepage_accounts)
        put("com.android.settings:drawable/ic_settings_accessibility", R.drawable.ic_homepage_accessibility)
        put("com.android.settings:drawable/ic_settings_about", R.drawable.ic_homepage_about)
    } }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != MainHook.PACKAGE_SETTINGS) return

        if (!ConfigUtils.settings.changeSettingsTheme) return

        XposedHelpers.findAndHookMethod(Icon::class.java, "loadDrawableInner",
                Context::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val icon = param.thisObject as Icon
                val context = param.args[0] as Context

                if (shouldChangeIcon()) {
                    val ownResource = ResourceUtils.getInstance(context)
                    val mType = XposedHelpers.getIntField(icon, "mType")
                    if (mType == 2) {
                        val resPackage = XposedHelpers.callMethod(icon, "getResPackage") as String
                        val resId = XposedHelpers.callMethod(icon, "getResId") as Int

                        val ai = context.packageManager.getApplicationInfo(
                                resPackage, PackageManager.MATCH_UNINSTALLED_PACKAGES)
                        if (ai != null) {
                            val resources = context.packageManager.getResourcesForApplication(ai)
                            val replacement = iconMap[resources.getResourceName(resId)]
                            if (replacement != null) {
                                param.result = ownResource.getDrawable(replacement, context.theme)
                                return
                            }
                        }
                    }
                    val background = ownResource.getDrawable(R.drawable.ic_homepage_generic, context.theme)
                    val drawable = XposedBridge.invokeOriginalMethod(
                            param.method, param.thisObject, arrayOf(context)) as Drawable
                    val size = ownResource.getDimensionPixelSize(R.dimen.dashboard_tile_foreground_image_size)
                    val inset = ownResource.getDimensionPixelSize(R.dimen.dashboard_tile_foreground_image_inset)
                    drawable.setBounds(0, 0, size, size)
                    drawable.setTint(Color.WHITE)
                    val layerDrawable = LayerDrawable(arrayOf(background, drawable))
                    val layerState = XposedHelpers.getObjectField(layerDrawable, "mLayerState")
                    val children = XposedHelpers.getObjectField(layerState, "mChildren") as Array<*>
                    children[1].let { child ->
                        XposedHelpers.setIntField(child, "mInsetL", inset)
                        XposedHelpers.setIntField(child, "mInsetT", inset)
                        XposedHelpers.setIntField(child, "mWidth", size)
                        XposedHelpers.setIntField(child, "mHeight", size)
                    }
                    param.result = layerDrawable
                }
            }
        })
    }

    fun shouldChangeIcon(): Boolean {
        Throwable().stackTrace.forEach { element ->
            if (element.className.startsWith("android.s")) return false
            if (element.methodName == "updateConditionIcons") return false
            if (element.methodName == "onBindSuggestionConditionHeader") return false
            if (element.className.endsWith("DashboardAdapter")) return true
            if (element.className.endsWith("SuggestionAdapter")) return false
        }
        return false
    }

    override fun handleInitPackageResources(resparam: XC_InitPackageResources.InitPackageResourcesParam) {
        if (resparam.packageName != MainHook.PACKAGE_SETTINGS) return

        if (!ConfigUtils.settings.changeSettingsTheme) return

        if (MainHook.ATLEAST_O_MR1) {
            val searchBarLayoutHook = object : XC_LayoutInflated() {
                override fun handleLayoutInflated(liparam: LayoutInflatedParam) {
                    val layout = liparam.view as ViewGroup
                    val context = layout.context
                    val searchBarId = context.resources.getIdentifier(
                            "search_bar", "id", MainHook.PACKAGE_SETTINGS)
                    val searchBarHeightId = context.resources.getIdentifier(
                            "search_bar_height", "dimen", MainHook.PACKAGE_SETTINGS)
                    val searchBarHeight = context.resources.getDimensionPixelSize(searchBarHeightId)
                    XposedHelpers.callMethod(layout.findViewById(searchBarId),
                            "setRadius", searchBarHeight / 2)

                    setBackgroundColor(layout)

                    val searchActionBarId = context.resources.getIdentifier(
                            "search_action_bar", "id", MainHook.PACKAGE_SETTINGS)
                    val searchActionBar = layout.findViewById<ViewGroup>(searchActionBarId)
                            ?: return
                    val ownContext = ResourceUtils.getInstance(context).context
                    LayoutInflater.from(ContextThemeWrapper(ownContext, context.theme)).inflate(
                            R.layout.search_bar_text, searchActionBar, true)
                    val title = XposedHelpers.callMethod(searchActionBar, "getTitle") as CharSequence
                    searchActionBar.findViewById<TextView>(R.id.search_action_bar_title).text = title
                    XposedHelpers.callMethod(searchActionBar, "setTitle", "" as CharSequence)
                }
            }
            resparam.res.hookLayout(MainHook.PACKAGE_SETTINGS, "layout", "settings_main_dashboard", searchBarLayoutHook)
            resparam.res.hookLayout(MainHook.PACKAGE_SETTINGS, "layout", "search_panel", searchBarLayoutHook)
            resparam.res.setReplacement(MainHook.PACKAGE_SETTINGS, "dimen", "search_bar_margin",
                    MainHook.modRes.fwd(R.dimen.search_bar_margin))
            resparam.res.setReplacement(MainHook.PACKAGE_SETTINGS, "dimen", "search_bar_negative_margin",
                    MainHook.modRes.fwd(R.dimen.search_bar_negative_margin))

            resparam.res.hookLayout(MainHook.PACKAGE_SETTINGS, "layout", "suggestion_condition_container",
                    object : XC_LayoutInflated() {
                        override fun handleLayoutInflated(liparam: LayoutInflatedParam) {
                            val context = liparam.view.context
                            val ownResources = ResourceUtils.getInstance(context)
                            val padding = ownResources.getDimensionPixelSize(R.dimen.settings_suggestion_padding)
                            val radius = ownResources.getDimensionPixelSize(R.dimen.settings_card_radius)
                            liparam.view.setPadding(padding, liparam.view.paddingTop,
                                    padding, liparam.view.paddingBottom)
                            val card = (liparam.view as ViewGroup).getChildAt(0)
                            XposedHelpers.callMethod(card, "setRadius", radius.toFloat())
                        }
                    })

            resparam.res.setReplacement(MainHook.PACKAGE_SETTINGS, "bool", "config_tintSettingIcon", false)
        } else {
            resparam.res.hookLayout(MainHook.PACKAGE_SETTINGS, "layout", "settings_main_dashboard",
                    object : XC_LayoutInflated() {
                        override fun handleLayoutInflated(liparam: LayoutInflatedParam) {
                            setBackgroundColor(liparam.view as ViewGroup)
                        }
                    })
        }

        resparam.res.hookLayout(MainHook.PACKAGE_SETTINGS, "layout", "settings_main_prefs",
                object : XC_LayoutInflated() {
                    override fun handleLayoutInflated(liparam: LayoutInflatedParam) {
                        setBackgroundColor(liparam.view as ViewGroup)
                    }
                })

        resparam.res.hookLayout(MainHook.PACKAGE_SETTINGS, "layout", "dashboard_tile",
                object : XC_LayoutInflated() {
                    override fun handleLayoutInflated(liparam: LayoutInflatedParam) {
                        val icon = liparam.view.findViewById<View>(android.R.id.icon)
                        (icon.layoutParams as LinearLayout.LayoutParams).apply {
                            val size = MainHook.modRes.getDimensionPixelSize(R.dimen.dashboard_tile_image_size)
                            width = size
                            height = size

                            val margin = MainHook.modRes.getDimensionPixelSize(R.dimen.dashboard_tile_image_margin)
                            leftMargin = margin
                            rightMargin = margin
                        }
                    }
                })
    }

    fun setBackgroundColor(layout: ViewGroup) {
        val context = layout.context

        val colorBackgroundAttr = context.getColorAttr(android.R.attr.colorBackground)
        val colorBackground = when (colorBackgroundAttr) {
            0xff303030.toInt() -> Color.BLACK
            0xfffafafa.toInt() -> Color.WHITE
            else -> colorBackgroundAttr
        }

        val mainContentId = context.resources.getIdentifier(
                "main_content", "id", MainHook.PACKAGE_SETTINGS)
        val mainContent = layout.findViewById<View>(mainContentId)
        mainContent.background = ColorDrawable(colorBackground)
    }
}
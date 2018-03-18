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

package xyz.paphonb.androidpify

import android.content.res.XModuleResources
import android.os.Build
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_LoadPackage
import xyz.paphonb.androidpify.hooks.*
import xyz.paphonb.androidpify.ui.SettingsActivity
import xyz.paphonb.androidpify.utils.ConfigUtils



object MainHook : IXposedHookZygoteInit, IXposedHookLoadPackage, IXposedHookInitPackageResources {

    private var MODULE_PATH = ""
    private lateinit var modResInternal: XModuleResources

    val modRes get() = modResInternal

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        MODULE_PATH = startupParam.modulePath
        ConfigUtils.reload()
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        SystemUIHook.handleLoadPackage(lpparam)
        TransitionsHook.handleLoadPackage(lpparam)
        LeftClockHook.handleLoadPackage(lpparam)
        CustomizationsHook.handleLoadPackage(lpparam)
        NotificationStackHook.handleLoadPackage(lpparam)
        QuickSettingsHook.handleLoadPackage(lpparam)
        SettingsHook.handleLoadPackage(lpparam)

        if (lpparam.packageName == PACKAGE_OWN) {
            XposedHelpers.findAndHookMethod(SETTINGS_OWN, lpparam.classLoader,
                    "isActivated", XC_MethodReplacement.returnConstant(true))
            XposedHelpers.findAndHookMethod(SETTINGS_OWN, lpparam.classLoader,
                    "isPrefsFileReadable", XC_MethodReplacement.returnConstant(
                    ConfigUtils.prefs.getBoolean("can_read_prefs", false)))
        }
    }

    override fun handleInitPackageResources(resparam: XC_InitPackageResources.InitPackageResourcesParam) {
        modResInternal = XModuleResources.createInstance(MODULE_PATH, resparam.res)
        NotificationStackHook.handleInitPackageResources(resparam)
        QuickSettingsHook.handleInitPackageResources(resparam)
        SettingsHook.handleInitPackageResources(resparam)
    }

    val ATLEAST_O_MR1 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1

    const val PACKAGE_ANDROID = "android"
    const val PACKAGE_SYSTEMUI = "com.android.systemui"
    const val PACKAGE_SETTINGS = "com.android.settings"

    const val PACKAGE_OWN = "xyz.paphonb.androidpify"
    private val SETTINGS_OWN = SettingsActivity.BaseFragment::class.java.name

    private const val LOG_FORMAT = "[Android P-ify] %1\$s %2\$s: %3\$s"
    private const val DEBUG = true

    fun logE(tag: String, msg: String, t: Throwable? = null) {
        XposedBridge.log(String.format(LOG_FORMAT, "[ERROR]", tag, msg))
        if (t != null)
            XposedBridge.log(t)
    }

    fun logW(tag: String, msg: String) {
        XposedBridge.log(String.format(LOG_FORMAT, "[WARNING]", tag, msg))
    }

    fun logI(tag: String, msg: String) {
        XposedBridge.log(String.format(LOG_FORMAT, "[INFO]", tag, msg))
    }

    fun logD(tag: String, msg: String) {
        if (DEBUG) XposedBridge.log(String.format(LOG_FORMAT, "[DEBUG]", tag, msg))
    }
}

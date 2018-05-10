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

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.IBinder
import android.os.Process
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import xyz.paphonb.androidpify.MainHook
import xyz.paphonb.androidpify.utils.SystemProp

object SystemUIHook : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != MainHook.PACKAGE_SYSTEMUI) return

        XposedHelpers.findAndHookMethod("com.android.systemui.SystemUIApplication",
                lpparam.classLoader, "onCreate", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val app = param.thisObject as Application
                val handler = Handler(app.mainLooper)

                RecentsHook.onCreate(app)

                app.registerReceiver(object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        handler.postDelayed({ Process.killProcess(Process.myPid()) }, 100)
                    }
                }, IntentFilter(ACTION_KILL_SYSTEMUI))

                app.registerReceiver(object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        // from GravityBox
                        try {
                            val classSm = XposedHelpers.findClass("android.os.ServiceManager", null)
                            val classIpm = XposedHelpers.findClass("android.os.IPowerManager.Stub", null)
                            val b = XposedHelpers.callStaticMethod(
                                    classSm, "getService", Context.POWER_SERVICE) as IBinder
                            val ipm = XposedHelpers.callStaticMethod(classIpm, "asInterface", b)
                            XposedHelpers.callMethod(ipm, "crash", "Hot reboot")
                        } catch (t: Throwable) {
                            try {
                                SystemProp.set("ctl.restart", "surfaceflinger")
                                SystemProp.set("ctl.restart", "zygote")
                            } catch (t2: Throwable) {

                            }

                        }
                    }
                }, IntentFilter(ACTION_SOFT_REBOOT))
            }
        })
    }

    const val ACTION_KILL_SYSTEMUI = "xyz.paphonb.androidpify.action.ACTION_KILL_SYSTEMUI"
    const val ACTION_SOFT_REBOOT = "xyz.paphonb.androidpify.action.ACTION_SOFT_REBOOT"
}
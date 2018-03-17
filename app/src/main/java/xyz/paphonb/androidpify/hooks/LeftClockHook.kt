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

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import xyz.paphonb.androidpify.MainHook
import xyz.paphonb.androidpify.utils.ConfigUtils

object LeftClockHook : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != MainHook.PACKAGE_SYSTEMUI) return

        if (!ConfigUtils.notifications.enableLeftClock) return

        val classPhoneStatusBarView = XposedHelpers.findClass(
                "com.android.systemui.statusbar.phone.PhoneStatusBarView", lpparam.classLoader)
        XposedHelpers.findAndHookMethod(classPhoneStatusBarView, "onFinishInflate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val phoneStatusBarView = param.thisObject as ViewGroup
                        val context = phoneStatusBarView.context
                        val clockId = context.resources.getIdentifier("clock", "id", MainHook.PACKAGE_SYSTEMUI)
                        val clock = (param.thisObject as View).findViewById<View>(clockId)

                        val statusBarContentsId = context.resources.getIdentifier("status_bar_contents", "id", MainHook.PACKAGE_SYSTEMUI)
                        val statusBarContents = phoneStatusBarView.findViewById<ViewGroup>(statusBarContentsId)

                        clock.setPadding(clock.paddingRight, clock.paddingTop,
                                clock.paddingLeft, clock.paddingBottom)
//                        val padding = clock.paddingEnd
//                        phoneStatusBarView.setPadding(padding, 0, padding, 0)
                        (clock.parent as ViewGroup).removeView(clock)
                        statusBarContents.addView(clock, 0)

//                        val signalClusterId = context.resources.getIdentifier("signal_cluster", "id", MainHook.PACKAGE_SYSTEMUI)
//                        val signalCluster = phoneStatusBarView.findViewById<ViewGroup>(signalClusterId)
//
//                        val wifiInOut = signalCluster.getChildAt(2)
//                        wifiInOut.visibility = View.GONE
//
//                        val mobildSignalId = context.resources.getIdentifier("mobile_signal_group", "id", MainHook.PACKAGE_SYSTEMUI)
//                        val mobileSignal = signalCluster.findViewById<View>(mobildSignalId)
//
//                        signalCluster.removeView(mobileSignal)
//                        signalCluster.addView(mobileSignal, 0)
//
//                        val wifiSpaceId = context.resources.getIdentifier("wifi_signal_spacer", "id", MainHook.PACKAGE_SYSTEMUI)
//                        val wifiSpace = signalCluster.findViewById<View>(wifiSpaceId)
//
//                        signalCluster.addView(View(context).apply {
//                            layoutParams = ViewGroup.LayoutParams(
//                                    wifiSpace.layoutParams.width, wifiSpace.layoutParams.height)
//                        }, 3)
                    }
                })

        val classCollapsedStatusBarFragment = XposedHelpers.findClass(
                "com.android.systemui.statusbar.phone.CollapsedStatusBarFragment", lpparam.classLoader)
        XposedHelpers.findAndHookMethod(classCollapsedStatusBarFragment, "onViewCreated",
                View::class.java, Bundle::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val statusBarView = param.args[0] as ViewGroup
                val context = statusBarView.context
                val clockId = context.resources.getIdentifier("clock", "id", MainHook.PACKAGE_SYSTEMUI)
                val clock = statusBarView.findViewById<View>(clockId)
                XposedHelpers.setAdditionalInstanceField(param.thisObject, "clock", clock)
            }
        })
        XposedHelpers.findAndHookMethod(classCollapsedStatusBarFragment, "showSystemIconArea",
                Boolean::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    val clock = XposedHelpers.getAdditionalInstanceField(param.thisObject, "clock")
                    XposedHelpers.callMethod(param.thisObject, "animateShow", clock, param.args[0])
                } catch (ignored: Throwable) {

                }
            }
        })
        XposedHelpers.findAndHookMethod(classCollapsedStatusBarFragment, "hideSystemIconArea",
                Boolean::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                try {
                    val clock = XposedHelpers.getAdditionalInstanceField(param.thisObject, "clock")
                    XposedHelpers.callMethod(param.thisObject, "animateHide", clock, param.args[0])
                } catch (ignored: Throwable) {

                }
            }
        })
    }
}
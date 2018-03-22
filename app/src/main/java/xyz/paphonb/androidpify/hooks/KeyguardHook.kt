package xyz.paphonb.androidpify.hooks

import android.widget.TextView
import de.robv.android.xposed.IXposedHookInitPackageResources
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_LayoutInflated
import xyz.paphonb.androidpify.MainHook
import xyz.paphonb.androidpify.utils.setGoogleSans

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

object KeyguardHook : IXposedHookInitPackageResources {

    override fun handleInitPackageResources(resparam: XC_InitPackageResources.InitPackageResourcesParam) {
        if (resparam.packageName != MainHook.PACKAGE_SYSTEMUI) return

        resparam.res.hookLayout(MainHook.PACKAGE_SYSTEMUI, "layout", "keyguard_status_view",
                object : XC_LayoutInflated() {
                    override fun handleLayoutInflated(liparam: LayoutInflatedParam) {
                        val dateId = liparam.view.context.resources
                                .getIdentifier("date_view", "id", MainHook.PACKAGE_SYSTEMUI)
                        liparam.view.findViewById<TextView>(dateId).setGoogleSans()

                        val alarmId = liparam.view.context.resources
                                .getIdentifier("alarm_status", "id", MainHook.PACKAGE_SYSTEMUI)
                        liparam.view.findViewById<TextView>(alarmId).setGoogleSans()
                    }
                })
    }
}
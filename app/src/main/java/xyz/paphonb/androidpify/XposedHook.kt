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

import de.robv.android.xposed.IXposedHookInitPackageResources
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_LoadPackage
import xyz.paphonb.androidpify.utils.ConfigUtils

class XposedHook : IXposedHookZygoteInit, IXposedHookLoadPackage, IXposedHookInitPackageResources {

    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        MainHook.initZygote(startupParam)
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        ConfigUtils.reload()
        MainHook.handleLoadPackage(lpparam)
    }

    override fun handleInitPackageResources(resparam: XC_InitPackageResources.InitPackageResourcesParam) {
        MainHook.handleInitPackageResources(resparam)
    }
}
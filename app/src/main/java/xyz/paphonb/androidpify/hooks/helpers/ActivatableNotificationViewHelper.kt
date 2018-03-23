package xyz.paphonb.androidpify.hooks.helpers

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers

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

class ActivatableNotificationViewHelper(classLoader: ClassLoader, child: ViewGroup)
    : ExpandableOutlineViewHelper(classLoader, child) {

    private val mBackgroundDimmed by AnyField<View>("mBackgroundDimmed")
    private val mBackgroundNormal by AnyField<View>("mBackgroundNormal")

    override fun applyRoundness() {
        super.applyRoundness()
        applyBackgroundRoundness(getCurrentBackgroundRadiusTop(), getCurrentBackgroundRadiusBottom())
    }

    private fun applyBackgroundRoundness(topRadius: Float, bottomRadius: Float) {
        NotificationBackgroundViewHelper.get(classLoader, mBackgroundDimmed).setRoundness(topRadius, bottomRadius)
        NotificationBackgroundViewHelper.get(classLoader, mBackgroundNormal).setRoundness(topRadius, bottomRadius)
    }

    override fun setBackgroundTop(backgroundTop: Int) {
        NotificationBackgroundViewHelper.get(classLoader, mBackgroundDimmed).setBackgroundTop(backgroundTop)
        NotificationBackgroundViewHelper.get(classLoader, mBackgroundNormal).setBackgroundTop(backgroundTop)
    }

    companion object {

        private var manager: Manager? = null


        fun hook(classLoader: ClassLoader) {
            getManager(classLoader)
        }

        fun getManager(classLoader: ClassLoader): Manager {
            if (manager == null) {
                manager = Manager(classLoader)
            }
            return manager!!
        }
    }

    class Manager(private val classLoader: ClassLoader) {

        private val TAG = "Helper"
        private val classActivatableNotificationView = XposedHelpers.findClass("com.android.systemui.statusbar.ActivatableNotificationView", classLoader)

        init {
            XposedHelpers.findAndHookConstructor(classActivatableNotificationView,
                    Context::class.java, AttributeSet::class.java, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    get(param.thisObject as ViewGroup)
                }
            })
        }

        fun get(child: ViewGroup): ActivatableNotificationViewHelper {
            var helper = XposedHelpers.getAdditionalInstanceField(child, TAG) as ActivatableNotificationViewHelper?
            if (helper == null) {
                helper = ActivatableNotificationViewHelper(classLoader, child)
                XposedHelpers.setAdditionalInstanceField(child, TAG, helper)
            }
            return helper
        }
    }
}
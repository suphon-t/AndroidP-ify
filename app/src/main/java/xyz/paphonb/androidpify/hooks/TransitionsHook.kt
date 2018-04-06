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

import android.annotation.SuppressLint
import android.content.Context
import android.view.animation.*
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import xyz.paphonb.androidpify.ClipRectAnimation
import xyz.paphonb.androidpify.MainHook
import xyz.paphonb.androidpify.PathParser
import xyz.paphonb.androidpify.utils.ConfigUtils

object TransitionsHook : IXposedHookLoadPackage {

    @SuppressLint("PrivateApi")
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != MainHook.PACKAGE_ANDROID) return
        if (!ConfigUtils.misc.newTransitions) return

        XposedHelpers.findAndHookMethod(
                AnimationUtils::class.java, "loadAnimation",
                Context::class.java, Int::class.java, object : XC_MethodHook() {

            override fun beforeHookedMethod(param: MethodHookParam) {
                val context = param.args[0] as Context
                val anim = param.args[1] as Int
                val name = context.resources.getResourceEntryName(anim)
                getOverrideAnimation(name)?.let { param.result = it }
            }
        })
    }

    fun getOverrideAnimation(name: String): Animation? {
        return when (name) {
            "activity_open_enter" -> getActivityOpenEnterAnim()
            "activity_open_exit" -> getActivityOpenExitAnim()
            "activity_close_enter" -> getActivityCloseEnterAnim()
            "activity_close_exit" -> getActivityCloseExitAnim()
            "task_open_enter" -> getTaskOpenEnterAnim()
            "task_open_exit" -> getTaskOpenExitAnim()
            "task_close_enter" -> getTaskCloseEnterAnim()
            "task_close_exit" -> getTaskCloseExitAnim()
            else -> null
        }
    }

    private fun getActivityOpenEnterAnim(): Animation {
        val anim = AnimationSet(false)
        anim.zAdjustment = Animation.ZORDER_TOP
        anim.addAnimation(TranslateAnimation(0f, 0f, 0.04100001f, 0f).apply {
            duration = 425
            interpolator = fastOutSlowIn()
            setRelativeToSelf()
        })
        anim.addAnimation(ClipRectAnimation(
                0f, 0.959f, 1f, 1f,
                0f, 0f, 1f, 1f
        ).apply {
            duration = 425
            interpolator = fastOutExtraSlowIn()
        })
        return anim
    }

    private fun getActivityOpenExitAnim(): Animation {
        val anim = AnimationSet(false)
        anim.addAnimation(TranslateAnimation(0f, 0f, 0f, -0.019999981f).apply {
            duration = 425
            interpolator = fastOutSlowIn()
            setRelativeToSelf()
        })
        anim.addAnimation(AlphaAnimation(1.0f, 0.9f).apply {
            duration = 117
            interpolator = LinearInterpolator()
        })
        return anim
    }

    private fun getActivityCloseEnterAnim(): Animation {
        val anim = AnimationSet(false)
        anim.addAnimation(TranslateAnimation(0f, 0f, -0.019999981f, 0f).apply {
            duration = 425
            interpolator = fastOutSlowIn()
            setRelativeToSelf()
        })
        anim.addAnimation(AlphaAnimation(0.9f, 1.0f).apply {
            duration = 425
            startOffset = 0
            interpolator = activityCloseDim()
        })
        return anim
    }

    private fun getActivityCloseExitAnim(): Animation {
        val anim = AnimationSet(false)
        anim.addAnimation(TranslateAnimation(0f, 0f, 0f, 0.04100001f).apply {
            duration = 425
            interpolator = fastOutSlowIn()
            setRelativeToSelf()
        })
        anim.addAnimation(ClipRectAnimation(
                0f, 0f, 1f, 1f,
                0f, 0.959f, 1f, 1f
        ).apply {
            duration = 425
            interpolator = fastOutExtraSlowIn()
        })
        return anim
    }

    private fun getTaskOpenEnterAnim(): Animation {
        val anim = AnimationSet(false)
        anim.addAnimation(TranslateAnimation(-1.0499878f, 0f, 0f, 0f).apply {
            duration = 383
            startOffset = 50
            interpolator = aggressiveEase()
            setFillEnabled()
            setRelativeToSelf()
        })
        anim.addAnimation(ScaleAnimation(1.0526f, 1.0f, 1.0526f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply {
            duration = 283
            interpolator = fastOutSlowIn()
            setFillEnabled()
        })
        anim.addAnimation(ScaleAnimation(0.95f, 1.0f, 0.95f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply {
            duration = 317
            startOffset = 283
            interpolator = fastOutSlowIn()
            setFillEnabled()
        })
        return anim
    }

    private fun getTaskOpenExitAnim(): Animation {
        val anim = AnimationSet(false)
        anim.addAnimation(TranslateAnimation(0.0f, 1.0499878f, 0.0f, 0.0f).apply {
            duration = 383
            startOffset = 50
            interpolator = aggressiveEase()
            setFillEnabled()
            setRelativeToSelf()
        })
        anim.addAnimation(ScaleAnimation(1.0f, 0.95f, 1.0f, 0.95f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply {
            duration = 283
            interpolator = fastOutSlowIn()
            setFillEnabled()
        })
        return anim
    }

    private fun getTaskCloseEnterAnim(): Animation {
        val anim = AnimationSet(false)
        anim.addAnimation(TranslateAnimation(1.0499878f, 0.0f, 0.0f, 0.0f).apply {
            duration = 383
            startOffset = 50
            interpolator = aggressiveEase()
            setFillEnabled()
            setRelativeToSelf()
        })
        anim.addAnimation(ScaleAnimation(1.0526f, 1.0f, 1.0526f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply {
            duration = 283
            interpolator = fastOutSlowIn()
            setFillEnabled()
        })
        anim.addAnimation(ScaleAnimation(0.95f, 1.0f, 0.95f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply {
            duration = 317
            startOffset = 283
            interpolator = fastOutSlowIn()
            setFillEnabled()
        })
        return anim
    }

    private fun getTaskCloseExitAnim(): Animation {
        val anim = AnimationSet(false)
        anim.addAnimation(TranslateAnimation(0.0f, -1.0499878f, 0.0f, 0.0f).apply {
            duration = 383
            startOffset = 50
            interpolator = aggressiveEase()
            setFillEnabled()
            setRelativeToSelf()
        })
        anim.addAnimation(ScaleAnimation(1.0f, 0.95f, 1.0f, 0.95f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f).apply {
            duration = 283
            interpolator = fastOutSlowIn()
            setFillEnabled()
        })
        return anim
    }

    private fun Animation.setFillEnabled() {
        isFillEnabled = true
        fillBefore = true
        fillAfter = true
    }

    private fun TranslateAnimation.setRelativeToSelf() {
        XposedHelpers.setIntField(this, "mFromXType", Animation.RELATIVE_TO_SELF)
        XposedHelpers.setIntField(this, "mToXType", Animation.RELATIVE_TO_SELF)
        XposedHelpers.setIntField(this, "mFromYType", Animation.RELATIVE_TO_SELF)
        XposedHelpers.setIntField(this, "mToYType", Animation.RELATIVE_TO_SELF)
    }

    private fun fastOutSlowIn(): Interpolator {
        return PathInterpolator(0.4f, 0.0f, 0.2f, 1.0f)
    }

    private fun fastOutExtraSlowIn(): Interpolator {
        return PathInterpolator(PathParser.createPathFromPathData("M 0,0 C 0.05, 0, 0.133333, 0.06, 0.166666, 0.4 C 0.208333, 0.82, 0.25, 1, 1, 1"))
    }

    private fun activityCloseDim(): Interpolator {
        return PathInterpolator(0.33f, 0.0f, 1.0f, 1.0f)
    }

    private fun aggressiveEase(): Interpolator {
        return PathInterpolator(0.2f, 0.0f, 0.0f, 1.0f)
    }
}

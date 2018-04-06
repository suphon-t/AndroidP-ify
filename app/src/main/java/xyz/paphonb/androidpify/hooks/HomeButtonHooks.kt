package xyz.paphonb.androidpify.hooks

import android.annotation.SuppressLint
import android.content.res.XResources
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import de.robv.android.xposed.IXposedHookInitPackageResources
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_LoadPackage
import xyz.paphonb.androidpify.MainHook
import xyz.paphonb.androidpify.R
import xyz.paphonb.androidpify.aosp.ButtonInterface
import xyz.paphonb.androidpify.aosp.OpaLayout
import xyz.paphonb.androidpify.utils.ConfigUtils


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

object HomeButtonHooks : IXposedHookLoadPackage, IXposedHookInitPackageResources {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != MainHook.PACKAGE_SYSTEMUI) return
        if (!ConfigUtils.misc.pixelHomeButton) return

        val classNavigationBarInflaterView = XposedHelpers.findClass("com.android.systemui.statusbar.phone.NavigationBarInflaterView", lpparam.classLoader)
        XposedHelpers.findAndHookMethod(classNavigationBarInflaterView, "createView",
                String::class.java, ViewGroup::class.java, LayoutInflater::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val view = param.result as View
                if (view.javaClass.simpleName == "OpaLayout") return // Already using OpaLayout
                val context = view.context
                val homeId = context.resources.getIdentifier("home", "id", MainHook.PACKAGE_SYSTEMUI)
                if (view.id == homeId) {
                    param.result = OpaLayout(context).apply { setHome(view as ImageView) }
                }
            }
        })

        val classButtonDispatcher = XposedHelpers.findClass("com.android.systemui.statusbar.phone.ButtonDispatcher", lpparam.classLoader)
        val classKeyButtonDrawable = XposedHelpers.findClass("com.android.systemui.statusbar.policy.KeyButtonDrawable", lpparam.classLoader)
        XposedHelpers.findAndHookMethod(classButtonDispatcher, "addView",
                View::class.java, object : XC_MethodHook() {
            @SuppressLint("ClickableViewAccessibility")
            override fun beforeHookedMethod(param: MethodHookParam) {
                val view = param.args[0] as? OpaLayout ?: return

                val mClickListener = XposedHelpers.getObjectField(param.thisObject, "mClickListener") as View.OnClickListener?
                val mTouchListener = XposedHelpers.getObjectField(param.thisObject, "mTouchListener") as View.OnTouchListener?
                val mLongClickListener = XposedHelpers.getObjectField(param.thisObject, "mLongClickListener") as View.OnLongClickListener?
                val mLongClickable = XposedHelpers.getObjectField(param.thisObject, "mLongClickable") as Boolean?
                val mAlpha = XposedHelpers.getObjectField(param.thisObject, "mAlpha") as Int?
                val mDarkIntensity = XposedHelpers.getObjectField(param.thisObject, "mDarkIntensity") as Float?
                val mVisibility = XposedHelpers.getObjectField(param.thisObject, "mVisibility") as Int?
                val mImageDrawable = XposedHelpers.getObjectField(param.thisObject, "mImageDrawable") as Drawable?
                val mVertical = XposedHelpers.getBooleanField(param.thisObject, "mVertical")
                getViews(param).add(view)
                view.setOnClickListener(mClickListener)
                view.setOnTouchListener(mTouchListener)
                view.setOnLongClickListener(mLongClickListener)
                mLongClickable?.let { view.isLongClickable = it }
                mAlpha?.let { view.alpha = it.toFloat() }
                mDarkIntensity?.let { setDarkIntensity(view, it) }
                mVisibility?.let { view.visibility = it }
                mImageDrawable?.let { setImageDrawable(view, it) }

                view.setVertical(mVertical)

                param.result = null
            }
        })

        try {
            XposedHelpers.findAndHookMethod(classButtonDispatcher, "setImageDrawable",
                    classKeyButtonDrawable, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val drawable = param.args[0] as Drawable?
                    XposedHelpers.setObjectField(param.thisObject, "mImageDrawable", drawable)
                    getViews(param).forEach { setImageDrawable(it, drawable) }
                    param.result = null
                }
            })
        } catch (t: Throwable) {
            MainHook.logE("HomeButtonHooks", "can't hook setImageDrawable", t)
        }

        try {
            XposedHelpers.findAndHookMethod(classButtonDispatcher, "abortCurrentGesture", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    getViews(param).forEach { abortCurrentGesture(it) }
                    param.result = null
                }
            })
        } catch (t: Throwable) {
            MainHook.logE("HomeButtonHooks", "can't hook abortCurrentGesture", t)
        }

        try {
            XposedHelpers.findAndHookMethod(classButtonDispatcher, "setDarkIntensity",
                    Float::class.java, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val intensity = param.args[0] as Float
                    XposedHelpers.setObjectField(param.thisObject, "mDarkIntensity", java.lang.Float.valueOf(intensity))
                    getViews(param).forEach { setDarkIntensity(it, intensity) }
                    param.result = null
                }
            })
        } catch (t: Throwable) {
            MainHook.logE("HomeButtonHooks", "can't hook setDarkIntensity", t)
        }

        try {
            XposedHelpers.findAndHookMethod(classButtonDispatcher, "setVertical",
                    Boolean::class.java, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val vertical = param.args[0] as Boolean
                    XposedHelpers.setObjectField(param.thisObject, "mVertical", java.lang.Boolean.valueOf(vertical))
                    getViews(param).forEach { setVertical(it, vertical) }
                    param.result = null
                }
            })
        } catch (t: Throwable) {
            MainHook.logE("HomeButtonHooks", "can't hook setVertical", t)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getViews(param: XC_MethodHook.MethodHookParam): ArrayList<View> {
        return XposedHelpers.getObjectField(param.thisObject, "mViews") as ArrayList<View>
    }

    private fun setImageDrawable(view: View, imageDrawable: Drawable?) {
        if (view is ButtonInterface) {
            view.setImageDrawable(imageDrawable)
        } else {
            XposedHelpers.callMethod(view, "setImageDrawable", arrayOf(Drawable::class.java), imageDrawable)
        }
    }

    private fun abortCurrentGesture(view: View) {
        if (view is ButtonInterface) {
            view.abortCurrentGesture()
        } else {
            XposedHelpers.callMethod(view, "abortCurrentGesture")
        }
    }

    private fun setDarkIntensity(view: View, intensity: Float) {
        if (view is ButtonInterface) {
            view.setDarkIntensity(intensity)
        } else {
            XposedHelpers.callMethod(view, "setDarkIntensity", arrayOf(Float::class.java), intensity)
        }
    }

    private fun setVertical(view: View, vertical: Boolean) {
        if (view is ButtonInterface) {
            view.setVertical(vertical)
        } else {
            XposedHelpers.callMethod(view, "setVertical", arrayOf(Boolean::class.java), vertical)
        }
    }

    override fun handleInitPackageResources(resparam: XC_InitPackageResources.InitPackageResourcesParam) {
        if (resparam.packageName != MainHook.PACKAGE_SYSTEMUI) return
        if (!ConfigUtils.misc.pixelHomeButton) return

        tryReplaceDrawable(resparam.res, "ic_sysbar_accessibility_button_dark", R.drawable.ic_sysbar_accessibility_button_dark)
        tryReplaceDrawable(resparam.res, "ic_sysbar_accessibility_button", R.drawable.ic_sysbar_accessibility_button)
        tryReplaceDrawable(resparam.res, "ic_sysbar_back_carmode", R.drawable.ic_sysbar_back_carmode)
        tryReplaceDrawable(resparam.res, "ic_sysbar_back_dark", R.drawable.ic_sysbar_back_dark)
        tryReplaceDrawable(resparam.res, "ic_sysbar_back_ime_carmode", R.drawable.ic_sysbar_back_ime_carmode)
        tryReplaceDrawable(resparam.res, "ic_sysbar_back_ime_dark", R.drawable.ic_sysbar_back_ime_dark)
        tryReplaceDrawable(resparam.res, "ic_sysbar_back_ime", R.drawable.ic_sysbar_back_ime)
        tryReplaceDrawable(resparam.res, "ic_sysbar_back", R.drawable.ic_sysbar_back)
        tryReplaceDrawable(resparam.res, "ic_sysbar_docked_dark", R.drawable.ic_sysbar_docked_dark)
        tryReplaceDrawable(resparam.res, "ic_sysbar_docked", R.drawable.ic_sysbar_docked)
        tryReplaceDrawable(resparam.res, "ic_sysbar_home_carmode", R.drawable.ic_sysbar_home_carmode)
        tryReplaceDrawable(resparam.res, "ic_sysbar_home_dark", R.drawable.ic_sysbar_home_dark)
        tryReplaceDrawable(resparam.res, "ic_sysbar_home", R.drawable.ic_sysbar_home)
        tryReplaceDrawable(resparam.res, "ic_sysbar_lights_out_dot_large", R.drawable.ic_sysbar_lights_out_dot_large)
        tryReplaceDrawable(resparam.res, "ic_sysbar_lights_out_dot_small", R.drawable.ic_sysbar_lights_out_dot_small)
        tryReplaceDrawable(resparam.res, "ic_sysbar_menu_dark", R.drawable.ic_sysbar_menu_dark)
        tryReplaceDrawable(resparam.res, "ic_sysbar_menu", R.drawable.ic_sysbar_menu)
        tryReplaceDrawable(resparam.res, "ic_sysbar_recent_dark", R.drawable.ic_sysbar_recent_dark)
        tryReplaceDrawable(resparam.res, "ic_sysbar_recent", R.drawable.ic_sysbar_recent)
    }

    private fun tryReplaceDrawable(res: XResources, name: String, replacement: Int) {
        try {
            res.setReplacement(MainHook.PACKAGE_SYSTEMUI, "drawable", name, MainHook.modRes.fwd(replacement))
        } catch (ignored: Throwable) {

        }
    }
}
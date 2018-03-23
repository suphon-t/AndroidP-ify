package xyz.paphonb.androidpify.hooks.helpers

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.View
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

class NotificationBackgroundViewHelper(classLoader: ClassLoader, target: View) : ObjectHelper<View>(classLoader, target) {

    private val mClipTopAmount by IntField("mClipTopAmount")
    private val mClipBottomAmount by IntField("mClipBottomAmount")
    private val mActualHeight by IntField("mActualHeight")
    private val mBackground by AnyField<Drawable>("mBackground")
    private var mBackgroundTop = 0
    private val mExpandAnimationRunning = false

    private var mBottomAmountClips = true
    private var mBottomIsRounded = false

    private val mActualWidth get() = target.width

    private val mCornerRadii = FloatArray(8)

    private fun onDraw(canvas: Canvas) {
        if (mClipTopAmount + mClipBottomAmount < mActualHeight - mBackgroundTop || mExpandAnimationRunning) {
            canvas.save()
            if (!mExpandAnimationRunning) {
                canvas.clipRect(0, mClipTopAmount, target.width, mActualHeight - mClipBottomAmount)
            }
            draw(canvas, mBackground)
            canvas.restore()
        }
    }

    private fun draw(canvas: Canvas, drawable: Drawable?) {
        if (drawable != null) {
            var bottom = mActualHeight
            if (mBottomIsRounded && mBottomAmountClips && !mExpandAnimationRunning) {
                bottom -= mClipBottomAmount
            }
            var left = 0
            var right = target.width
            if (mExpandAnimationRunning) {
                left = ((target.width.toFloat() - mActualWidth) / 2.0f).toInt()
                right = (left.toFloat() + mActualWidth).toInt()
            }
            drawable.setBounds(left, mBackgroundTop, right, bottom)
            drawable.draw(canvas)
        }
    }

    fun setRoundness(topRoundness: Float, bottomRoundNess: Float) {
        mBottomIsRounded = bottomRoundNess != 0.0f
        mCornerRadii[0] = topRoundness
        mCornerRadii[1] = topRoundness
        mCornerRadii[2] = topRoundness
        mCornerRadii[3] = topRoundness
        mCornerRadii[4] = bottomRoundNess
        mCornerRadii[5] = bottomRoundNess
        mCornerRadii[6] = bottomRoundNess
        mCornerRadii[7] = bottomRoundNess
        updateBackgroundRadii()
    }

    fun setBottomAmountClips(clips: Boolean) {
        if (clips != mBottomAmountClips) {
            mBottomAmountClips = clips
            target.invalidate()
        }
    }

    private fun updateBackgroundRadii() {
        if (mBackground is LayerDrawable) {
            ((mBackground as LayerDrawable).getDrawable(0) as GradientDrawable).cornerRadii = mCornerRadii
        }
    }

    fun setBackgroundTop(backgroundTop: Int) {
        mBackgroundTop = backgroundTop
        target.invalidate()
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

        fun get(classLoader: ClassLoader, child: View): NotificationBackgroundViewHelper {
            return getManager(classLoader).get(child)
        }
    }

    class Manager(private val classLoader: ClassLoader) {

        private val TAG = "Helper"
        private val classNotificationBackgroundView = XposedHelpers.findClass("com.android.systemui.statusbar.NotificationBackgroundView", classLoader)

        init {
            XposedHelpers.findAndHookConstructor(classNotificationBackgroundView,
                    Context::class.java, AttributeSet::class.java, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    get(param.thisObject as View)
                }
            })

            XposedHelpers.findAndHookMethod(classNotificationBackgroundView, "onDraw",
                    Canvas::class.java, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    get(param.thisObject as View).onDraw(param.args[0] as Canvas)
                }
            })

            val invalidateHook = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    (param.thisObject as View).invalidate()
                }
            }

            XposedHelpers.findAndHookMethod(classNotificationBackgroundView, "setActualHeight",
                    Int::class.java, invalidateHook)

            XposedHelpers.findAndHookMethod(classNotificationBackgroundView, "setClipTopAmount",
                    Int::class.java, invalidateHook)

            XposedHelpers.findAndHookMethod(classNotificationBackgroundView, "setClipBottomAmount",
                    Int::class.java, invalidateHook)
        }

        fun get(child: View): NotificationBackgroundViewHelper {
            var helper = XposedHelpers.getAdditionalInstanceField(child, TAG) as NotificationBackgroundViewHelper?
            if (helper == null) {
                helper = NotificationBackgroundViewHelper(classLoader, child)
                XposedHelpers.setAdditionalInstanceField(child, TAG, helper)
            }
            return helper
        }
    }
}
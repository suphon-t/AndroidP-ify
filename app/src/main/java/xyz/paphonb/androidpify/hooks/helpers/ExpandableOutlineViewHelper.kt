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

package xyz.paphonb.androidpify.hooks.helpers

import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Path
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import xyz.paphonb.androidpify.hooks.NotificationStackHook


open class ExpandableOutlineViewHelper(classLoader: ClassLoader, target: ViewGroup)
    : ObjectHelper<ViewGroup>(classLoader, target) {

    private val EMPTY_PATH = Path()
    private var mCurrentBottomRoundness = 0f
    private var mCurrentTopRoundness = 0f
    private var mBottomRoundness = 0f
    private var mTopRoundness = 0f
    private var mBackgroundTop: Int = 0
    private var mClipRoundedToClipTopAmount: Boolean = false
    private var mDistanceToTopRoundness: Float = -1f
    private val mClipPath = Path()
    private val mOutlineRadius get() = NotificationStackHook.mCornerRadius
    private val mTmpPath = Path()
    private val mTmpPath2 = Path()

    private val mProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            with(target) {
                if (isCustomOutline() || mCurrentTopRoundness != 0f || mCurrentBottomRoundness != 0f) {
                    val clipPath = getClipPath()
                    if (clipPath.isConvex) {
                        outline.setConvexPath(clipPath)
                    }
                } else {
                    val shouldTranslateContents = XposedHelpers.getBooleanField(this, "mShouldTranslateContents")
                    val translation = if (shouldTranslateContents) getTranslation().toInt() else 0
                    val mClipTopAmount = getClipTopAmount()
                    val mClipBottomAmount = getClipBottomAmount()
                    val top = mClipTopAmount + mBackgroundTop
                    outline.setRect(Math.max(translation, 0), top, width + Math.min(translation, 0), Math.max(getActualHeight() - mClipBottomAmount, top))
                }
                outline.alpha = XposedHelpers.getFloatField(this, "mOutlineAlpha")
            }
        }
    }

    fun afterInit() {
        XposedHelpers.setObjectField(target, "mProvider", mProvider)
        target.outlineProvider =
                XposedHelpers.getObjectField(target, "mProvider") as ViewOutlineProvider
    }

    fun setDistanceToTopRoundness(distanceToTopRoundness: Float) {
        if (distanceToTopRoundness != mDistanceToTopRoundness) {
            mClipRoundedToClipTopAmount = distanceToTopRoundness >= 0.0f
            mDistanceToTopRoundness = distanceToTopRoundness
            target.invalidate()
        }
    }

    private fun childNeedsClipping(child: View): Boolean {
        return true
    }

    fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        with(target) {
            canvas.save()
            var intersectPath: Path? = null
            val mClipTopAmount = getClipTopAmount()
            val mClipBottomAmount = getClipBottomAmount()
            if (mClipRoundedToClipTopAmount) {
                val top = (mClipTopAmount.toFloat() - mDistanceToTopRoundness).toInt()
                getRoundedRectPath(0, top, width, Math.max((getActualHeight() - mClipBottomAmount).toFloat(), top.toFloat() + mOutlineRadius).toInt(), mOutlineRadius.toFloat(), 0.0f, mClipPath)
                intersectPath = mClipPath
            }
            var clipped = false
            if (childNeedsClipping(child)) {
                val clipPath = getCustomClipPath(child) ?: getClipPath()
                if (intersectPath != null) {
                    clipPath.op(intersectPath, Path.Op.INTERSECT)
                }
                canvas.clipPath(clipPath)
                clipped = true
            }
            if (!(clipped || intersectPath == null)) {
                canvas.clipPath(intersectPath)
            }
            val result = XposedHelpers.callMethod(child, "draw", canvas, this, drawingTime) as Boolean
            canvas.restore()
            return result

//            canvas.save()
//            var intersectPath: Path? = null
//            if (mClipRoundedToClipTopAmount) {
//                val top = (getClipTopAmount().toFloat() - mDistanceToTopRoundness).toInt()
//                getRoundedRectPath(0, top, width,
//                        Math.max((getActualHeight() - getClipBottomAmount()).toFloat(), top.toFloat() + mOutlineRadius).toInt(),
//                        mOutlineRadius.toFloat(), 0.0f, mClipPath)
//                intersectPath = mClipPath
//            }
//            var clipped = false
//            if (childNeedsClipping(child)) {
//                var clipPath = getCustomClipPath(child)
//                if (clipPath == null) {
//                    clipPath = getClipPath()
//                }
//                if (intersectPath != null) {
//                    clipPath.op(intersectPath, Path.Op.INTERSECT)
//                }
//                canvas.clipPath(clipPath)
//                clipped = true
//            }
//            if (!(clipped || intersectPath == null)) {
//                canvas.clipPath(intersectPath)
//            }
//            val result = XposedHelpers.callMethod(child, "draw", canvas, target, drawingTime) as Boolean
//            canvas.restore()
//            return result
        }
    }

    private fun getCustomClipPath(child: View): Path? {
        return null
    }

    private fun getClipPath(): Path {
        return getClipPath(false, false)
    }

    private fun getClipPath(ignoreTranslation: Boolean, clipRoundedToBottom: Boolean): Path {
        with (target) {
            val left: Int
            val top: Int
            val right: Int
            var bottom: Int
            var intersectPath: Path? = null
            val shouldTranslateContents = XposedHelpers.getBooleanField(this, "mShouldTranslateContents")
            val mClipTopAmount = XposedHelpers.getIntField(this, "mClipTopAmount")
            val mClipBottomAmount = XposedHelpers.getIntField(this, "mClipBottomAmount")
            val mAlwaysRoundBothCorners = false
            if (isCustomOutline()) {
                val mOutlineRect = XposedHelpers.getObjectField(this, "mOutlineRect") as Rect
                left = mOutlineRect.left
                top = mOutlineRect.top
                right = mOutlineRect.right
                bottom = mOutlineRect.bottom
            } else {
                val translation = if (!shouldTranslateContents || ignoreTranslation) 0 else getTranslation().toInt()
                left = Math.max(translation, 0)
                top = mClipTopAmount + mBackgroundTop
                right = Math.min(translation, 0) + width
                bottom = Math.max(getActualHeight(), top)
                val intersectBottom = Math.max(getActualHeight() - mClipBottomAmount, top)
                if (bottom != intersectBottom) {
                    if (clipRoundedToBottom) {
                        bottom = intersectBottom
                    } else {
                        getRoundedRectPath(left, top, right, intersectBottom, 0.0f, 0.0f, mTmpPath2)
                        intersectPath = mTmpPath2
                    }
                }
            }
            bottom -= top
            if (bottom == 0) {
                return EMPTY_PATH
            }
            var topRoundness = if (mAlwaysRoundBothCorners) mOutlineRadius.toFloat() else mCurrentTopRoundness * mOutlineRadius
            var bottomRoundness = if (mAlwaysRoundBothCorners) mOutlineRadius.toFloat() else mCurrentBottomRoundness * mOutlineRadius
            if (topRoundness + bottomRoundness > bottom.toFloat()) {
                val overShoot = topRoundness + bottomRoundness - bottom.toFloat()
                topRoundness -= mCurrentTopRoundness * overShoot / (mCurrentTopRoundness + mCurrentBottomRoundness)
                bottomRoundness -= mCurrentBottomRoundness * overShoot / (mCurrentTopRoundness + mCurrentBottomRoundness)
            }
            getRoundedRectPath(left, top, right, bottom, topRoundness, bottomRoundness, mTmpPath)
            val roundedRectPath = mTmpPath
            if (intersectPath != null) {
                roundedRectPath.op(intersectPath, Path.Op.INTERSECT)
            }
            return roundedRectPath
        }
    }

    private fun getRoundedRectPath(left: Int, top: Int, right: Int, bottom: Int, topRoundness: Float, bottomRoundness: Float, outPath: Path) {
        outPath.reset()
        val width = right - left
        var bottomRoundnessX = bottomRoundness
        val topRoundnessX = Math.min((width / 2).toFloat(), topRoundness)
        bottomRoundnessX = Math.min((width / 2).toFloat(), bottomRoundnessX)
        if (topRoundness > 0.0f) {
            outPath.moveTo(left.toFloat(), top.toFloat() + topRoundness)
            outPath.quadTo(left.toFloat(), top.toFloat(), left.toFloat() + topRoundnessX, top.toFloat())
            outPath.lineTo(right.toFloat() - topRoundnessX, top.toFloat())
            outPath.quadTo(right.toFloat(), top.toFloat(), right.toFloat(), top.toFloat() + topRoundness)
        } else {
            outPath.moveTo(left.toFloat(), top.toFloat())
            outPath.lineTo(right.toFloat(), top.toFloat())
        }
        if (bottomRoundness > 0.0f) {
            outPath.lineTo(right.toFloat(), bottom.toFloat() - bottomRoundness)
            outPath.quadTo(right.toFloat(), bottom.toFloat(), right.toFloat() - bottomRoundnessX, bottom.toFloat())
            outPath.lineTo(left.toFloat() + bottomRoundnessX, bottom.toFloat())
            outPath.quadTo(left.toFloat(), bottom.toFloat(), left.toFloat(), bottom.toFloat() - bottomRoundness)
        } else {
            outPath.lineTo(right.toFloat(), bottom.toFloat())
            outPath.lineTo(left.toFloat(), bottom.toFloat())
        }
        outPath.close()
    }

    private fun getActualHeight(): Int {
        return NotificationStackHook.methodGetActualHeight.invoke(target) as Int
    }

    private fun getClipTopAmount(): Int {
        return XposedHelpers.getIntField(target, "mClipTopAmount")
    }

    private fun getClipBottomAmount(): Int {
        return XposedHelpers.getIntField(target, "mClipBottomAmount")
    }

    private fun isCustomOutline(): Boolean {
        return NotificationStackHook.fieldCustomOutline.getBoolean(target)
    }

    fun getCurrentBackgroundRadiusTop(): Float {
        return mCurrentTopRoundness * mOutlineRadius
    }

    fun getCurrentTopRoundness(): Float {
        return mCurrentTopRoundness
    }

    fun getCurrentBottomRoundness(): Float {
        return mCurrentBottomRoundness
    }

    protected fun getCurrentBackgroundRadiusBottom(): Float {
        return mCurrentBottomRoundness * mOutlineRadius
    }

    fun setTopRoundness(topRoundness: Float, animate: Boolean) {
        if (mTopRoundness != topRoundness) {
            mTopRoundness = topRoundness
            setTopRoundnessInternal(topRoundness)
        }
    }

    fun setBottomRoundness(bottomRoundness: Float, animate: Boolean) {
        if (mBottomRoundness != bottomRoundness) {
            mBottomRoundness = bottomRoundness
            setBottomRoundnessInternal(bottomRoundness)
        }
    }

    private fun setTopRoundnessInternal(topRoundness: Float) {
        mCurrentTopRoundness = topRoundness
        applyRoundness()
    }

    private fun setBottomRoundnessInternal(bottomRoundness: Float) {
        mCurrentBottomRoundness = bottomRoundness
        applyRoundness()
    }

    protected open fun applyRoundness() {
        with(target) {
            invalidateOutline()
            invalidate()
        }
    }

    protected open fun setBackgroundTop(backgroundTop: Int) {
        if (mBackgroundTop != backgroundTop) {
            mBackgroundTop = backgroundTop
            target.invalidateOutline()
        }
    }

    private fun getTranslation(): Float {
        return XposedHelpers.callMethod(target, "getTranslation") as Float
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

        fun get(classLoader: ClassLoader, child: ViewGroup): ExpandableOutlineViewHelper {
            return getManager(classLoader).get(child)
        }
    }

    class Manager(private val classLoader: ClassLoader) {

        private val TAG = "Helper"
        private val classExpandableOutlineView by lazy { XposedHelpers.findClass("com.android.systemui.statusbar.ExpandableOutlineView", classLoader) }

        init {
            XposedHelpers.findAndHookConstructor(classExpandableOutlineView,
                    Context::class.java, AttributeSet::class.java, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    get(param.thisObject as ViewGroup)
                }

                override fun afterHookedMethod(param: MethodHookParam) {
                    super.afterHookedMethod(param)
                    get(param.thisObject as ViewGroup).afterInit()
                }
            })

            val applyRoundnessHook = object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    get(param.thisObject as ViewGroup).applyRoundness()
                }
            }

            XposedHelpers.findAndHookMethod(classExpandableOutlineView, "setActualHeight",
                    Int::class.java, Boolean::class.java, applyRoundnessHook)

            XposedHelpers.findAndHookMethod(classExpandableOutlineView, "setClipTopAmount",
                    Int::class.java, applyRoundnessHook)

            XposedHelpers.findAndHookMethod(classExpandableOutlineView, "setClipBottomAmount",
                    Int::class.java, applyRoundnessHook)

            XposedHelpers.findAndHookMethod(classExpandableOutlineView, "setOutlineAlpha",
                    Float::class.java, applyRoundnessHook)
        }

        fun get(child: ViewGroup): ExpandableOutlineViewHelper {
            var helper = XposedHelpers.getAdditionalInstanceField(child, TAG) as ExpandableOutlineViewHelper?
            if (helper == null) {
                helper = ExpandableOutlineViewHelper(classLoader, child)
                XposedHelpers.setAdditionalInstanceField(child, TAG, helper)
            }
            return helper
        }
    }
}
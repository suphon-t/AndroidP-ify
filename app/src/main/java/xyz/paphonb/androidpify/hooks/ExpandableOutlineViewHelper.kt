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

import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Path
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import de.robv.android.xposed.XposedHelpers

open class ExpandableOutlineViewHelper(private val expandableOutlineView: ViewGroup) {

    private val EMPTY_PATH = Path()
    private var mCurrentBottomRoundness = 0f
    private var mCurrentTopRoundness = 0f
    private var mBottomRoundness = 0f
    private var mTopRoundness = 0f
    private val mBackgroundTop: Int = 0
    private var mClipRoundedToClipTopAmount: Boolean = false
    private var mDistanceToTopRoundness: Float = -1f
    private val mClipPath = Path()
    private var mOutlineRadius = 0
    private val mTmpPath = Path()
    private val mTmpPath2 = Path()

    private val mProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            with(expandableOutlineView) {
                if (isCustomOutline() || mCurrentTopRoundness != 0.0f || mCurrentBottomRoundness != 0.0f) {
                    val clipPath = getClipPath()
                    if (clipPath.isConvex) {
                        outline.setConvexPath(clipPath)
                    }
                } else {
                    val translation = translationX
                    val top = getClipTopAmount() + mBackgroundTop
                    outline.setRect(Math.max(translation.toInt(), 0), top,
                            width + Math.min(translation.toInt(), 0),
                            Math.max(getActualHeight() - getClipBottomAmount(), top))
                }
                outline.alpha = NotificationStackHook.fieldOutlineAlpha.getFloat(this)

//                val translation = translationX.toInt()
//                if (!isCustomOutline()) {
//                    outline.setRoundRect(translation,
//                            getClipTopAmount(),
//                            width + translation,
//                            Math.max(getActualHeight() - getClipBottomAmount(), getClipTopAmount()),
//                            mOutlineRadius.toFloat())
//                } else {
//                    outline.setRoundRect(NotificationStackHook.fieldOutlineRect.get(this) as Rect, mOutlineRadius.toFloat())
//                }
//                outline.alpha = NotificationStackHook.fieldOutlineAlpha.getFloat(this)
            }
        }
    }

    init {
//        XposedHelpers.setObjectField(expandableOutlineView, "mProvider", mProvider)
//        expandableOutlineView.outlineProvider =
//                XposedHelpers.getObjectField(expandableOutlineView, "mProvider") as ViewOutlineProvider
    }

    fun setDistanceToTopRoundness(distanceToTopRoundness: Float) {
        if (distanceToTopRoundness != mDistanceToTopRoundness) {
            mClipRoundedToClipTopAmount = distanceToTopRoundness >= 0.0f
            mDistanceToTopRoundness = distanceToTopRoundness
            expandableOutlineView.invalidate()
        }
    }

    private fun childNeedsClipping(child: View): Boolean {
        return true
    }

    fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        with(expandableOutlineView) {
            canvas.save()
            var intersectPath: Path? = null
            if (mClipRoundedToClipTopAmount) {
                val top = (getClipTopAmount().toFloat() - mDistanceToTopRoundness).toInt()
                getRoundedRectPath(0, top, width,
                        Math.max((getActualHeight() - getClipBottomAmount()).toFloat(), top.toFloat() + mOutlineRadius).toInt(),
                        mOutlineRadius.toFloat(), 0.0f, mClipPath)
                intersectPath = mClipPath
            }
            var clipped = false
            if (childNeedsClipping(child)) {
                var clipPath = getCustomClipPath(child)
                if (clipPath == null) {
                    clipPath = getClipPath()
                }
                if (intersectPath != null) {
                    clipPath.op(intersectPath, Path.Op.INTERSECT)
                }
                canvas.clipPath(clipPath)
                clipped = true
            }
            if (!(clipped || intersectPath == null)) {
                canvas.clipPath(intersectPath)
            }
            val result = XposedHelpers.callMethod(child, "draw", canvas, expandableOutlineView, drawingTime) as Boolean
            canvas.restore()
            return result
        }
    }

    private fun getCustomClipPath(child: View): Path? {
        return null
    }

    private fun getClipPath(): Path {
        return getClipPath(false, false)
    }

    private fun getClipPath(ignoreTranslation: Boolean, clipRoundedToBottom: Boolean): Path {
        with (expandableOutlineView) {
            val left: Int
            val top: Int
            val right: Int
            var bottom: Int
            var intersectPath: Path? = null
            if (isCustomOutline()) {
                val mOutlineRect = NotificationStackHook.fieldOutlineRect.get(this) as Rect
                left = mOutlineRect.left
                top = mOutlineRect.top
                right = mOutlineRect.right
                bottom = mOutlineRect.bottom
            } else {
                val translation = if (ignoreTranslation) 0 else translationX.toInt()
                left = Math.max(translation, 0)
                top = getClipTopAmount() + mBackgroundTop
                right = Math.min(translation, 0) + width
                bottom = Math.max(getActualHeight(), top)
                val intersectBottom = Math.max(getActualHeight() - getClipBottomAmount(), top)
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
            var topRoundness = mCurrentTopRoundness * NotificationStackHook.mCornerRadius
            var bottomRoundness = mCurrentBottomRoundness * NotificationStackHook.mCornerRadius
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
        return NotificationStackHook.methodGetActualHeight.invoke(expandableOutlineView) as Int
    }

    private fun getClipTopAmount(): Int {
        return NotificationStackHook.methodGetClipTopAmount.invoke(expandableOutlineView) as Int
    }

    private fun getClipBottomAmount(): Int {
        return NotificationStackHook.methodGetClipBottomAmount.invoke(expandableOutlineView) as Int
    }

    private fun isCustomOutline(): Boolean {
        return NotificationStackHook.fieldCustomOutline.getBoolean(expandableOutlineView)
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

    private fun applyRoundness() {
        with(expandableOutlineView) {
            invalidateOutline()
            invalidate()
        }
    }

    companion object {
        private const val TAG = "ExpandableOutlineViewHelper"

        fun get(child: ViewGroup): ExpandableOutlineViewHelper {
            var helper = XposedHelpers.getAdditionalInstanceField(child, TAG) as ExpandableOutlineViewHelper?
            if (helper == null) {
                helper = ExpandableOutlineViewHelper(child)
            }
            return helper
        }
    }
}
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

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import de.robv.android.xposed.IXposedHookInitPackageResources
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_LayoutInflated
import de.robv.android.xposed.callbacks.XC_LoadPackage
import xyz.paphonb.androidpify.MainHook
import xyz.paphonb.androidpify.R
import xyz.paphonb.androidpify.hooks.helpers.ExpandableOutlineViewHelper
import xyz.paphonb.androidpify.utils.ConfigUtils
import xyz.paphonb.androidpify.utils.setGoogleSans
import java.lang.ref.WeakReference

@SuppressLint("StaticFieldLeak")
object NotificationStackHook : IXposedHookLoadPackage, IXposedHookInitPackageResources {

    private lateinit var classLoader: ClassLoader
    private lateinit var stackRef: WeakReference<ViewGroup>
    private val stack get() = stackRef.get()!!

    private val classStack by lazy { XposedHelpers.findClass("com.android.systemui.statusbar.stack.NotificationStackScrollLayout", classLoader) }
    private val fieldShouldDrawNotificationBackground by lazy { XposedHelpers.findField(classStack, "mShouldDrawNotificationBackground") }
    private val fieldCurrentBounds by lazy { XposedHelpers.findField(classStack, "mCurrentBounds") }
    private val fieldAmbientState by lazy { XposedHelpers.findField(classStack, "mAmbientState") }
    private val fieldOwnScrollY by lazy { XposedHelpers.findField(classStack, "mOwnScrollY") }
    private val fieldBackgroundPaint by lazy { XposedHelpers.findField(classStack, "mBackgroundPaint") }
    private val fieldTopPadding by lazy { XposedHelpers.findField(classStack, "mTopPadding") }
    private val methodGetIntrinsicPadding by lazy { XposedHelpers.findMethodExact(classStack, "getIntrinsicPadding", *blankArray())!! }

    private val classExpandableView by lazy { XposedHelpers.findClass("com.android.systemui.statusbar.ExpandableView", classLoader) }
    val methodGetActualHeight by lazy { XposedHelpers.findMethodExact(classExpandableView, "getActualHeight", *blankArray())!! }
    val methodGetClipTopAmount by lazy { XposedHelpers.findMethodExact(classExpandableView, "getClipTopAmount", *blankArray())!!}
    val methodGetClipBottomAmount by lazy { XposedHelpers.findMethodExact(classExpandableView, "getClipBottomAmount", *blankArray())!! }

    private val classExpandableOutlineView by lazy { XposedHelpers.findClass("com.android.systemui.statusbar.ExpandableOutlineView", classLoader) }
    val fieldCustomOutline by lazy { XposedHelpers.findField(classExpandableOutlineView, "mCustomOutline")!! }
    val fieldOutlineRect by lazy { XposedHelpers.findField(classExpandableOutlineView, "mOutlineRect")!! }
    val fieldOutlineAlpha by lazy { XposedHelpers.findField(classExpandableOutlineView, "mOutlineAlpha")!! }

    private val classNotificationPanelView by lazy { XposedHelpers.findClass("com.android.systemui.statusbar.phone.NotificationPanelView", classLoader) }
    private val classPanelView by lazy { XposedHelpers.findClass("com.android.systemui.statusbar.phone.PanelView", classLoader) }
    private val fieldQsMinExpansionHeight by lazy { XposedHelpers.findField(classNotificationPanelView, "mQsMinExpansionHeight")!! }
    private val fieldQsMaxExpansionHeight by lazy { XposedHelpers.findField(classNotificationPanelView, "mQsMaxExpansionHeight")!! }
    private val fieldKeyguardShowing by lazy { XposedHelpers.findField(classNotificationPanelView, "mKeyguardShowing")!! }
    private val fieldQsExpandImmediate by lazy { XposedHelpers.findField(classNotificationPanelView, "mQsExpandImmediate")!! }
    private val fieldIsExpanding by lazy { XposedHelpers.findField(classNotificationPanelView, "mIsExpanding")!! }
    private val fieldQsExpandedWhenExpandingStarted by lazy { XposedHelpers.findField(classNotificationPanelView, "mQsExpandedWhenExpandingStarted")!! }
    private val fieldStatusBarState by lazy { XposedHelpers.findField(classNotificationPanelView, "mStatusBarState")!! }
    private val fieldQsSizeChangeAnimator by lazy { XposedHelpers.findField(classNotificationPanelView, "mQsSizeChangeAnimator")!! }
    private val fieldQsExpansionHeight by lazy { XposedHelpers.findField(classNotificationPanelView, "mQsExpansionHeight")!! }
    private val methodGetQsExpansionFraction by lazy { XposedHelpers.findMethodExact(classNotificationPanelView, "getQsExpansionFraction", *blankArray())!! }
    private val methodGetTempQsMaxExpansion by lazy { XposedHelpers.findMethodExact(classNotificationPanelView, "getTempQsMaxExpansion", *blankArray())!! }
    private val methodGetExpandedFraction by lazy { XposedHelpers.findMethodExact(classPanelView, "getExpandedFraction", *blankArray())!! }

    val mSidePaddings by lazy { MainHook.modRes.getDimensionPixelSize(R.dimen.notification_side_paddings) }
    val mSeparatorWidth by lazy { MainHook.modRes.getDimensionPixelSize(R.dimen.widget_separator_width) }
    val mSeparatorThickness by lazy { MainHook.modRes.getDimensionPixelSize(R.dimen.widget_separator_thickness) }
    val mDarkSeparatorPadding by lazy { MainHook.modRes.getDimensionPixelSize(R.dimen.widget_bottom_separator_padding) }
    val mCornerRadius by lazy { MainHook.modRes.getDimensionPixelSize(R.dimen.notification_corner_radius) }

    val mClipPath = Path()

    var mTmpBitmap: Bitmap? = null
    var mTmpCanvas: Canvas? = null
    val mPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC) }
    val mClipPaint = Paint(Color.WHITE).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC) }

    fun getTmpCanvas(canvas: Canvas): Canvas {
        if (mTmpCanvas == null || (mTmpCanvas!!.width != canvas.width || mTmpCanvas!!.height != canvas.height)) {
            mTmpBitmap = Bitmap.createBitmap(canvas.width, canvas.height, Bitmap.Config.ARGB_8888)
            mTmpCanvas = Canvas(mTmpBitmap)
        }
        mTmpBitmap!!.eraseColor(0)
        return mTmpCanvas!!
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != MainHook.PACKAGE_SYSTEMUI) return
        if (!ConfigUtils.notifications.changePullDown) return

        classLoader = lpparam.classLoader

        ExpandableOutlineViewHelper.hook(classLoader)
//        ActivatableNotificationViewHelper.hook(classLoader)
//        NotificationBackgroundViewHelper.hook(classLoader)

//        findAndHookConstructor(classExpandableOutlineView, Context::class.java, AttributeSet::class.java,
//                object : XC_MethodHook() {
//                    override fun afterHookedMethod(param: MethodHookParam) {
//                        ExpandableOutlineViewHelper.get(param.thisObject as ViewGroup)
//                    }
//                })

//        val updateClippingToTopRoundedCornerHook = object : XC_MethodHook() {
//            override fun afterHookedMethod(param: MethodHookParam?) {
//                updateClippingToTopRoundedCorner()
//            }
//        }
//
//        XposedHelpers.findAndHookMethod(classStack, "startAnimationToState", updateClippingToTopRoundedCornerHook)
//        XposedHelpers.findAndHookMethod(classStack, "applyCurrentState", updateClippingToTopRoundedCornerHook)
//        XposedHelpers.findAndHookMethod(classStack, "onPreDrawDuringAnimation", updateClippingToTopRoundedCornerHook)

//        XposedHelpers.findAndHookMethod(classExpandableOutlineView, "initDimens",
//                object : XC_MethodHook() {
//            override fun beforeHookedMethod(param: MethodHookParam) {
//                ExpandableOutlineViewHelper.get(param.thisObject as ViewGroup)
//            }
//        })

        findAndHookMethod(ViewGroup::class.java, "drawChild",
                Canvas::class.java, View::class.java, Long::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (!classStack.isInstance(param.thisObject)) return
                if (!classExpandableOutlineView.isInstance(param.args[1])) return
                param.result = drawChild(param.args[0] as Canvas,
                        param.args[1] as View,
                        param.args[2] as Long)
//
//                if (!classExpandableOutlineView.isInstance(param.thisObject)) return
//
//                param.result = ExpandableOutlineViewHelper.get(param.thisObject as ViewGroup)
//                        .drawChild(param.args[0] as Canvas,
//                                param.args[1] as View,
//                                param.args[2] as Long)
            }
        })

        findAndHookMethod(classStack, "initView",
                Context::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                stackRef = WeakReference(param.thisObject as ViewGroup)
            }
        })

        findAndHookMethod(classStack, "applyCurrentBackgroundBounds",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    stack.invalidate()
                    param.result = null
                }
            })

        findAndHookMethod(classStack, "onMeasure",
                Int::class.java, Int::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                (stack).apply {
                    val widthMeasureSpec = param.args[0] as Int
                    val heightMeasureSpec = param.args[1] as Int
                    val childWidthSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec) - mSidePaddings * 2, MeasureSpec.getMode(widthMeasureSpec))
                    // We need to measure all children even the GONE ones, such that the heights are calculated
                    // correctly as they are used to calculate how many we can fit on the screen.
                    val size = childCount
                    for (i in 0 until size) {
                        XposedHelpers.callMethod(param.thisObject,
                                "measureChild", getChildAt(i), childWidthSpec, heightMeasureSpec)
                    }
                }
            }
        })

        findAndHookMethod(classStack, "onDraw",
                Canvas::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                param.result = null
                onDraw(param.args[0] as Canvas)
            }
        })

        findAndHookMethod(classStack, "updateFirstAndLastBackgroundViews",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        updateFirstAndLastBackgroundViews()
                        param.result = null
                    }
                })

        findAndHookMethod(classNotificationPanelView, "calculateQsTopPadding",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = calculateQsTopPadding(param.thisObject as FrameLayout)
                    }
                })

//        val colorForeground = Color.WHITE
//        val intensity = (0).toFloat()
//        val tintArea = Rect(0, 0, 0, 0)
//
//        applyDarkness(R.id.battery, tintArea, intensity, colorForeground)
//        applyDarkness(R.id.clock, tintArea, intensity, colorForeground)

        // private void applyDarkness(int id, Rect tintArea, float intensity, int color)
    }

    private fun onDraw(canvas: Canvas) {
        with(stack) {
            val mShouldDrawNotificationBackground = fieldShouldDrawNotificationBackground.getBoolean(stack)
            val mCurrentBounds = fieldCurrentBounds.get(stack) as Rect
            val mAmbientState = fieldAmbientState.get(stack)
            val mBackgroundPaint = fieldBackgroundPaint.get(stack) as Paint

            if (mShouldDrawNotificationBackground && !(XposedHelpers.callMethod(mAmbientState, "isDark") as Boolean)
                    && mCurrentBounds.top < mCurrentBounds.bottom) {
                val left = mSidePaddings
                val right = width - mSidePaddings
                val top = mCurrentBounds.top
                val bottom = mCurrentBounds.bottom

                canvas.drawRoundRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(),
                        mCornerRadius.toFloat(), mCornerRadius.toFloat(), mBackgroundPaint)
            }
        }
    }

    fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        with(stack) {
            val mCurrentBounds = fieldCurrentBounds.get(stack) as Rect
            val start = mCurrentBounds.top
            val end = mCurrentBounds.bottom
            val childStart = child.translationY;
            val childEnd = (Math.max(methodGetActualHeight.invoke(child) as Int -
                    methodGetClipBottomAmount.invoke(child) as Int, 0)) + childStart
            val left = mSidePaddings
            val right = width - mSidePaddings
            canvas.save()
            if (end <= start) {
//                getRoundedRectPath(left, childStart.toInt(), right, childEnd.toInt(), mCornerRadius.toFloat(), mCornerRadius.toFloat(), mClipPath)
                // Don't do anything for now
            } else {
                getRoundedRectPath(left, start, right, end, mCornerRadius.toFloat(), mCornerRadius.toFloat(), mClipPath)
                canvas.clipPath(mClipPath)
            }
//            val tmpCanvas = getTmpCanvas(canvas)
            val result = XposedHelpers.callMethod(child, "draw", canvas, this, drawingTime) as Boolean
//            tmpCanvas.drawPath(mClipPath, mClipPaint)
//            canvas.drawBitmap(mTmpBitmap!!, 0f, 0f, mPaint)
            canvas.restore()
            return result
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

    private fun updateFirstAndLastBackgroundViews() {
        with(stack) {
            val firstChild = XposedHelpers.callMethod(this, "getFirstChildWithBackground")
            val lastChild = XposedHelpers.callMethod(this, "getLastChildWithBackground")

            val mFirstVisibleBackgroundChild = XposedHelpers.getObjectField(this, "mFirstVisibleBackgroundChild")
            val mLastVisibleBackgroundChild = XposedHelpers.getObjectField(this, "mLastVisibleBackgroundChild")
            val firstChanged = firstChild !== mFirstVisibleBackgroundChild
            val lastChanged = lastChild !== mLastVisibleBackgroundChild

            val mAnimationsEnabled = XposedHelpers.getBooleanField(this, "mAnimationsEnabled")
            val mIsExpanded = XposedHelpers.getBooleanField(this, "mIsExpanded")
            if (mAnimationsEnabled && mIsExpanded) {
                XposedHelpers.setBooleanField(this, "mAnimateNextBackgroundTop", firstChanged)
                XposedHelpers.setBooleanField(this, "mAnimateNextBackgroundBottom", lastChanged)
            } else {
                XposedHelpers.setBooleanField(this, "mAnimateNextBackgroundTop", false)
                XposedHelpers.setBooleanField(this, "mAnimateNextBackgroundBottom", false)
            }
            if (firstChanged && mFirstVisibleBackgroundChild !== null) {
                ExpandableOutlineViewHelper.get(classLoader, mFirstVisibleBackgroundChild as ViewGroup).apply {
                    setTopRoundness(0f, target.isShown)
                }
            }
            if (lastChanged && mLastVisibleBackgroundChild !== null) {
                ExpandableOutlineViewHelper.get(classLoader, mLastVisibleBackgroundChild as ViewGroup).apply {
                    setBottomRoundness(0f, target.isShown)
                }
            }
            XposedHelpers.setObjectField(this, "mFirstVisibleBackgroundChild", firstChild)
            XposedHelpers.setObjectField(this, "mLastVisibleBackgroundChild", lastChild)
            val mAmbientState = XposedHelpers.getObjectField(this, "mAmbientState")
            XposedHelpers.callMethod(mAmbientState, "setLastVisibleBackgroundChild", lastChild)
            applyRoundedNess()
        }
    }

    private fun applyRoundedNess() {
        with(stack) {
            val mFirstVisibleBackgroundChild = XposedHelpers.getObjectField(this, "mFirstVisibleBackgroundChild") as View?
            val mLastVisibleBackgroundChild = XposedHelpers.getObjectField(this, "mLastVisibleBackgroundChild") as View?
            if (mFirstVisibleBackgroundChild !== null && mFirstVisibleBackgroundChild.isShown) {
                ExpandableOutlineViewHelper.get(classLoader,
                        mFirstVisibleBackgroundChild as ViewGroup).setTopRoundness(1f, false)
            }
            if (mLastVisibleBackgroundChild !== null && mLastVisibleBackgroundChild.isShown) {
                ExpandableOutlineViewHelper.get(classLoader,
                        mLastVisibleBackgroundChild as ViewGroup).setBottomRoundness(1f, false)
            }
        }
    }

    private fun calculateQsTopPadding(panel: FrameLayout): Float {
        with(panel) {
            val mQsMinExpansionHeight = fieldQsMinExpansionHeight.getInt(panel)
            val mQsMaxExpansionHeight = fieldQsMaxExpansionHeight.getInt(panel)
            val mKeyguardShowing = fieldKeyguardShowing.getBoolean(panel)
            val mQsExpandImmediate = fieldQsExpandImmediate.getBoolean(panel)
            val mIsExpanding = fieldIsExpanding.getBoolean(panel)
            val mQsExpandedWhenExpandingStarted = fieldQsExpandedWhenExpandingStarted.getBoolean(panel)
            val mStatusBarState = fieldStatusBarState.getInt(panel)
            val mQsSizeChangeAnimator = fieldQsSizeChangeAnimator.get(panel) as ValueAnimator?
            val mQsExpansionHeight = fieldQsExpansionHeight.getFloat(panel)
            if (mKeyguardShowing && (mQsExpandImmediate || mIsExpanding && mQsExpandedWhenExpandingStarted)) {
                // Either QS pushes the notifications down when fully expanded, or QS is fully above the
                // notifications (mostly on tablets). maxNotifications denotes the normal top padding
                // on Keyguard, maxQs denotes the top padding from the quick settings panel. We need to
                // take the maximum and linearly interpolate with the panel expansion for a nice motion.
                val mClockPositionResult = XposedHelpers.getObjectField(panel, "mClockPositionResult")
                val stackScrollerPadding = XposedHelpers.getIntField(mClockPositionResult, "stackScrollerPadding")
                val stackScrollerPaddingAdjustment = XposedHelpers.getIntField(mClockPositionResult, "stackScrollerPaddingAdjustment")
                val maxNotifications = stackScrollerPadding - stackScrollerPaddingAdjustment
                val maxQs = methodGetTempQsMaxExpansion.invoke(panel) as Int + mSidePaddings
                val max = if (mStatusBarState == 1)
                    Math.max(maxNotifications, maxQs)
                else
                    maxQs
                return (interpolate(methodGetExpandedFraction.invoke(panel) as Float,
                        mQsMinExpansionHeight.toFloat(), max.toFloat()).toInt()).toFloat()
            } else return if (mQsSizeChangeAnimator != null) {
                (mQsSizeChangeAnimator.animatedValue as Int).toFloat()
            } else if (mKeyguardShowing) {

                // We can only do the smoother transition on Keyguard when we also are not collapsing
                // from a scrolled quick settings.
                interpolate(methodGetQsExpansionFraction.invoke(panel) as Float,
                        (methodGetIntrinsicPadding.invoke(stack) as Int).toFloat(),
                        (mQsMaxExpansionHeight + mSidePaddings).toFloat())
            } else {
                mQsExpansionHeight + mSidePaddings
            }
        }
    }

    private fun interpolate(t: Float, start: Float, end: Float): Float {
        return (1 - t) * start + t * end
    }

    private fun blankArray() = arrayOf<Class<*>>()

    override fun handleInitPackageResources(resparam: XC_InitPackageResources.InitPackageResourcesParam) {
        resparam.res.hookLayout(MainHook.PACKAGE_ANDROID, "layout", "notification_material_action",
                object : XC_LayoutInflated() {
                    override fun handleLayoutInflated(liparam: LayoutInflatedParam) {
                        val button = liparam.view as Button
                        button.setAllCaps(false)
                        button.setGoogleSans("Medium")
                    }
                })

        if (resparam.packageName != MainHook.PACKAGE_SYSTEMUI) return

        resparam.res.hookLayout(MainHook.PACKAGE_SYSTEMUI, "layout", "status_bar_notification_dismiss_all",
                object : XC_LayoutInflated() {
                    override fun handleLayoutInflated(liparam: LayoutInflatedParam) {
                        val id = liparam.view.context.resources.getIdentifier(
                                "dismiss_text", "id", MainHook.PACKAGE_SYSTEMUI)
                        liparam.view.findViewById<TextView>(id)?.run {
                            if (setGoogleSans("Medium")) {
                                setAllCaps(false)
                            }
                        }
                    }
                })
    }
}

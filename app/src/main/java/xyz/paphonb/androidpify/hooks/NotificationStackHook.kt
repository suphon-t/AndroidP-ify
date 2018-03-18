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
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import de.robv.android.xposed.IXposedHookInitPackageResources
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_LoadPackage
import xyz.paphonb.androidpify.MainHook
import xyz.paphonb.androidpify.R
import xyz.paphonb.androidpify.aosp.StatusIconContainer
import xyz.paphonb.androidpify.utils.ConfigUtils
import xyz.paphonb.androidpify.utils.ResourceUtils
import xyz.paphonb.androidpify.utils.getColorAttr
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

    private val classQSContainerImpl by lazy { XposedHelpers.findClass("com.android.systemui.qs.QSContainerImpl", classLoader) }
    private val classQuickStatusBarHeader by lazy { XposedHelpers.findClass("com.android.systemui.qs.QuickStatusBarHeader", classLoader) }
    private val classQSFooterImpl by lazy { XposedHelpers.findClass("com.android.systemui.qs.QSFooterImpl", classLoader) }
    private val classTouchAnimatorBuilder by lazy { XposedHelpers.findClass("com.android.systemui.qs.TouchAnimator\$Builder", classLoader) }
    private val classTintedIconManager by lazy { XposedHelpers.findClass("com.android.systemui.statusbar.phone.StatusBarIconController\$TintedIconManager", classLoader) }
    private val classStatusBarIconController by lazy { XposedHelpers.findClass("com.android.systemui.statusbar.phone.StatusBarIconController", classLoader) }
    private val classDependency by lazy { XposedHelpers.findClass("com.android.systemui.Dependency", classLoader) }

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
                    override fun afterHookedMethod(param: MethodHookParam) {
                        applyRoundedNess()
                    }
                })

        findAndHookMethod(classNotificationPanelView, "calculateQsTopPadding",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        param.result = calculateQsTopPadding(param.thisObject as FrameLayout)
                    }
                })

        findAndHookMethod(classQSContainerImpl, "onFinishInflate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val qsContainer = param.thisObject as ViewGroup
                        val context = qsContainer.context
                        val ownContext = ResourceUtils.createOwnContext(context)
                        val qsElevation = ownContext.resources.getDimensionPixelSize(R.dimen.qs_background_elevation).toFloat()

                        qsContainer.background = null

                        qsContainer.addView(View(context).apply {
                            id = R.id.quick_settings_background
                            background = getBackground(context)
                            elevation = qsElevation
                            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0)
                            setMargins(this)
                        }, 0)

                        qsContainer.addView(View(context).apply {
                            id = R.id.quick_settings_status_bar_background
                            background = ColorDrawable(0xFF000000.toInt())
                            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                    ownContext.resources.getDimensionPixelOffset(R.dimen.quick_qs_offset_height))
                        }, 1)

                        qsContainer.addView(View(context).apply {
                            id = R.id.quick_settings_gradient_view
                            background = ownContext.resources.getDrawable(R.drawable.qs_bg_gradient)
                            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                    ownContext.resources.getDimensionPixelOffset(R.dimen.qs_gradient_height)).apply {
                                topMargin = ownContext.resources.getDimensionPixelOffset(R.dimen.quick_qs_offset_height)
                            }
                        }, 2)

                        (XposedHelpers.getObjectField(param.thisObject, "mQSPanel") as View).apply {
                            elevation = qsElevation
                            setMargins(this)
                            (layoutParams as FrameLayout.LayoutParams).topMargin = ownContext
                                    .resources.getDimensionPixelSize(R.dimen.quick_qs_offset_height)
                        }

                        (XposedHelpers.getObjectField(param.thisObject, "mQSFooter") as View).apply {
                            elevation = qsElevation
                            setMargins(this)
                            findViewById<View>(context.resources.getIdentifier(
                                    "expand_indicator", "id", MainHook.PACKAGE_SYSTEMUI)).visibility = View.GONE
                        }

                        (XposedHelpers.getObjectField(param.thisObject, "mQSDetail") as View).apply {
                            elevation = qsElevation
                            background = getBackground(context)
                            setMargins(this)
                        }

                        (XposedHelpers.getObjectField(param.thisObject, "mQSCustomizer") as View).apply {
                            elevation = qsElevation
                            setMargins(this)
                        }

                        (XposedHelpers.getObjectField(param.thisObject, "mHeader") as View).apply {
                            elevation = qsElevation
                        }

                        qsContainer.findViewById<View>(context.resources.getIdentifier("quick_qs_panel", "id", MainHook.PACKAGE_SYSTEMUI)).apply {
                            (layoutParams as ViewGroup.MarginLayoutParams).topMargin = ownContext
                                    .resources.getDimensionPixelSize(R.dimen.quick_qs_top_margin)
                        }

                        // Swap CarrierText with DateView
                        val carrierText = qsContainer.findViewById<View>(context.resources.getIdentifier(
                                "qs_carrier_text", "id", MainHook.PACKAGE_SYSTEMUI))
                        (carrierText.parent as ViewGroup).removeView(carrierText)

                        val datePaddings = ownContext.resources
                                .getDimensionPixelSize(R.dimen.quick_qs_date_padding)
                        val date = qsContainer.findViewById<TextView>(context.resources.getIdentifier(
                                "date", "id", MainHook.PACKAGE_SYSTEMUI))
                        date.setTextColor(Color.WHITE)
                        date.setPadding(datePaddings, datePaddings, datePaddings, datePaddings)
                        (date.parent as ViewGroup).removeView(carrierText)
                        (date.layoutParams as LinearLayout.LayoutParams).apply {
                            width = 0
                            weight = 1f
                        }

                        val footerLayout = qsContainer.findViewById<ViewGroup>(R.id.quick_qs_footer_layout)
                        val footerLeft = footerLayout.getChildAt(0) as ViewGroup
                        footerLeft.removeAllViews()
                        footerLeft.addView(carrierText)

                        qsContainer.findViewById<ViewGroup>(R.id.quick_qs_system_icons)
                                .addView(date, 1)

                        XposedHelpers.callMethod(footerLayout.parent, "updateResources")
                    }
                })

        findAndHookMethod(classQSContainerImpl, "updateExpansion",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        with(param.thisObject as ViewGroup) {
                            val height = bottom - top
                            findViewById<View>(R.id.quick_settings_background).apply {
                                top = (XposedHelpers.getObjectField(param.thisObject, "mQSPanel") as View).top
                                bottom = height
                            }
                        }
                    }
                })

//        val colorForeground = Color.WHITE
//        val intensity = (0).toFloat()
//        val tintArea = Rect(0, 0, 0, 0)
//
//        applyDarkness(R.id.battery, tintArea, intensity, colorForeground)
//        applyDarkness(R.id.clock, tintArea, intensity, colorForeground)

        // private void applyDarkness(int id, Rect tintArea, float intensity, int color)

        var intensity = 1f
        var color = Color.BLACK

        findAndHookMethod(classQuickStatusBarHeader, "onFinishInflate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val header = param.thisObject as ViewGroup
                        val context = header.context

                        val systemIcons = header.getChildAt(0) as ViewGroup
                        val battery = systemIcons.findViewById<View>(context.resources.getIdentifier(
                                "battery", "id", MainHook.PACKAGE_SYSTEMUI))
                        val foreground = Color.WHITE
                        val background = 0x4DFFFFFF
                        XposedHelpers.callMethod(XposedHelpers.getObjectField(battery, "mDrawable"), "setColors", foreground, background)
                        XposedHelpers.callMethod(battery, "setTextColor", foreground)

                        // Move clock to left side
                        val clock = systemIcons.findViewById<View>(context.resources.getIdentifier(
                                "clock", "id", MainHook.PACKAGE_SYSTEMUI))
                        systemIcons.removeView(clock)
                        systemIcons.addView(clock, 0)
                        // Swap clock padding too
                        clock.setPadding(clock.paddingRight, clock.paddingTop,
                                clock.paddingLeft, clock.paddingBottom)

                        systemIcons.id = R.id.quick_qs_system_icons

                        val quickQsStatusIcons = LinearLayout(context)
                        quickQsStatusIcons.id = R.id.quick_qs_status_icons

                        val ownResources = ResourceUtils.createOwnContext(context).resources
                        quickQsStatusIcons.layoutParams = RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.MATCH_PARENT,
                                ownResources.getDimensionPixelSize(R.dimen.qs_status_icons_height)).apply {
                            addRule(RelativeLayout.BELOW, R.id.quick_qs_system_icons)
                            topMargin = ownResources.getDimensionPixelSize(R.dimen.qs_status_icons_margin_top)
                            bottomMargin = ownResources.getDimensionPixelSize(R.dimen.qs_status_icons_margin_bottom)
                        }
                        header.addView(quickQsStatusIcons, 0)

                        val statusIcons = StatusIconContainer(context, lpparam.classLoader)
                        statusIcons.layoutParams = LinearLayout.LayoutParams(
                                0, LinearLayout.LayoutParams.MATCH_PARENT).apply { weight = 1f }
                        quickQsStatusIcons.addView(statusIcons)

                        val ctw = ContextThemeWrapper(context, context.resources.getIdentifier(
                                "Theme.SystemUI", "style", MainHook.PACKAGE_SYSTEMUI))
                        val signalCluster = LayoutInflater.from(ctw).inflate(context.resources.getIdentifier(
                                "signal_cluster_view", "layout", MainHook.PACKAGE_SYSTEMUI),
                                quickQsStatusIcons, false)
                        signalCluster.id = R.id.signal_cluster
                        signalCluster.layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.MATCH_PARENT).apply {
                            marginStart = ownResources.getDimensionPixelSize(R.dimen.signal_cluster_margin_start)
                        }
                        quickQsStatusIcons.addView(signalCluster)

                        val fillColor = fillColorForIntensity(intensity, context)

                        val applyDarkness = XposedHelpers.findMethodExact(classQuickStatusBarHeader, "applyDarkness",
                                Int::class.java, Rect::class.java, Float::class.java, Int::class.java)
                        applyDarkness.invoke(header, R.id.signal_cluster, Rect(), intensity, fillColor)

                        val constructor = XposedHelpers.findConstructorExact(classTintedIconManager,
                                ViewGroup::class.java)
                        val iconManager = constructor.newInstance(statusIcons)
                        XposedHelpers.callMethod(iconManager, "setTint", fillColor)
                        XposedHelpers.setAdditionalInstanceField(header, "mIconManager",
                                iconManager)

                        XposedHelpers.callMethod(header, "updateResources")
                    }
                })

        findAndHookMethod(classQuickStatusBarHeader, "updateResources", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                classTouchAnimatorBuilder.newInstance().apply {
                    val header = param.thisObject as ViewGroup
                    val quickQsStatusIcons = header.findViewById<View>(R.id.quick_qs_status_icons)
                            ?: return
                    val addFloat = XposedHelpers.findMethodExact(classTouchAnimatorBuilder, "addFloat",
                            Object::class.java, String::class.java, FloatArray::class.java)
                    val alphas = FloatArray(2)
                    alphas[0] = 1f
                    alphas[1] = 0f
                    addFloat.invoke(this, quickQsStatusIcons, "alpha", alphas)
                    XposedHelpers.setAdditionalInstanceField(header, "animator", XposedHelpers.callMethod(this, "build"))
                }
            }
        })

        findAndHookMethod(classQuickStatusBarHeader, "setExpansion",
                Float::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                XposedHelpers.getAdditionalInstanceField(param.thisObject, "animator")?.let {
                    XposedHelpers.callMethod(it, "setPosition", param.args[0])
                }
            }
        })

        findAndHookMethod(ViewGroup::class.java, "onAttachedToWindow", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (!classQuickStatusBarHeader.isInstance(param.thisObject)) return

                val iconController = XposedHelpers.callStaticMethod(classDependency, "get", classStatusBarIconController)
                XposedHelpers.callMethod(iconController, "addIconGroup",
                        XposedHelpers.getAdditionalInstanceField(param.thisObject, "mIconManager"))
            }
        })

        findAndHookMethod(ViewGroup::class.java, "onDetachedFromWindow", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (!classQuickStatusBarHeader.isInstance(param.thisObject)) return

                val iconController = XposedHelpers.callStaticMethod(classDependency, "get", classStatusBarIconController)
                XposedHelpers.callMethod(iconController, "removeIconGroup",
                        XposedHelpers.getAdditionalInstanceField(param.thisObject, "mIconManager"))
            }
        })

        findAndHookMethod(classQuickStatusBarHeader, "applyDarkness",
                Int::class.java, Rect::class.java, Float::class.java, Int::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                intensity = param.args[2] as Float
                color = param.args[3] as Int
                if (param.args[0] as Int != R.id.signal_cluster)
                    param.result = null
            }
        })

        findAndHookMethod(classQSFooterImpl, "createSettingsAlphaAnimator",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        classTouchAnimatorBuilder.newInstance().apply {
                            val footer = param.thisObject as ViewGroup
                            val context = footer.context

                            val carrierTextId = context.resources.getIdentifier(
                                    "qs_carrier_text", "id", MainHook.PACKAGE_SYSTEMUI)
                            val mEdit = XposedHelpers.getObjectField(param.thisObject, "mEdit")
                            val mMultiUserSwitch = XposedHelpers.getObjectField(param.thisObject, "mMultiUserSwitch")
                            val mSettingsContainer = try {
                                XposedHelpers.getObjectField(param.thisObject, "mSettingsContainer")
                            } catch (e: NoSuchFieldError) {
                                XposedHelpers.getObjectField(param.thisObject, "mSettingsButton")
                            }
                            val carrierText = footer.findViewById<View>(carrierTextId)
                            val addFloat = XposedHelpers.findMethodExact(classTouchAnimatorBuilder, "addFloat",
                                    Object::class.java, String::class.java, FloatArray::class.java)
                            val alphas = FloatArray(2)
                            alphas[0] = 0f
                            alphas[1] = 1f
                            addFloat.invoke(this, mEdit, "alpha", alphas)
                            addFloat.invoke(this, mMultiUserSwitch, "alpha", alphas)
                            addFloat.invoke(this, mSettingsContainer, "alpha", alphas)
                            if (carrierText != null)
                                addFloat.invoke(this, carrierText, "alpha", alphas)
                            param.result = XposedHelpers.callMethod(this, "build")
                        }
                    }
                })

        val clearAlarmShowing = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                XposedHelpers.setBooleanField(param.thisObject, "mAlarmShowing", false)
            }
        }
        findAndHookMethod(classQSFooterImpl, "updateAnimator", Int::class.java, clearAlarmShowing)
        findAndHookMethod(classQSFooterImpl, "updateAlarmVisibilities", clearAlarmShowing)

        findAndHookMethod(classQSFooterImpl, "onFinishInflate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val footer = param.thisObject as ViewGroup
                        val context = footer.context

                        val layout = LinearLayout(context)
                        layout.id = R.id.quick_qs_footer_layout
                        layout.layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT)

                        footer.getChildAt(0).apply {
                            footer.removeView(this)
                            layout.addView(this)
                            layoutParams = LinearLayout.LayoutParams(layoutParams as ViewGroup.MarginLayoutParams).apply {
                                width = 0
                                weight = 1f
                            }
                        }

                        LayoutInflater.from(ResourceUtils.createOwnContext(context))
                                .inflate(R.layout.qs_pull_handle, layout, true)

                        footer.getChildAt(0).apply {
                            footer.removeView(this)
                            layout.addView(this)
                            layoutParams = LinearLayout.LayoutParams(layoutParams as ViewGroup.MarginLayoutParams).apply {
                                width = 0
                                weight = 1f
                            }
                        }

                        footer.addView(layout)
                    }
                })
    }

    private fun fillColorForIntensity(intensity: Float, context: Context): Int {
        return if (intensity == 0.0f) {
            val id = context.resources.getIdentifier("light_mode_icon_color_dual_tone_fill", "color", MainHook.PACKAGE_SYSTEMUI)
            context.getColor(id)
        } else {
            val id = context.resources.getIdentifier("dark_mode_icon_color_dual_tone_fill", "color", MainHook.PACKAGE_SYSTEMUI)
            context.getColor(id)
        }
    }

    private fun setMargins(view: View) {
        val lp = view.layoutParams as FrameLayout.LayoutParams
        lp.rightMargin = mSidePaddings
        lp.leftMargin = mSidePaddings
    }

    private fun getBackground(context: Context): Drawable {
        val foreground = context.getColorAttr(android.R.attr.colorForeground)
        val ownResources = ResourceUtils.createOwnContext(context).resources
        return if (foreground == Color.WHITE) {
            ownResources.getDrawable(R.drawable.qs_background_primary_dark, context.theme)
        } else {
            ownResources.getDrawable(R.drawable.qs_background_primary, context.theme)
        }
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

    private fun applyRoundedNess() {
        with(stack) {

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
        if (resparam.packageName != MainHook.PACKAGE_SYSTEMUI) return
        if (!ConfigUtils.notifications.changePullDown) return

        resparam.res.setReplacement(MainHook.PACKAGE_SYSTEMUI, "dimen", "qs_header_system_icons_area_height",
                MainHook.modRes.fwd(R.dimen.qs_header_system_icons_area_height))
        resparam.res.setReplacement(MainHook.PACKAGE_SYSTEMUI, "dimen", "status_bar_header_height",
                MainHook.modRes.fwd(R.dimen.quick_qs_total_height))
    }
}
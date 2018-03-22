package xyz.paphonb.androidpify.hooks

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.*
import android.view.View.MeasureSpec
import android.view.ViewGroup.MarginLayoutParams
import android.widget.*
import de.robv.android.xposed.*
import de.robv.android.xposed.XposedHelpers.findAndHookConstructor
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.callbacks.XC_InitPackageResources
import de.robv.android.xposed.callbacks.XC_LoadPackage
import xyz.paphonb.androidpify.MainHook
import xyz.paphonb.androidpify.R
import xyz.paphonb.androidpify.aosp.QSScrollLayout
import xyz.paphonb.androidpify.aosp.StatusIconContainer
import xyz.paphonb.androidpify.utils.*




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

object QuickSettingsHook : IXposedHookLoadPackage, IXposedHookInitPackageResources {

    lateinit var classLoader: ClassLoader

    private val classQSContainerImpl by lazy { XposedHelpers.findClass("com.android.systemui.qs.QSContainerImpl", classLoader) }
    private val classQuickStatusBarHeader by lazy { XposedHelpers.findClass("com.android.systemui.qs.QuickStatusBarHeader", classLoader) }
    private val classQSFooter by lazy { XposedHelpers.findClass("com.android.systemui.qs.QSFooter", classLoader) }
    private val classQSFooterImpl by lazy { XposedHelpers.findClass("com.android.systemui.qs.QSFooterImpl", classLoader) }
    private val classTouchAnimatorBuilder by lazy { XposedHelpers.findClass("com.android.systemui.qs.TouchAnimator\$Builder", classLoader) }
    private val classDarkIconManager by lazy { XposedHelpers.findClass("com.android.systemui.statusbar.phone.StatusBarIconController\$DarkIconManager", classLoader) }
    private val classTintedIconManager by lazy { XposedHelpers.findClass("com.android.systemui.statusbar.phone.StatusBarIconController\$TintedIconManager", classLoader) }
    private val classStatusBarIconController by lazy { XposedHelpers.findClass("com.android.systemui.statusbar.phone.StatusBarIconController", classLoader) }
    private val classDependency by lazy { XposedHelpers.findClass("com.android.systemui.Dependency", classLoader) }
    private val classQSIconView by lazy { XposedHelpers.findClass("com.android.systemui.plugins.qs.QSIconView", classLoader) }
    private val classQSTileBaseView by lazy { XposedHelpers.findClass("com.android.systemui.qs.tileimpl.QSTileBaseView", classLoader) }
    private val classQSTileState by lazy { XposedHelpers.findClass("com.android.systemui.plugins.qs.QSTile\$State", classLoader) }
    private val classSlashImageView by lazy { XposedHelpers.findClass("com.android.systemui.qs.tileimpl.SlashImageView", classLoader) }
    private val classSlashState by lazy { XposedHelpers.findClass("com.android.systemui.plugins.qs.QSTile.SlashState", classLoader) }
    private val classQSTileImpl by lazy { XposedHelpers.findClass("com.android.systemui.qs.tileimpl.QSTileImpl", classLoader) }
    private val classQSAnimator by lazy { XposedHelpers.findClass("com.android.systemui.qs.QSAnimator", classLoader) }
    private val classQSPanel by lazy { XposedHelpers.findClass("com.android.systemui.qs.QSPanel", classLoader) }
    private val classQuickQSPanel by lazy { XposedHelpers.findClass("com.android.systemui.qs.QuickQSPanel", classLoader) }
    private val classQS by lazy { XposedHelpers.findClass("com.android.systemui.plugins.qs.QS", classLoader) }
    private val classTileLayout by lazy { XposedHelpers.findClass("com.android.systemui.qs.TileLayout", classLoader) }

    val mSidePaddings by lazy { MainHook.modRes.getDimensionPixelSize(R.dimen.notification_side_paddings) }
    val mCornerRadius by lazy { MainHook.modRes.getDimensionPixelSize(R.dimen.notification_corner_radius) }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != MainHook.PACKAGE_SYSTEMUI) return
        if (!ConfigUtils.notifications.changePullDown) return

        classLoader = lpparam.classLoader

        findAndHookMethod(classQSContainerImpl, "onFinishInflate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val qsContainer = param.thisObject as ViewGroup
                        val context = qsContainer.context
                        val ownContext = ResourceUtils.createOwnContext(context)
                        val qsElevation = ownContext.resources.getDimensionPixelSize(R.dimen.qs_background_elevation).toFloat()

                        if (!MainHook.ATLEAST_O_MR1) {
                            qsContainer.removeViewAt(0)
                            XposedHelpers.setIntField(qsContainer, "mGutterHeight", 0)
                        }

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
                        footerLeft.background = null
                        footerLeft.setOnClickListener(null)

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
                            if (!MainHook.ATLEAST_O_MR1) {
                                val elevation = findViewById<View>(R.id.quick_settings_background).elevation
                                (XposedHelpers.getObjectField(param.thisObject, "mQSDetail") as View).elevation = elevation
                                (XposedHelpers.getObjectField(param.thisObject, "mQSFooter") as View).elevation = elevation
                                (XposedHelpers.getObjectField(param.thisObject, "mQSPanel") as View).elevation = elevation
                            }
                        }
                    }
                })

        var intensity = 1f

        findAndHookMethod(classQuickStatusBarHeader, "onFinishInflate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val header = param.thisObject as ViewGroup
                        val context = header.context

                        val quickStatusBarIcons = context.resources.getIdentifierNullable(
                                "quick_status_bar_icons", "id", MainHook.PACKAGE_SYSTEMUI)
                        header.findViewById<View?>(quickStatusBarIcons)?.apply {
                            header.removeViewAt(0)
                        }
                        val systemIcons = header.getChildAt(0) as ViewGroup
                        val battery = systemIcons.findViewById<View>(context.resources.getIdentifier(
                                "battery", "id", MainHook.PACKAGE_SYSTEMUI))
                        val foreground = Color.WHITE
                        val background = 0x4DFFFFFF
                        try {
                            XposedHelpers.callMethod(XposedHelpers.getObjectField(battery, "mDrawable"), "setColors", foreground, background)
                            XposedHelpers.callMethod(battery, "setTextColor", foreground)
                        } catch (ignored : Throwable) {

                        }

                        if (!MainHook.ATLEAST_O_MR1) {
                            systemIcons.layoutParams.height = MainHook.modRes
                                    .getDimensionPixelSize(R.dimen.qs_header_system_icons_area_height)
                        }

                        // Move clock to left side
                        val clock = systemIcons.findViewById<View>(context.resources.getIdentifier(
                                "clock", "id", MainHook.PACKAGE_SYSTEMUI))
                        systemIcons.removeView(clock)
                        systemIcons.addView(clock, 0)
                        // Swap clock padding too
                        clock.setPadding(clock.paddingRight, clock.paddingTop,
                                clock.paddingLeft, clock.paddingBottom)

                        systemIcons.findViewById<View?>(context.resources.getIdentifierNullable(
                                "left_clock", "id", MainHook.PACKAGE_SYSTEMUI))?.apply {
                            systemIcons.removeView(this)
                            systemIcons.addView(this, 0)
                        }

                        systemIcons.id = R.id.quick_qs_system_icons

                        val quickQsStatusIcons = LinearLayout(context)
                        quickQsStatusIcons.id = R.id.quick_qs_status_icons

                        val ownResources = ResourceUtils.createOwnContext(context).resources
                        if (header is RelativeLayout) {
                            quickQsStatusIcons.layoutParams = RelativeLayout.LayoutParams(
                                    RelativeLayout.LayoutParams.MATCH_PARENT,
                                    ownResources.getDimensionPixelSize(R.dimen.qs_status_icons_height)).apply {
                                addRule(RelativeLayout.BELOW, R.id.quick_qs_system_icons)
                                topMargin = ownResources.getDimensionPixelSize(R.dimen.qs_status_icons_margin_top)
                                bottomMargin = ownResources.getDimensionPixelSize(R.dimen.qs_status_icons_margin_bottom)
                            }
                        } else {
                            quickQsStatusIcons.layoutParams = FrameLayout.LayoutParams(
                                    RelativeLayout.LayoutParams.MATCH_PARENT,
                                    ownResources.getDimensionPixelSize(R.dimen.qs_status_icons_height)).apply {
                                topMargin = ownResources.getDimensionPixelSize(R.dimen.qs_status_icons_margin_top) +
                                        systemIcons.layoutParams.height
                                bottomMargin = ownResources.getDimensionPixelSize(R.dimen.qs_status_icons_margin_bottom)
                            }
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

                        val iconManager = if (MainHook.ATLEAST_O_MR1) {
                            val constructor = XposedHelpers.findConstructorExact(
                                    classTintedIconManager, ViewGroup::class.java)
                            constructor.newInstance(statusIcons).apply {
                                XposedHelpers.callMethod(this, "setTint", fillColor)
                            }
                        } else {
                            val constructor = XposedHelpers.findConstructorExact(
                                    classDarkIconManager, LinearLayout::class.java)
                            constructor.newInstance(statusIcons)
                        }
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
                XposedHelpers.getAdditionalInstanceField(param.thisObject, "mIconManager")?.let {
                    XposedHelpers.callMethod(iconController, "addIconGroup", it)
                } ?: logE("onAttachedToWindow: mIconManager = null")
            }
        })

        findAndHookMethod(ViewGroup::class.java, "onDetachedFromWindow", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (!classQuickStatusBarHeader.isInstance(param.thisObject)) return

                val iconController = XposedHelpers.callStaticMethod(classDependency, "get", classStatusBarIconController)
                XposedHelpers.getAdditionalInstanceField(param.thisObject, "mIconManager")?.let {
                    XposedHelpers.callMethod(iconController, "removeIconGroup", it)
                } ?: logE("onAttachedToWindow: mIconManager = null")
            }
        })

        findAndHookMethod(classQuickStatusBarHeader, "applyDarkness",
                Int::class.java, Rect::class.java, Float::class.java, Int::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                intensity = param.args[2] as Float
                if (param.args[0] as Int != R.id.signal_cluster)
                    param.result = null
            }
        })

        val footerClass = if (MainHook.ATLEAST_O_MR1) classQSFooterImpl else classQSFooter

        findAndHookMethod(footerClass, "createSettingsAlphaAnimator",
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
        findAndHookMethod(footerClass, "updateAnimator", Int::class.java, clearAlarmShowing)
        findAndHookMethod(footerClass, "updateAlarmVisibilities", clearAlarmShowing)

        findAndHookMethod(footerClass, "onFinishInflate",
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

        try {
            findAndHookMethod(classQuickStatusBarHeader, "onTuningChanged",
                    String::class.java, String::class.java, XC_MethodReplacement.DO_NOTHING)
        } catch (ignored: Throwable) {

        }

        if (ConfigUtils.notifications.circleTileBackground) {
            findAndHookConstructor(classQSTileBaseView, Context::class.java,
                    classQSIconView, Boolean::class.java, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val iconFrame = XposedHelpers.getObjectField(param.thisObject, "mIconFrame") as ViewGroup
                    val context = iconFrame.context

                    ImageView(context).apply {
                        id = R.id.qs_tile_background
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        setImageDrawable(MainHook.modRes.getDrawable(R.drawable.ic_qs_circle))
                        iconFrame.addView(this, 0)
                    }

                    XposedHelpers.setAdditionalInstanceField(param.thisObject, "mCircleColor", 0)
                    XposedHelpers.setAdditionalInstanceField(param.thisObject, "mColorActive",
                            context.getColorAccent())
                    XposedHelpers.setAdditionalInstanceField(param.thisObject, "mColorDisabled",
                            context.getDisabled(context.getColorAttr(android.R.attr.textColorTertiary)))
                }
            })

            findAndHookMethod(classQSTileBaseView, "handleStateChanged",
                    classQSTileState, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val tileBaseView = param.thisObject as ViewGroup
                    val bg = tileBaseView.findViewById<ImageView>(R.id.qs_tile_background)

                    val circleColor = getCircleColor(tileBaseView,
                            XposedHelpers.getIntField(param.args[0], "state"))
                    val mCircleColor = XposedHelpers.getAdditionalInstanceField(tileBaseView, "mCircleColor") as Int
                    if (circleColor != mCircleColor) {
                        if (bg.isShown) {
                            val animator = ValueAnimator.ofArgb(mCircleColor, circleColor).apply { duration = 350 }
                            animator.addUpdateListener { bg.imageTintList = ColorStateList.valueOf((it.animatedValue as Int)) }
                            animator.start()
                        } else {
                            bg.imageTintList = ColorStateList.valueOf(circleColor)
                        }
                        XposedHelpers.setAdditionalInstanceField(tileBaseView, "mCircleColor", circleColor)
                    }
                }
            })

            findAndHookMethod(classSlashImageView, "setState",
                    classSlashState, Drawable::class.java, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.args[0] = null
                }
            })

            findAndHookMethod(classQSTileImpl, "getColorForState",
                    Context::class.java, Int::class.java, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val state = param.args[1] as Int

                    if (state == 2) {
                        param.result = Color.WHITE
                    }
                }
            })

            findAndHookMethod(classQSAnimator, "onAnimationAtEnd",
                    object : XC_MethodHook() {
                        @Suppress("UNCHECKED_CAST")
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val topFiveQs = XposedHelpers.getObjectField(
                                    param.thisObject, "mTopFiveQs") as ArrayList<View>
                            topFiveQs.forEach { (it.parent as View).visibility = View.VISIBLE }
                        }
                    })

            findAndHookMethod(classQSAnimator, "onAnimationStarted",
                    object : XC_MethodHook() {
                        @Suppress("UNCHECKED_CAST")
                        override fun afterHookedMethod(param: MethodHookParam) {
                            if (!XposedHelpers.getBooleanField(param.thisObject, "mOnFirstPage"))
                                return

                            val topFiveQs = XposedHelpers.getObjectField(
                                    param.thisObject, "mTopFiveQs") as ArrayList<View>
                            topFiveQs.forEach { (it.parent as View).visibility = View.INVISIBLE }
                        }
                    })

            findAndHookMethod(classQSAnimator, "clearAnimationState",
                    object : XC_MethodHook() {
                        @Suppress("UNCHECKED_CAST")
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val topFiveQs = XposedHelpers.getObjectField(
                                    param.thisObject, "mTopFiveQs") as ArrayList<View>
                            topFiveQs.forEach { (it.parent as View).visibility = View.VISIBLE }
                        }
                    })

            findAndHookMethod(classQSPanel, "addDivider",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val divider = XposedHelpers.getObjectField(param.thisObject, "mDivider") as View
                            val context = divider.context

                            divider.setBackgroundColor(applyAlpha(divider.alpha,
                                    context.getColorAttr(android.R.attr.colorForeground)))
                        }
                    })
        }

        if (ConfigUtils.notifications.qsVerticalScroll) {
            findAndHookMethod(classQSPanel, "setupTileLayout",
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val qsPanel = param.thisObject as ViewGroup
                            val context = qsPanel.context

                            val constructor = XposedHelpers.findConstructorExact(classTileLayout,
                                    Context::class.java)
                            val tileLayout = constructor.newInstance(context)
                            XposedHelpers.callMethod(tileLayout, "setListening",
                                    XposedHelpers.getObjectField(qsPanel, "mListening"))
                            qsPanel.addView(tileLayout as View)
                            XposedHelpers.setObjectField(qsPanel, "mTileLayout", tileLayout)
                            param.result = null
                        }
                    })

            findAndHookConstructor(classQSPanel, Context::class.java, AttributeSet::class.java,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val qsPanel = param.thisObject as ViewGroup
                            val context = qsPanel.context

                            val brightnessView = XposedHelpers.getObjectField(qsPanel, "mBrightnessView") as View
                            val tileLayout = XposedHelpers.getObjectField(qsPanel, "mTileLayout") as View
                            val views = ArrayList<View>()
                            var startIndex = -1
                            for (i in (0 until qsPanel.childCount)) {
                                val view = qsPanel.getChildAt(i)
                                if (view === brightnessView || view === tileLayout) {
                                    views.add(view)
                                    if (startIndex == -1)
                                        startIndex = i
                                }
                            }
                            if (startIndex == -1)
                                return

                            qsPanel.removeView(brightnessView)
                            qsPanel.removeView(tileLayout)
                            tileLayout.isFocusable = false

                            val scrollLayout = QSScrollLayout(context, *views.toTypedArray())
                            scrollLayout.layoutParams = LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT, 0).apply { weight = 1f }
                            scrollLayout.id = R.id.qs_scroll_layout
                            qsPanel.addView(scrollLayout, startIndex)
                        }
                    })

            findAndHookMethod(classQSPanel, "setExpanded",
                    Boolean::class.java, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val qsPanel = param.thisObject as ViewGroup
                    val mExpanded = XposedHelpers.getBooleanField(qsPanel, "mExpanded")
                    val expanded = param.args[0] as Boolean
                    if (mExpanded != expanded && !mExpanded) {
                        val scrollLayout = qsPanel.findViewById<QSScrollLayout>(R.id.qs_scroll_layout)
                        scrollLayout.scrollY = 0
                    }
                }
            })

            findAndHookMethod(classQSPanel, "updateResources",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val qsPanel = param.thisObject as ViewGroup
                            val brightnessView = XposedHelpers.getObjectField(qsPanel, "mBrightnessView") as View

                            brightnessView.setPadding(
                                    brightnessView.paddingLeft,
                                    qsPanel.paddingTop,
                                    brightnessView.paddingRight,
                                    brightnessView.paddingBottom)

                            qsPanel.setPadding(0, 0, 0, qsPanel.paddingBottom)
                        }
                    })

            findAndHookConstructor(classQSAnimator, classQS, classQuickQSPanel, classQSPanel,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val qsPanel = param.args[2] as ViewGroup
                            val scrollLayout = qsPanel.findViewById<QSScrollLayout>(R.id.qs_scroll_layout)
                            scrollLayout.setOnScrollChangeListener { _, _, scrollY: Int, _, _ ->
                                XposedHelpers.callMethod(param.thisObject, "onPageChanged", scrollY == 0)
                            }
                            param.result = null
                        }
                    })

            findAndHookMethod(FrameLayout::class.java, "onMeasure",
                    Int::class.java, Int::class.java, object : XC_MethodHook() {
                var widthMeasureSpec = 0
                var heightMeasureSpec = 0

                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (!classQSContainerImpl.isInstance(param.thisObject)) return

                    widthMeasureSpec = param.args[0] as Int
                    heightMeasureSpec = param.args[1] as Int
                    val qsContainer = param.thisObject as FrameLayout
                    with(qsContainer) {
                        val mSizePoint = XposedHelpers.getObjectField(qsContainer, "mSizePoint") as Point
                        val mQSPanel = XposedHelpers.getObjectField(qsContainer, "mQSPanel") as View
                        val mQSCustomizer = XposedHelpers.getObjectField(qsContainer, "mQSCustomizer") as View
                        display.getRealSize(mSizePoint)
                        val config = resources.configuration
                        val navBelow = config.smallestScreenWidthDp >= 600 ||
                                config.orientation != Configuration.ORIENTATION_LANDSCAPE

                        val params = mQSPanel.layoutParams as MarginLayoutParams
                        var maxQs = mSizePoint.y - params.topMargin -
                                params.bottomMargin - paddingBottom -
                                MainHook.modRes.getDimensionPixelSize(R.dimen.qs_notif_collapsed_space)
                        if (navBelow) {
                            maxQs -= resources.getDimensionPixelSize(resources.getIdentifier(
                                    "navigation_bar_size", "dimen", MainHook.PACKAGE_SYSTEMUI))
                        }
                        mQSPanel.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(maxQs, MeasureSpec.AT_MOST))
                        val layoutParams = mQSPanel.layoutParams as FrameLayout.LayoutParams
                        val widthSpec = MeasureSpec.makeMeasureSpec(mQSPanel.measuredWidth, MeasureSpec.EXACTLY)
                        val heightSpec = MeasureSpec.makeMeasureSpec(layoutParams.topMargin +
                                layoutParams.bottomMargin + mQSPanel.measuredHeight, MeasureSpec.EXACTLY)
                        XposedBridge.invokeOriginalMethod(param.method, param.thisObject,
                                arrayOf(widthSpec, heightSpec))
                        mQSCustomizer.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(mSizePoint.y, MeasureSpec.EXACTLY))
                    }

                    param.result = null
                }
            })

            findAndHookMethod(ViewGroup::class.java, "onInterceptTouchEvent",
                    MotionEvent::class.java, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (!classQSPanel.isInstance(param.thisObject)) return

                    val qsPanel = param.thisObject as ViewGroup
                    val event = param.args[0] as MotionEvent
                    val mExpanded = XposedHelpers.getBooleanField(qsPanel, "mExpanded")
                    val mScrollLayout = qsPanel.findViewById<QSScrollLayout>(R.id.qs_scroll_layout)
                    val shouldIntercept = mScrollLayout.shouldIntercept(event)
                    param.result = mExpanded && mScrollLayout.shouldIntercept(event)
//                logI("onInterceptTouchEvent -> ${event.action}, $mExpanded && $shouldIntercept")
                }
            })
        }
    }

    private fun getCircleColor(view: ViewGroup, state: Int): Int {
        return when (state) {
            0, 1 -> XposedHelpers.getAdditionalInstanceField(view, "mColorDisabled") as Int
            2 -> XposedHelpers.getAdditionalInstanceField(view, "mColorActive") as Int
            else -> {
                val stringBuilder = StringBuilder()
                stringBuilder.append("Invalid state ")
                stringBuilder.append(state)
                logE(stringBuilder.toString())
                0
            }
        }
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

    override fun handleInitPackageResources(resparam: XC_InitPackageResources.InitPackageResourcesParam) {
        if (resparam.packageName != MainHook.PACKAGE_SYSTEMUI) return
        if (!ConfigUtils.notifications.changePullDown) return

        if (MainHook.ATLEAST_O_MR1) {
            resparam.res.setReplacement(MainHook.PACKAGE_SYSTEMUI, "dimen", "qs_header_system_icons_area_height",
                    MainHook.modRes.fwd(R.dimen.qs_header_system_icons_area_height))
        } else {
            resparam.res.setReplacement(MainHook.PACKAGE_SYSTEMUI, "dimen", "qs_gutter_height",
                    MainHook.modRes.fwd(R.dimen.zero))
        }
        resparam.res.setReplacement(MainHook.PACKAGE_SYSTEMUI, "dimen", "status_bar_header_height",
                MainHook.modRes.fwd(R.dimen.quick_qs_total_height))
        try {
            // Fix weird top margin on panel with scrollable QuickQS
            resparam.res.setReplacement(MainHook.PACKAGE_SYSTEMUI, "dimen", "qs_scroller_top_margin",
                    MainHook.modRes.fwd(R.dimen.zero))
            resparam.res.setReplacement(MainHook.PACKAGE_SYSTEMUI, "dimen", "qs_panel_margin_top",
                    MainHook.modRes.fwd(R.dimen.quick_qs_offset_height))
        } catch (ignored: Throwable) {

        }
    }
}
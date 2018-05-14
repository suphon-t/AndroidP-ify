package xyz.paphonb.androidpify.hooks

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.XResources
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.service.quicksettings.Tile
import android.text.TextUtils
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
import xyz.paphonb.androidpify.views.MobileSignalGroup
import kotlin.math.max
import kotlin.math.min

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
    private val classSignalTileView by lazy { XposedHelpers.findClass("com.android.systemui.qs.SignalTileView", classLoader) }
    private val classCellTileView by lazy { XposedHelpers.findClass("com.android.systemui.qs.CellTileView", classLoader) }
    private val classSignalIcon by lazy { XposedHelpers.findClass("com.android.systemui.qs.CellTileView\$SignalIcon", classLoader) }
    private val classBatteryMeterView by lazy { XposedHelpers.findClass("com.android.systemui.BatteryMeterView", classLoader) }
    private val classPageIndicator by lazy { XposedHelpers.findClass("com.android.systemui.qs.PageIndicator", classLoader) }
    private val classCellularTile by lazy { XposedHelpers.findClass("com.android.systemui.qs.tiles.CellularTile", classLoader) }
    private val classQSIconViewImpl by lazy { XposedHelpers.findClass("com.android.systemui.qs.tileimpl.QSIconViewImpl", classLoader) }
    private val classQSTileView by lazy { XposedHelpers.findClass("com.android.systemui.qs.tileimpl.QSTileView", classLoader) }
    private val classDrawableIcon by lazy { XposedHelpers.findClass("com.android.systemui.qs.tileimpl.QSTileImpl\$DrawableIcon", classLoader) }
    private val classResourceIcon by lazy { XposedHelpers.findClass("com.android.systemui.qs.tileimpl.QSTileImpl\$ResourceIcon", classLoader) }
    private val classRotationLockTile by lazy { XposedHelpers.findClass("com.android.systemui.qs.tiles.RotationLockTile", classLoader) }
    private val classBluetoothTile by lazy { XposedHelpers.findClass("com.android.systemui.qs.tiles.BluetoothTile", classLoader) }
    private val classBluetoothBatteryMeterDrawable by lazy { XposedHelpers.findClass("com.android.systemui.qs.tiles.BluetoothTile\$BluetoothBatteryDrawable", classLoader) }

    val mSidePaddings by lazy { MainHook.modRes.getDimensionPixelSize(R.dimen.notification_side_paddings) }
    val mCornerRadius by lazy { MainHook.modRes.getDimensionPixelSize(R.dimen.notification_corner_radius) }
    val qsHeaderSystemIconsAreaHeight by lazy { MainHook.modRes.getDimensionPixelSize(R.dimen.qs_header_system_icons_area_height) }
    val qsNotifCollapsedSpace by lazy { MainHook.modRes.getDimensionPixelSize(R.dimen.qs_notif_collapsed_space) }
    val signalIndicatorToIconFrameSpacing by lazy { MainHook.modRes.getDimensionPixelSize(R.dimen.signal_indicator_to_icon_frame_spacing) }

    var bgColor = Color.WHITE

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != MainHook.PACKAGE_SYSTEMUI) return
        if (!ConfigUtils.notifications.changePullDown) return

        classLoader = lpparam.classLoader

        findAndHookMethod(classQSContainerImpl, "onFinishInflate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val qsContainer = param.thisObject as ViewGroup
                        val context = qsContainer.context
                        val ownResources = ResourceUtils.getInstance(context)
                        val qsElevation = ownResources.getDimensionPixelSize(R.dimen.qs_background_elevation).toFloat()

                        bgColor = context.getColorAttr(android.R.attr.colorBackgroundFloating)

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
                                    ownResources.getDimensionPixelSize(R.dimen.quick_qs_offset_height))
                        }, 1)

                        qsContainer.addView(View(context).apply {
                            id = R.id.quick_settings_gradient_view
                            background = ownResources.getDrawable(R.drawable.qs_bg_gradient)
                            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                    ownResources.getDimensionPixelSize(R.dimen.qs_gradient_height)).apply {
                                topMargin = ownResources.getDimensionPixelSize(R.dimen.quick_qs_offset_height)
                            }
                        }, 2)

                        (XposedHelpers.getObjectField(param.thisObject, "mQSPanel") as View).apply {
                            elevation = qsElevation
                            setMargins(this)
                            (layoutParams as FrameLayout.LayoutParams).apply {
                                topMargin = ownResources.getDimensionPixelSize(R.dimen.quick_qs_offset_height)
                                bottomMargin = ownResources.getDimensionPixelSize(R.dimen.qs_footer_height)
                            }
                        }

                        (XposedHelpers.getObjectField(param.thisObject, "mQSFooter") as ViewGroup).apply {
                            elevation = qsElevation
                            setMargins(this)
                            findViewById<View>(context.resources.getIdentifier(
                                    "expand_indicator", "id", MainHook.PACKAGE_SYSTEMUI)).visibility = View.GONE

                            val actionsContainer = (getChildAt(0) as ViewGroup).run { getChildAt(childCount - 1) }
                            (actionsContainer.layoutParams as MarginLayoutParams).topMargin =
                                    resUtils.getDimensionPixelSize(R.dimen.settings_container_top_margin)
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
                            if (parent is HorizontalScrollView) {
                                val scrollView = parent as View
                                val scrollContainer = scrollView.parent as View
                                (scrollContainer.layoutParams as MarginLayoutParams).topMargin = 0
                            }
                            (layoutParams as ViewGroup.MarginLayoutParams).topMargin = ownResources
                                    .getDimensionPixelSize(R.dimen.quick_qs_top_margin)
                        }

                        // Swap CarrierText with DateView
                        val carrierText = qsContainer.findViewById<View>(context.resources.getIdentifier(
                                "qs_carrier_text", "id", MainHook.PACKAGE_SYSTEMUI))
                        (carrierText.parent as ViewGroup).removeView(carrierText)

                        val datePaddings = ownResources.getDimensionPixelSize(R.dimen.quick_qs_date_padding)
                        val date = qsContainer.findViewById<TextView>(context.resources.getIdentifier(
                                "date", "id", MainHook.PACKAGE_SYSTEMUI))
                        date.setTextColor(Color.WHITE)
                        date.setPadding(datePaddings, datePaddings, datePaddings, datePaddings)
                        (date.parent as ViewGroup).removeView(carrierText)
                        (date.parent as ViewGroup).removeView(date)
                        (date.layoutParams as LinearLayout.LayoutParams).apply {
                            width = 0
                            weight = 1f
                        }

                        val mobileSignalGroup = MobileSignalGroup(context, classLoader)
                        mobileSignalGroup.id = r_mobile_signal_group
                        mobileSignalGroup.layoutParams = MarginLayoutParams(
                                MarginLayoutParams.WRAP_CONTENT,
                                MarginLayoutParams.WRAP_CONTENT).apply {
                            marginEnd =  mobileSignalGroup.resUtils.getDimensionPixelSize(R.dimen.qs_footer_mobile_group_margin_end)
                        }

                        val tooltip = qsContainer.findViewById<ViewGroup>(r_quick_qs_tooltip)
                        val footerLayout = qsContainer.findViewById<ViewGroup>(r_quick_qs_footer_layout)
                        val footerLeft = footerLayout.getChildAt(0) as ViewGroup
                        footerLeft.moveChildsTo(tooltip)
                        footerLeft.addView(mobileSignalGroup)
                        footerLeft.addView(carrierText)
                        footerLeft.background = null
                        footerLeft.setOnClickListener(null)

                        qsContainer.findViewById<ViewGroup>(r_quick_qs_system_icons)
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
                        val headerLayout = header as? RelativeLayout ?: header.getChildAt(1) as? RelativeLayout ?: header as FrameLayout
                        val context = header.context

                        val quickStatusBarIcons = context.resources.getIdentifier(
                                "quick_status_bar_icons", "id", MainHook.PACKAGE_SYSTEMUI)
                        headerLayout.findViewById<View?>(quickStatusBarIcons)?.apply {
                            headerLayout.removeViewAt(0)
                        }
                        val systemIcons = headerLayout.getChildAt(0) as ViewGroup
                        val battery = systemIcons.findViewById<View>(context.resources.getIdentifier(
                                "battery", "id", MainHook.PACKAGE_SYSTEMUI))
                        val foreground = Color.WHITE
                        val background = 0x4DFFFFFF
                        try {
                            XposedHelpers.callMethod(XposedHelpers.getObjectField(battery, "mDrawable"), "setColors", foreground, background)
                            XposedHelpers.callMethod(battery, "setTextColor", foreground)
                        } catch (ignored : Throwable) {

                        }

                        systemIcons.layoutParams.height = qsHeaderSystemIconsAreaHeight

                        // Move clock to left side
                        systemIcons.findViewById<TextView?>(context.resources.getIdentifier(
                                "clock", "id", MainHook.PACKAGE_SYSTEMUI))?.apply {
                            setTextColor(Color.WHITE)
                            systemIcons.removeView(this)
                            systemIcons.addView(this, 0)
                            // Swap clock padding too
                            setPadding(paddingRight, paddingTop, paddingLeft, paddingBottom)
                        }

                        systemIcons.findViewById<TextView?>(context.resources.getIdentifier(
                                "qs_clock", "id", MainHook.PACKAGE_SYSTEMUI))?.apply {
                            setTextColor(Color.WHITE)
                            systemIcons.removeView(this)
                            systemIcons.addView(this, 0)
                            // Swap clock padding too
                            setPadding(paddingRight, paddingTop, paddingLeft, paddingBottom)
                        }

                        systemIcons.findViewById<TextView?>(context.resources.getIdentifier(
                                "left_clock", "id", MainHook.PACKAGE_SYSTEMUI))?.apply {
                            setTextColor(Color.WHITE)
                            systemIcons.removeView(this)
                            systemIcons.addView(this, 0)
                        }

                        systemIcons.findViewById<TextView?>(context.resources.getIdentifier(
                                "qs_left_clock", "id", MainHook.PACKAGE_SYSTEMUI))?.apply {
                            setTextColor(Color.WHITE)
                            systemIcons.removeView(this)
                            systemIcons.addView(this, 0)
                        }

                        systemIcons.id = r_quick_qs_system_icons

                        val quickQsStatusIcons = LinearLayout(context)
                        quickQsStatusIcons.id = r_quick_qs_status_icons

                        val tooltip = LinearLayout(context)
                        tooltip.id = r_quick_qs_tooltip

                        val ownResources = ResourceUtils.getInstance(context)
                        if (headerLayout is RelativeLayout) {
                            quickQsStatusIcons.layoutParams = RelativeLayout.LayoutParams(
                                    RelativeLayout.LayoutParams.MATCH_PARENT,
                                    ownResources.getDimensionPixelSize(R.dimen.qs_status_icons_height)).apply {
                                addRule(RelativeLayout.BELOW, r_quick_qs_system_icons)
                                topMargin = ownResources.getDimensionPixelSize(R.dimen.qs_status_icons_margin_top)
                                bottomMargin = ownResources.getDimensionPixelSize(R.dimen.qs_status_icons_margin_bottom)
                            }
                            tooltip.layoutParams = RelativeLayout.LayoutParams(
                                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                                    ownResources.getDimensionPixelSize(R.dimen.qs_header_tooltip_height)).apply {
                                addRule(RelativeLayout.BELOW, r_quick_qs_system_icons)
                                addRule(RelativeLayout.CENTER_HORIZONTAL)
                                topMargin = ownResources.getDimensionPixelSize(R.dimen.qs_header_tooltip_margin_top)
                            }
                        } else {
                            quickQsStatusIcons.layoutParams = FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    ownResources.getDimensionPixelSize(R.dimen.qs_status_icons_height)).apply {
                                topMargin = ownResources.getDimensionPixelSize(R.dimen.qs_status_icons_margin_top) +
                                        systemIcons.layoutParams.height
                                bottomMargin = ownResources.getDimensionPixelSize(R.dimen.qs_status_icons_margin_bottom)
                            }
                            tooltip.layoutParams = FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.WRAP_CONTENT,
                                    ownResources.getDimensionPixelSize(R.dimen.qs_header_tooltip_height)).apply {
                                topMargin = ownResources.getDimensionPixelSize(R.dimen.qs_header_tooltip_margin_top) +
                                        systemIcons.layoutParams.height
                                gravity = Gravity.CENTER_HORIZONTAL
                            }
                        }
                        headerLayout.addView(tooltip, 0)
                        headerLayout.addView(quickQsStatusIcons, 0)

                        val statusIcons = StatusIconContainer(context, lpparam.classLoader)
                        statusIcons.layoutParams = LinearLayout.LayoutParams(
                                0, LinearLayout.LayoutParams.MATCH_PARENT).apply { weight = 1f }
                        quickQsStatusIcons.addView(statusIcons)

                        val ctw = ContextThemeWrapper(context, context.resources.getIdentifier(
                                "Theme.SystemUI", "style", MainHook.PACKAGE_SYSTEMUI))
                        val signalCluster = LayoutInflater.from(ctw).inflate(context.resources.getIdentifier(
                                "signal_cluster_view", "layout", MainHook.PACKAGE_SYSTEMUI),
                                quickQsStatusIcons, false)
                        signalCluster.id = r_signal_cluster
                        signalCluster.layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.MATCH_PARENT).apply {
                            marginStart = ownResources.getDimensionPixelSize(R.dimen.signal_cluster_margin_start)
                        }
                        quickQsStatusIcons.addView(signalCluster)

                        val fillColor = fillColorForIntensity(intensity, context)

                        val applyDarkness = XposedHelpers.findMethodExact(classQuickStatusBarHeader, "applyDarkness",
                                Int::class.java, Rect::class.java, Float::class.java, Int::class.java)
                        applyDarkness.invoke(header, r_signal_cluster, Rect(), intensity, fillColor)

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
                    val quickQsStatusIcons = header.findViewById<View>(r_quick_qs_status_icons)
                            ?: return
                    val tooltip = header.findViewById<View>(r_quick_qs_tooltip)
                    val addFloat = XposedHelpers.findMethodExact(classTouchAnimatorBuilder, "addFloat",
                            Object::class.java, String::class.java, FloatArray::class.java)
                    val alphas = FloatArray(2)
                    alphas[0] = 1f
                    alphas[1] = 0f
                    val alphasInverted = FloatArray(2)
                    alphasInverted[0] = 0f
                    alphasInverted[1] = 1f
                    addFloat.invoke(this, quickQsStatusIcons, "alpha", alphas)
                    addFloat.invoke(this, tooltip, "alpha", alphasInverted)
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
                if (param.args[0] as Int != r_signal_cluster)
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
                            val qsDragHandle = footer.findViewById<View>(R.id.qs_drag_handle_view)
                            val mobileSignalGroup = footer.findViewById<View>(r_mobile_signal_group)
                            val addFloat = XposedHelpers.findMethodExact(classTouchAnimatorBuilder, "addFloat",
                                    Object::class.java, String::class.java, FloatArray::class.java)
                            val alphas = FloatArray(2, { it.toFloat() })
                            alphas[0] = 0f
                            alphas[1] = 1f
                            val alphasInverted = FloatArray(2, { it.toFloat() })
                            alphasInverted[0] = 1f
                            alphasInverted[1] = 0f
                            addFloat.invoke(this, mEdit, "alpha", alphas)
                            addFloat.invoke(this, mMultiUserSwitch, "alpha", alphas)
                            addFloat.invoke(this, mSettingsContainer, "alpha", alphas)
                            if (carrierText != null)
                                addFloat.invoke(this, carrierText, "alpha", alphas)
                            if (qsDragHandle != null)
                                addFloat.invoke(this, qsDragHandle, "alpha", alphasInverted)
                            if (mobileSignalGroup != null)
                                addFloat.invoke(this, mobileSignalGroup, "alpha", alphas)
                            param.result = XposedHelpers.callMethod(this, "build")
                        }
                    }
                })

        var alarmShowing = false
        XposedBridge.hookAllMethods(footerClass, "onNextAlarmChanged", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                alarmShowing = XposedHelpers.getBooleanField(param.thisObject, "mAlarmShowing")
                XposedHelpers.setBooleanField(param.thisObject, "mAlarmShowing", false)
            }
        })

        findAndHookMethod(footerClass, "updateAnimator", Int::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val animator = XposedHelpers.getObjectField(param.thisObject, "mAnimator")
                if (animator != null) {
                    XposedHelpers.callMethod(animator, "setPosition", 1f)
                }
                XposedHelpers.setObjectField(param.thisObject, "mAnimator", null)
            }
        })

        findAndHookMethod(footerClass, "updateAlarmVisibilities", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val alarm = XposedHelpers.getObjectField(param.thisObject, "mAlarmStatus") as TextView
                val alarmCollapsed = XposedHelpers.getObjectField(param.thisObject, "mAlarmStatusCollapsed") as View
                val visibility = if (alarmShowing) View.VISIBLE else View.GONE
                alarm.textSize = 12f
                alarm.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                alarm.alpha = 1f
                alarm.visibility = visibility
                alarmCollapsed.visibility = visibility
                param.result = null
            }
        })

        findAndHookMethod(footerClass, "updateVisibilities",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val mSettingsButton = XposedHelpers.getObjectField(param.thisObject, "mSettingsButton") as View
                        val mExpanded = XposedHelpers.getBooleanField(param.thisObject, "mExpanded")
                        mSettingsButton.visibility = if (mExpanded) View.VISIBLE else View.GONE
                    }
                })

        findAndHookMethod(footerClass, "onFinishInflate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val footer = param.thisObject as ViewGroup
                        val context = footer.context

                        footer.layoutParams.height = footer.resUtils.getDimensionPixelSize(R.dimen.qs_footer_height)

                        val layout = LinearLayout(context)
                        layout.id = r_quick_qs_footer_layout
                        layout.layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT)

                        val alarmGroup = footer.findViewById<View>(context.resources.getIdSystemUi("date_time_alarm_group"))
                        alarmGroup.apply {
                            footer.removeView(this)
                            layout.addView(this)
                            layoutParams = LinearLayout.LayoutParams(layoutParams as ViewGroup.MarginLayoutParams).apply {
                                width = 0
                                weight = 1f
                            }
                        }

                        LayoutInflater.from(ResourceUtils.getInstance(context).context)
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

        findAndHookMethod(footerClass, "setListening", Boolean::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val footer = param.thisObject as ViewGroup
                val mobileSignalGroup = footer.findViewById<View>(r_mobile_signal_group) as? MobileSignalGroup
                if (mobileSignalGroup != null) {
                    mobileSignalGroup.listening = param.args[0] as Boolean
                } else {
                    MainHook.logE("QuickSettingsHook", "mobile signal group is null")
                }
            }
        })

        XposedBridge.hookAllMethods(classCellularTile, "handleUpdateState", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val state = param.args[0]
                val cb = param.args[1] ?: param.thisObject.objField("mSignalCallback").objField("mInfo")
                val value = XposedHelpers.getBooleanField(state, "value")
                val context = param.thisObject.field<Context>("mContext")
                state.setIntField("state", if (value) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE)
                if (cb.field("noSim")) {
                    state.setField("state", Tile.STATE_UNAVAILABLE)
                } else {
                    state.setField("icon", getIcon(context, param.thisObject))
                    if (cb.field("airplaneModeEnabled")) {
                        state.setIntField("state", Tile.STATE_UNAVAILABLE)
                        state.setAdditionalField("secondaryLabel", context.resources.getStringSystemUi("status_bar_airplane"))
                    } else if (value) {
                        state.setIntField("state", Tile.STATE_ACTIVE)
                        state.setAdditionalField("secondaryLabel", getMobileDataDescription(context, cb, value))
                    } else {
                        state.setIntField("state", Tile.STATE_INACTIVE)
                        state.setAdditionalField("secondaryLabel", context.resources.getStringSystemUi("switch_bar_off"))
                    }
                }
            }

            fun getMobileDataDescription(context: Context, cb: Any, enabled: Boolean): CharSequence? {
                val dataContentDescription = cb.field<String>("dataContentDescription")
                val roaming = cb.field<Boolean>("roaming")
                val roamingString = context.resources.getStringSystemUi("accessibility_data_connection_roaming")
                return if (roaming && !TextUtils.isEmpty(dataContentDescription)) {
                    String.format("%s — %s", roamingString, dataContentDescription)
                } else if (roaming) {
                    roamingString
                } else if (enabled) {
                    dataContentDescription
                } else {
                    null
                }
            }

            fun getIcon(context: Context, tile: Any): Any? {
                var icon = tile.additionalField<Any?>("icon")
                if (icon == null) {
                    icon = classDrawableIcon.newInstance(context.resUtils.getDrawable(R.drawable.ic_swap_vert))
                    tile.setAdditionalField("icon", icon)
                }
                return icon
            }
        })

        XposedHelpers.findAndHookMethod(classCellularTile, "createTileView", Context::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val context = param.args[0]
                param.result = XposedHelpers.newInstance(classQSIconViewImpl, context)
            }
        })

        XposedHelpers.findAndHookMethod(classQSTileView, "createLabel", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val labelContainer = param.thisObject.field<ViewGroup>("mLabelContainer")
                val context = labelContainer.context
                val secondLine = labelContainer.findViewById<View>(context.resources.getIdSystemUi("app_label"))
                secondLine.alpha = 0.6f
                param.thisObject.setAdditionalField("secondLine", secondLine)
                val label = param.thisObject.field<TextView>("mLabel")
                label.setTypeface(Typeface.DEFAULT, Typeface.NORMAL)
            }
        })

        XposedBridge.hookAllMethods(classQSTileView, "handleStateChanged", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val state = param.args[0]
                val secondLine = param.thisObject.additionalField<TextView?>("secondLine")
                if (secondLine != null) {
                    val secondaryLabel = state.additionalField<String?>("secondaryLabel")
                    if (secondaryLabel != secondLine.text) {
                        secondLine.text = secondaryLabel
                        secondLine.visibility = if (TextUtils.isEmpty(secondaryLabel)) View.GONE else View.VISIBLE
                    }
                }
            }
        })

        XposedHelpers.findAndHookMethod(classQSTileState, "copyTo", classQSTileState, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val oldState = param.thisObject
                val newState = param.args[0]
                newState.setAdditionalField("secondaryLabel", oldState.additionalField("secondaryLabel"))
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
                    val ownResources = ResourceUtils.getInstance(context)

                    ImageView(context).apply {
                        id = r_qs_tile_background
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        setImageDrawable(ownResources.getDrawable(R.drawable.ic_qs_circle))
                        iconFrame.addView(this, 0)
                    }

                    iconFrame.clipChildren = false
                    iconFrame.clipToPadding = false

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
                    val bg = tileBaseView.findViewById<ImageView>(r_qs_tile_background)

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
                        param.result = bgColor
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

            findAndHookConstructor(classSignalTileView, Context::class.java, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    (param.thisObject as ViewGroup).apply {
                        clipChildren = false
                        clipToPadding = false
                    }
                }
            })

            findAndHookMethod(classSignalTileView, "createIcon",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val mOverlay = XposedHelpers.getObjectField(param.thisObject, "mOverlay") as ImageView
                            mOverlay.imageTintList = ColorStateList.valueOf(Color.WHITE)
                        }
                    })

            findAndHookMethod(classSignalTileView, "layoutIndicator",
                    View::class.java, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    (param.thisObject as ViewGroup).apply {
                        val indicator = param.args[0] as ImageView
                        indicator.imageTintList = ColorStateList.valueOf(indicator.context.getColorAccent())

                        val mIconFrame = XposedHelpers.getObjectField(this, "mIconFrame") as View
                        val isRtl = layoutDirection == View.LAYOUT_DIRECTION_RTL
                        val left: Int
                        val right: Int
                        if (isRtl) {
                            right = getLeft() - signalIndicatorToIconFrameSpacing
                            left = right - indicator.measuredWidth
                        } else {
                            left = getRight() + signalIndicatorToIconFrameSpacing
                            right = left + indicator.measuredWidth
                        }
                        indicator.layout(
                                left,
                                mIconFrame.bottom - indicator.measuredHeight,
                                right,
                                mIconFrame.bottom)
                    }
                    param.result = null
                }
            })

            findAndHookConstructor(classCellTileView, Context::class.java, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val drawable = XposedHelpers.getObjectField(param.thisObject, "mSignalDrawable")
                    XposedHelpers.callMethod(drawable, "setColors",
                            0x4dffffff, Color.WHITE)
                }
            })

            findAndHookMethod(classSignalIcon, "getDrawable",
                    Context::class.java, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val drawable = param.result
                    XposedHelpers.callMethod(drawable, "setColors",
                            0x4dffffff, Color.WHITE)
                }
            })

            XposedBridge.hookAllMethods(classPageIndicator, "setNumPages", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val indicator = param.thisObject as ViewGroup
                    val accentColor = indicator.context.getColorAccent()
                    for (i in (0 until indicator.childCount)) {
                        val child = indicator.getChildAt(i)
                        if (child is ImageView) {
                            child.imageTintList = ColorStateList.valueOf(accentColor)
                        }
                    }
                }
            })

            XposedBridge.hookAllMethods(classRotationLockTile, "handleUpdateState", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val state = param.args[0]
                    val context = param.thisObject.field<Context>("mContext")
                    val locked = state.field<Boolean>("value")
                    if (!locked) {
                        state.setField("label", context.getString(context.resources.getIdentifier(
                                "quick_settings_rotation_unlocked_label", "string", MainHook.PACKAGE_SYSTEMUI)))
                    }
                    state.setField("icon", getIcon(context, param.thisObject))
                }

                fun getIcon(context: Context, tile: Any): Any? {
                    var icon = tile.additionalField<Any?>("icon")
                    if (icon == null) {
                        icon = classResourceIcon.newInstance(context.resources.getIdentifier(
                                "ic_portrait_from_auto_rotate", "drawable", MainHook.PACKAGE_SYSTEMUI))
                        tile.setAdditionalField("icon", icon)
                    }
                    return icon
                }
            })

            XposedBridge.hookAllMethods(classBluetoothTile, "handleUpdateState", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val state = param.args[0]
                    state.objField("icon")?.setAdditionalField("tint", true)
                }
            })

            findAndHookMethod(classBluetoothBatteryMeterDrawable, "getDrawable",
                    Context::class.java, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (param.thisObject.additionalField<Any?>("tint") != null) {
                        val drawable = param.result as Drawable
                        val context = param.args[0] as Context
                        val tintColor = context.getColorAttr(android.R.attr.colorBackgroundFloating)
                        drawable.colorFilter = PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_IN)
                    }
                }
            })
        }

        if (ConfigUtils.notifications.qsVerticalScroll || ConfigUtils.notifications.swapQsAndBrightness) {
            findAndHookConstructor(classQSPanel, Context::class.java, AttributeSet::class.java,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val qsPanel = param.thisObject as ViewGroup
                            val context = qsPanel.context

                            val brightnessView = XposedHelpers.getObjectField(qsPanel, "mBrightnessView") as View
                            val tileLayout = XposedHelpers.getObjectField(qsPanel, "mTileLayout") as View

                            val swap = ConfigUtils.notifications.swapQsAndBrightness
                            val brightnessIndex = qsPanel.indexOfChild(brightnessView)
                            val tileLayoutIndex = qsPanel.indexOfChild(tileLayout)
                            val minIndex = min(brightnessIndex, tileLayoutIndex)
                            val maxIndex = max(brightnessIndex, tileLayoutIndex)
                            val brightnessFirst = with(brightnessIndex < tileLayoutIndex) {
                                if (swap) !this else this
                            }

                            qsPanel.removeViewAt(maxIndex)
                            qsPanel.removeViewAt(minIndex)

                            if (ConfigUtils.notifications.qsVerticalScroll) {
                                val views = if (brightnessFirst)
                                    arrayOf(brightnessView, tileLayout)
                                else
                                    arrayOf(tileLayout, brightnessView)
                                tileLayout.isFocusable = false
                                val scrollLayout = QSScrollLayout(context, *views)
                                scrollLayout.layoutParams = LinearLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT, 0).apply { weight = 1f }
                                scrollLayout.id = r_qs_scroll_layout
                                qsPanel.addView(scrollLayout, minIndex)
                            } else {
                                qsPanel.addView(if (brightnessFirst) brightnessView else tileLayout, minIndex)
                                qsPanel.addView(if (brightnessFirst) tileLayout else brightnessView, maxIndex)
                            }

                            (qsPanel.getChildAt(0).layoutParams as MarginLayoutParams)
                                    .topMargin = qsPanel.resUtils.getDimensionPixelSize(R.dimen.qs_panel_padding_top)
                        }
                    })
        } else {
            findAndHookConstructor(classQSPanel, Context::class.java, AttributeSet::class.java,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val qsPanel = param.thisObject as ViewGroup
                            (qsPanel.getChildAt(0).layoutParams as MarginLayoutParams)
                                    .topMargin = qsPanel.resUtils.getDimensionPixelSize(R.dimen.qs_panel_padding_top)
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

            findAndHookMethod(classQSPanel, "setExpanded",
                    Boolean::class.java, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val qsPanel = param.thisObject as ViewGroup
                    val mExpanded = XposedHelpers.getBooleanField(qsPanel, "mExpanded")
                    val expanded = param.args[0] as Boolean
                    if (mExpanded != expanded && !mExpanded) {
                        val scrollLayout = qsPanel.findViewById<QSScrollLayout>(r_qs_scroll_layout)
                        scrollLayout.scrollY = 0
                    }
                }
            })

            findAndHookMethod(classQSPanel, "updateResources",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            logThrowable("QuickSettingsHook", "Uncaught throwable in updateResources") {
//                                qsPanelUpdateResources(param.thisObject as ViewGroup)
                            }
                        }
                    })

            findAndHookConstructor(classQSAnimator, classQS, classQuickQSPanel, classQSPanel,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val qsPanel = param.args[2] as ViewGroup
                            val scrollLayout = qsPanel.findViewById<QSScrollLayout>(r_qs_scroll_layout)
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
                                qsNotifCollapsedSpace
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
                    val mScrollLayout = qsPanel.findViewById<QSScrollLayout>(r_qs_scroll_layout)
                    val shouldIntercept = mScrollLayout.shouldIntercept(event)
                    param.result = mExpanded && mScrollLayout.shouldIntercept(event)
//                logI("onInterceptTouchEvent -> ${event.action}, $mExpanded && $shouldIntercept")
                }
            })
        }

        findAndHookMethod(classBatteryMeterView, "updateShowPercent",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val layout = param.thisObject as ViewGroup
                        if (layout.childCount == 2 && layout.getChildAt(0) is TextView) {
                            val text = layout.getChildAt(0) as TextView
                            text.setPadding(text.paddingRight, text.paddingTop, text.paddingLeft, text.paddingBottom)
                            layout.removeViewAt(0)
                            layout.addView(text, 1)
                        }
                    }
                })
    }

    fun qsPanelUpdateResources(qsPanel: ViewGroup) {
        val brightnessView = XposedHelpers.getObjectField(qsPanel, "mBrightnessView") as View

        brightnessView.setPadding(
                brightnessView.paddingLeft,
                brightnessView.resources.getDimenSystemUi("qs_brightness_padding_top"),
                brightnessView.paddingRight,
                brightnessView.paddingBottom)

        MainHook.logD("QSHook", "padding b: ${qsPanel.paddingLeft}, ${qsPanel.paddingTop}, ${qsPanel.paddingRight}, ${qsPanel.paddingBottom}")

        qsPanel.setPadding(
                0, 69,
                0, qsPanel.paddingBottom)

        MainHook.logD("QSHook", "padding a: ${qsPanel.paddingLeft}, ${qsPanel.paddingTop}, ${qsPanel.paddingRight}, ${qsPanel.paddingBottom}")
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
        return ResourceUtils.getInstance(context)
                .getDrawable(R.drawable.qs_background_primary, context.theme)
    }

    var r_quick_qs_system_icons = R.id.quick_qs_system_icons
    var r_quick_qs_status_icons = R.id.quick_qs_status_icons
    var r_signal_cluster = R.id.signal_cluster
    var r_quick_qs_footer_layout = R.id.quick_qs_footer_layout
    var r_qs_tile_background = R.id.qs_tile_background
    var r_qs_scroll_layout = R.id.qs_scroll_layout
    var r_quick_qs_tooltip = R.id.quick_qs_tooltip
    var r_mobile_signal_group = R.id.mobile_signal_group

    override fun handleInitPackageResources(resparam: XC_InitPackageResources.InitPackageResourcesParam) {
        if (resparam.packageName != MainHook.PACKAGE_SYSTEMUI) return
        if (!ConfigUtils.notifications.changePullDown) return

        r_quick_qs_system_icons = XResources.getFakeResId(MainHook.modRes, R.id.quick_qs_system_icons)
        r_quick_qs_status_icons = XResources.getFakeResId(MainHook.modRes, R.id.quick_qs_status_icons)
        r_signal_cluster = XResources.getFakeResId(MainHook.modRes, R.id.signal_cluster)
        r_quick_qs_footer_layout = XResources.getFakeResId(MainHook.modRes, R.id.quick_qs_footer_layout)
        r_qs_tile_background = XResources.getFakeResId(MainHook.modRes, R.id.qs_tile_background)
        r_qs_scroll_layout = XResources.getFakeResId(MainHook.modRes, R.id.qs_scroll_layout)
        r_quick_qs_tooltip = XResources.getFakeResId(MainHook.modRes, R.id.quick_qs_tooltip)
        r_mobile_signal_group = XResources.getFakeResId(MainHook.modRes, R.id.mobile_signal_group)

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
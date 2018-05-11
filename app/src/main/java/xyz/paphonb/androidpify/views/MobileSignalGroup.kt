package xyz.paphonb.androidpify.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import de.robv.android.xposed.XposedHelpers
import xyz.paphonb.androidpify.utils.getColorAttr
import xyz.paphonb.androidpify.utils.getIdSystemUi
import xyz.paphonb.androidpify.utils.getLayoutSystemUi
import xyz.paphonb.androidpify.utils.moveChildsTo
import java.lang.reflect.Proxy

class MobileSignalGroup(context: Context, classLoader: ClassLoader) : LinearLayout(context) {

    private val classDependency = XposedHelpers.findClass("com.android.systemui.Dependency", classLoader)!!
    private val classNetworkController = XposedHelpers.findClass(CLASS_NETWORK_CONTROLLER, classLoader)!!
    private val classSignalCallback = XposedHelpers.findClass("$CLASS_NETWORK_CONTROLLER\$SignalCallback", classLoader)!!
    private val classSignalDrawable = XposedHelpers.findClass("com.android.systemui.statusbar.phone.SignalDrawable", classLoader)!!

    private val colorForeground = context.getColorAttr(android.R.attr.colorForeground)

    private val mobileRoaming by lazy { findViewById<ImageView>(resources.getIdSystemUi("mobile_roaming")) }
    private val mobileSignal by lazy { findViewById<ImageView>(resources.getIdSystemUi("mobile_signal")) }

    var listening = false
        set(value) {
            if (value != field) {
                field = value
                updateListeners()
            }
        }

    private var visible = true
    private var mobileSignalIconId = 0
    private var roaming = false

    init {
        val tmp = LayoutInflater.from(context)
                .inflate(resources.getLayoutSystemUi("mobile_signal_group"), this, false) as ViewGroup
        tmp.moveChildsTo(this)

    }

    fun updateListeners() {
        val networkController = XposedHelpers.callStaticMethod(classDependency, "get", classNetworkController)
        if (listening) {
            if (XposedHelpers.callMethod(networkController, "hasVoiceCallingFeature") as Boolean) {
                XposedHelpers.callMethod(networkController, "addCallback", signalCallback)
            }
        } else {
            XposedHelpers.callMethod(networkController, "removeCallback", signalCallback)
        }
    }

    private fun setMobileDataIndicators(statusIcon: Any, roaming: Boolean) {
        visible = XposedHelpers.getBooleanField(statusIcon, "visible")
        mobileSignalIconId = XposedHelpers.getIntField(statusIcon, "icon")
        this.roaming = roaming
        handleUpdateState()
    }

    private fun setNoSims(show: Boolean) {
        if (show) {
            visible = false
        }
        handleUpdateState()
    }

    private fun handleUpdateState() {
        visibility = if (visible) View.VISIBLE else View.GONE
        if (visible) {
            mobileRoaming.visibility = if (roaming) View.VISIBLE else View.GONE
            mobileRoaming.imageTintList = ColorStateList.valueOf(colorForeground)
            val signalDrawable = XposedHelpers.newInstance(classSignalDrawable, context) as Drawable
            XposedHelpers.callMethod(signalDrawable, "setDarkIntensity", getColorIntensity(colorForeground))
            mobileSignal.setImageDrawable(signalDrawable)
            mobileSignal.setImageLevel(mobileSignalIconId)
        }
    }

    private fun getColorIntensity(color: Int): Float {
        return if (color == -1) 0.0f else 1.0f
    }

    private val signalCallback = Proxy.newProxyInstance(classLoader, arrayOf(classSignalCallback),
            { proxy, method, args -> when (method.name) {
                "setMobileDataIndicators" -> setMobileDataIndicators(args[0], args[10] as Boolean)
                "setNoSims" -> setNoSims(args[0] as Boolean)
                "equals" -> proxy === args[0]
                else -> null
            } })

    companion object {
        const val CLASS_NETWORK_CONTROLLER = "com.android.systemui.statusbar.policy.NetworkController"
    }
}
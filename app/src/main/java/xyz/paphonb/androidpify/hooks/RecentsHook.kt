package xyz.paphonb.androidpify.hooks

import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import xyz.paphonb.androidpify.MainHook
import xyz.paphonb.androidpify.aosp.OverviewProxyService
import xyz.paphonb.androidpify.utils.ConfigUtils

@SuppressLint("StaticFieldLeak")
object RecentsHook : IXposedHookLoadPackage {

    const val TAG = "RecentsHook"

    val systemUiApp: Application get() = systemUiAppInternal!!
    var systemUiAppInternal: Application? = null

    val overviewProxyService by lazy { OverviewProxyService(systemUiApp) }

    fun onCreate(app: Application) {
        if (!ConfigUtils.misc.proxyOverview) return
        systemUiAppInternal = app

        app.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                overviewProxyService.startConnectionToCurrentUser()
            }
        }, IntentFilter(Intent.ACTION_USER_UNLOCKED))
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != MainHook.PACKAGE_SYSTEMUI) return
        if (!ConfigUtils.misc.proxyOverview) return

        val recents = XposedHelpers.findClass("com.android.systemui.recents.Recents", lpparam.classLoader)
        XposedHelpers.findAndHookMethod(recents, "showRecentApps", Boolean::class.java, Boolean::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        overviewProxyService.proxy?.run {
                            onOverviewShown(param.args[0] as Boolean)
                            param.result = null
                        }
                    }
                })

        XposedHelpers.findAndHookMethod(recents, "hideRecentApps", Boolean::class.java, Boolean::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        overviewProxyService.proxy?.run {
                            onOverviewHidden(param.args[0] as Boolean, param.args[1] as Boolean)
                            param.result = null
                        }
                    }
                })

        XposedHelpers.findAndHookMethod(recents, "toggleRecentApps",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        overviewProxyService.proxy?.run {
                            onOverviewToggle()
                            param.result = null
                        }
                    }
                })
    }
}
package xyz.paphonb.androidpify.hooks

import android.graphics.Rect
import android.graphics.drawable.RippleDrawable
import android.view.animation.DecelerateInterpolator
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

object RippleHook : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val classRippleForeground = XposedHelpers.findClass("android.graphics.drawable.RippleForeground", lpparam.classLoader)
        XposedHelpers.setStaticObjectField(classRippleForeground, "LINEAR_INTERPOLATOR", DecelerateInterpolator(5f))
        XposedHelpers.findAndHookConstructor(classRippleForeground, RippleDrawable::class.java, Rect::class.java,
                Float::class.java, Float::class.java, Boolean::class.java, Boolean::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                param.args[4] = false
            }
        })
    }
}

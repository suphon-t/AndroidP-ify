package xyz.paphonb.androidpify.hooks

import android.app.ActivityManager
import android.app.ActivityManager.RecentTaskInfo
import android.app.ActivityManager.TaskDescription
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.RemoteException
import android.view.View
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import xyz.paphonb.androidpify.MainHook
import xyz.paphonb.androidpify.utils.ConfigUtils
import java.util.*
import kotlin.collections.HashSet

object LauncherHook : IXposedHookLoadPackage {

    val launcherPackages = HashSet<String>().apply {
        add(MainHook.PACKAGE_LAUNCHER)
        add(MainHook.PACKAGE_OP_LAUNCHER)
        add(MainHook.PACKAGE_LAWNCHAIR)
    }
    private val usingOldLauncher3 by lazy { ConfigUtils.misc.proxyOverviewPackage == "mpl6" }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (!launcherPackages.contains(lpparam.packageName)) return
        if (!ConfigUtils.misc.proxyOverview) return

        val activityManagerWrapper = XposedHelpers.findClass("com.android.systemui.shared.system.ActivityManagerWrapper", lpparam.classLoader)
        XposedHelpers.findAndHookMethod(activityManagerWrapper, "getRunningTask", Int::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        try {
                            param.result = getRunningTask()
                        } catch (t: Throwable) {
                            MainHook.logE("LauncherHook", "error in getRunningTask", t)
                        }
                    }
                })

        if (usingOldLauncher3) {
            val recentsTaskLoader = XposedHelpers.findClass("com.android.systemui.shared.recents.model.RecentsTaskLoader", lpparam.classLoader)
            val recentsTaskLoadPlan = XposedHelpers.findClass("com.android.systemui.shared.recents.model.RecentsTaskLoadPlan", lpparam.classLoader)
            val preloadOptions = XposedHelpers.findClass("com.android.systemui.shared.recents.model.RecentsTaskLoadPlan\$PreloadOptions", lpparam.classLoader)
            XposedHelpers.findAndHookMethod(recentsTaskLoadPlan, "preloadPlan", preloadOptions, recentsTaskLoader, Int::class.java, Int::class.java,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            preloadPlan(param.thisObject, param.args[0], param.args[1], param.args[2] as Int, param.args[3] as Int)
                            param.result = null
                        }
                    })
        }

        val TaskSnapshot = XposedHelpers.findClass("android.app.ActivityManager\$TaskSnapshot", lpparam.classLoader)
        val thumbnailData = XposedHelpers.findClass("com.android.systemui.shared.recents.model.ThumbnailData", lpparam.classLoader)
        XposedHelpers.findAndHookConstructor(thumbnailData, TaskSnapshot, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val thisObj = param.thisObject
                val taskSnapshot = param.args[0]
                val snapshot = XposedHelpers.callMethod(taskSnapshot, "getSnapshot")
                val thumbnail = XposedHelpers.callStaticMethod(Bitmap::class.java, "createHardwareBitmap", snapshot)
                XposedHelpers.setObjectField(thisObj, "thumbnail", thumbnail)
                XposedHelpers.setObjectField(thisObj, "insets", Rect(XposedHelpers.callMethod(taskSnapshot, "getContentInsets") as Rect?))
                XposedHelpers.setIntField(thisObj, "orientation", XposedHelpers.callMethod(taskSnapshot, "getOrientation") as Int)
                XposedHelpers.setBooleanField(thisObj, "reducedResolution", XposedHelpers.callMethod(taskSnapshot, "isReducedResolution") as Boolean)
                XposedHelpers.setFloatField(thisObj, "scale", XposedHelpers.callMethod(taskSnapshot, "getScale") as Float)
                XposedHelpers.setBooleanField(thisObj, "isRealSnapshot", true)
                param.result = null
            }
        })

        val recentsActivityTracker = XposedHelpers.findClass("com.android.quickstep.RecentsActivityTracker", lpparam.classLoader)
        val remoteAnimationProvider = XposedHelpers.findClass("com.android.quickstep.util.RemoteAnimationProvider", lpparam.classLoader)
        XposedHelpers.findAndHookMethod(recentsActivityTracker, "registerAndStartActivity", Intent::class.java,
                remoteAnimationProvider, Context::class.java, Handler::class.java, Long::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                XposedHelpers.callMethod(param.thisObject, "register")
                val intent = param.args[0] as Intent
                val context = param.args[2] as Context
                context.startActivity(intent)
                param.result = null
            }
        })

        val launcherInitListener = XposedHelpers.findClass("com.android.launcher3.LauncherInitListener", lpparam.classLoader)
        XposedHelpers.findAndHookMethod(launcherInitListener, "registerAndStartActivity", Intent::class.java,
                remoteAnimationProvider, Context::class.java, Handler::class.java, Long::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                XposedHelpers.callMethod(param.thisObject, "register")
                val intent = param.args[0] as Intent
                val context = param.args[2] as Context
                context.startActivity(XposedHelpers.callMethod(param.thisObject, "addToIntent", intent) as Intent)
                param.result = null
            }
        })

        val iconLoader = XposedHelpers.findClass("com.android.systemui.shared.recents.model.IconLoader", lpparam.classLoader)
        val taskKey = XposedHelpers.findClass("com.android.systemui.shared.recents.model.Task\$TaskKey", lpparam.classLoader)
        XposedHelpers.findAndHookMethod(iconLoader, "createNewIconForTask", taskKey,
                ActivityManager.TaskDescription::class.java, Boolean::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                param.result = createNewIconForTask(param.thisObject, param.args[0],
                        param.args[1] as ActivityManager.TaskDescription, param.args[2] as Boolean)
            }
        })

        val activityOptionsCompat = XposedHelpers.findClass("com.android.systemui.shared.system.ActivityOptionsCompat", lpparam.classLoader)
        XposedHelpers.findAndHookMethod(activityOptionsCompat, "makeSplitScreenOptions", Boolean::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                param.result = ActivityOptions.makeBasic()
            }
        })

        val splitScreen = XposedHelpers.findClass("com.android.quickstep.TaskSystemShortcut\$SplitScreen", lpparam.classLoader)
        val abstractFloatingView = XposedHelpers.findClass("com.android.launcher3.AbstractFloatingView", lpparam.classLoader)
        XposedBridge.hookAllMethods(splitScreen, "getOnClickListener", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val listener = param.result
                if (listener != null) {
                    param.result = View.OnClickListener {
                        val baseDraggingActivity = XposedHelpers.getObjectField(param.thisObject, "mActivity")
                        XposedHelpers.callStaticMethod(abstractFloatingView, "closeOpenViews", baseDraggingActivity, true, 399)

                        val taskView = XposedHelpers.getObjectField(param.thisObject, "mTaskView") as View
                        val task = XposedHelpers.getObjectField(taskView, "mTask")
                        val key = XposedHelpers.getObjectField(task, "key")
                        val taskId = XposedHelpers.getObjectField(key, "id")
                        XposedHelpers.callMethod(getAmService(), "moveTaskToDockedStack", taskId, 1,
                                true, true, getViewBounds(taskView))
                        XposedHelpers.callMethod(taskView, "launchTask", false)
                    }
                }
            }
        })

        val recentsActivity = XposedHelpers.findClass("com.android.quickstep.RecentsActivity", lpparam.classLoader)
        XposedHelpers.findAndHookMethod(recentsActivity, "getActivityLaunchOptions",
                View::class.java, Boolean::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val view = param.args[0] as View
                param.result = ActivityOptions.makeClipRevealAnimation(view, 0, 0, view.width, view.height)
            }
        })

        val recentsView = XposedHelpers.findClass("com.android.quickstep.views.RecentsView", lpparam.classLoader)
        XposedHelpers.findAndHookMethod(recentsView, "isRecentsEnabled", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                param.result = true
            }
        })
    }

    fun getViewBounds(source: View): Rect {
        val pts = IntArray(2)
        source.getLocationOnScreen(pts)
        return Rect(pts[0], pts[1], pts[0] + source.width, pts[1] + source.height)
    }

    fun createNewIconForTask(iconLoader: Any, taskKey: Any, taskDescription: ActivityManager.TaskDescription, z: Boolean): Drawable? {
        val i = XposedHelpers.getIntField(taskKey, "userId")
        var inMemoryIcon = XposedHelpers.callMethod(taskDescription, "getInMemoryIcon") as Bitmap?
        if (inMemoryIcon != null) {
            return XposedHelpers.callMethod(iconLoader, "createDrawableFromBitmap", inMemoryIcon, i, taskDescription) as Drawable?
        }
        val iconFilename = XposedHelpers.callMethod(taskDescription, "getIconFilename")
        inMemoryIcon = XposedHelpers.callStaticMethod(TaskDescription::class.java, "loadTaskDescriptionIcon", iconFilename, i) as Bitmap?
        if (inMemoryIcon != null) {
            return XposedHelpers.callMethod(iconLoader, "createDrawableFromBitmap", inMemoryIcon, i, taskDescription) as Drawable?
        }
        var taskKey2 = XposedHelpers.callMethod(iconLoader, "getAndUpdateActivityInfo", taskKey)
        if (taskKey2 != null) {
            taskKey2 = XposedHelpers.callMethod(iconLoader, "getBadgedActivityIcon", taskKey2, i, taskDescription)
            if (taskKey2 != null) {
                return taskKey2 as Drawable?
            }
        }
        return if (z) {
            XposedHelpers.callMethod(iconLoader, "getDefaultIcon", i) as Drawable?
        } else null
    }

    @Suppress("UNCHECKED_CAST")
    fun getRunningTask(): ActivityManager.RunningTaskInfo? {
        return try {
            val tasks = XposedHelpers.callMethod(getAmService(), "getTasks", 10, 0) as List<ActivityManager.RunningTaskInfo>
            if (tasks.isEmpty()) null else tasks[0]
        } catch (e: RemoteException) {
            null
        }
    }

    fun getAmService() = XposedHelpers.callStaticMethod(ActivityManager::class.java, "getService")!!

    fun preloadPlan(loadPlan: Any, preloadOptions: Any, recentsTaskLoader: Any, i: Int, i2: Int) {
        val activityManagerWrapper = XposedHelpers.findClass("com.android.systemui.shared.system.ActivityManagerWrapper", loadPlan::class.java.classLoader)
        val Task = XposedHelpers.findClass("com.android.systemui.shared.recents.model.Task", loadPlan::class.java.classLoader)
        val TaskKey = XposedHelpers.findClass("com.android.systemui.shared.recents.model.Task\$TaskKey", loadPlan::class.java.classLoader)
        val TaskStack = XposedHelpers.findClass("com.android.systemui.shared.recents.model.TaskStack", loadPlan::class.java.classLoader)
        val amwInstance = XposedHelpers.callStaticMethod(activityManagerWrapper, "getInstance")
        val context = XposedHelpers.getObjectField(loadPlan, "mContext") as Context
        context.resources
        val arrayList = ArrayList<Any>()
        var rawTasks = XposedHelpers.getObjectField(loadPlan, "mRawTasks") as List<*>?
        if (rawTasks == null) {
            rawTasks = XposedHelpers.callMethod(amwInstance, "getRecentTasks",
                    XposedHelpers.callStaticMethod(ActivityManager::class.java, "getMaxRecentTasksStatic"), i2) as List<*>
            Collections.reverse(rawTasks)
            XposedHelpers.setObjectField(loadPlan, "mRawTasks", rawTasks)
        }
        var size = rawTasks.size
        var i3 = 0
        while (i3 < size) {
            val recentTaskInfo = rawTasks[i3] as RecentTaskInfo
            val windowingMode = 0 // recentTaskInfo.configuration.windowConfiguration.getWindowingMode()
            val i4 = i3
            val i5 = size
            val taskKeyId = XposedHelpers.getIntField(recentTaskInfo, "id")
            val userId = XposedHelpers.getIntField(recentTaskInfo, "userId")
            val lastActiveTime = XposedHelpers.getLongField(recentTaskInfo, "lastActiveTime")
            val taskKey = XposedHelpers.newInstance(TaskKey, recentTaskInfo.persistentId, windowingMode, recentTaskInfo.baseIntent, userId, lastActiveTime)
            val z: Boolean = windowingMode != 5
            val z2 = taskKeyId == i
            val activityInfo = XposedHelpers.callMethod(recentsTaskLoader, "getAndUpdateActivityInfo", taskKey) as ActivityInfo?
            val excluded = activityInfo?.run { flags and ActivityInfo.FLAG_EXCLUDE_FROM_RECENTS != 0 } ?: false
            if (activityInfo != null && !excluded) {
                val loadTitles = XposedHelpers.getBooleanField(preloadOptions, "loadTitles")
                var activityTitle: String?
                val z3: Boolean
                val activityIcon: Drawable?
                activityTitle = if (loadTitles) {
                    XposedHelpers.callMethod(recentsTaskLoader, "getAndUpdateActivityTitle", taskKey, recentTaskInfo.taskDescription) as String?
                } else {
                    ""
                }
                val str = activityTitle
                activityTitle = if (loadTitles) {
                    XposedHelpers.callMethod(recentsTaskLoader, "getAndUpdateContentDescription", taskKey, recentTaskInfo.taskDescription) as String?
                } else {
                    ""
                }
                val str2 = activityTitle
                if (z) {
                    z3 = false
                    activityIcon = XposedHelpers.callMethod(recentsTaskLoader,
                            "getAndUpdateActivityIcon", taskKey, recentTaskInfo.taskDescription, false) as Drawable?
                } else {
                    z3 = false
                    activityIcon = null
                }
                val drawable = activityIcon
                val thumbnail = XposedHelpers.callMethod(recentsTaskLoader, "getAndUpdateThumbnail", taskKey, z3, z3)
                val activityPrimaryColor = XposedHelpers.callMethod(recentsTaskLoader, "getActivityPrimaryColor", recentTaskInfo.taskDescription)
                val activityBackgroundColor = XposedHelpers.callMethod(recentsTaskLoader, "getActivityBackgroundColor", recentTaskInfo.taskDescription)
                val z4 = activityInfo.applicationInfo.flags and 1 != 0
                val locked = false
                val supportsSplitScreenMultiWindow = XposedHelpers.getBooleanField(recentTaskInfo, "supportsSplitScreenMultiWindow")
                val resizeMode = 2
                val task = XposedHelpers.newInstance(Task, taskKey, drawable, thumbnail, str, str2, activityPrimaryColor,
                        activityBackgroundColor, z2, z, z4, supportsSplitScreenMultiWindow, recentTaskInfo.taskDescription, resizeMode, recentTaskInfo.topActivity, locked)
                arrayList.add(task)
            }
            i3 = i4 + 1
            size = i5
        }
        val stack = XposedHelpers.newInstance(TaskStack)
        XposedHelpers.callMethod(stack, "setTasks", arrayList, false)
        XposedHelpers.setObjectField(loadPlan, "mStack", stack)
    }
}
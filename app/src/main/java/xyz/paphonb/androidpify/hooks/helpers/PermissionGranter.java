/*
 * Copyright (C) 2015 Peter Gregus for GravityBox Project (C3C076@xda)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.paphonb.androidpify.hooks.helpers;

import android.Manifest;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import xyz.paphonb.androidpify.MainHook;
import xyz.paphonb.androidpify.hooks.LauncherHook;

public class PermissionGranter {
    public static final String TAG = "GB:PermissionGranter";
    public static final boolean DEBUG = false;

    private static final String CLASS_PACKAGE_MANAGER_SERVICE = "com.android.server.pm.PackageManagerService";
    private static final String CLASS_PACKAGE_PARSER_PACKAGE = "android.content.pm.PackageParser.Package";

    private static final String GET_TASKS = "android.permission.GET_TASKS";
    private static final String REMOVE_TASKS = "android.permission.REMOVE_TASKS";
    private static final String REAL_GET_TASKS = "android.permission.REAL_GET_TASKS";
    private static final String READ_FRAME_BUFFER = "android.permission.READ_FRAME_BUFFER";
    private static final String GET_DETAILED_TASKS = "android.permission.GET_DETAILED_TASKS";
    private static final String ACCESS_INSTANT_APPS = "android.permission.ACCESS_INSTANT_APPS";
    private static final String MANAGE_ACTIVITY_STACKS = "android.permission.MANAGE_ACTIVITY_STACKS";
    private static final String START_TASKS_FROM_RECENTS = "android.permission.START_TASKS_FROM_RECENTS";
    private static final String INTERACT_ACROSS_USERS = "android.permission.INTERACT_ACROSS_USERS";
    private static final String CREATE_USERS = "android.permission.CREATE_USERS";
    private static final String MANAGE_USERS = "android.permission.MANAGE_USERS";
    private static final String WRITE_SETTINGS = "android.permission.WRITE_SETTINGS";
    private static final String FORCE_STOP_PACKAGES = "android.permission.FORCE_STOP_PACKAGES";
    private static final String MANAGE_DEVICE_ADMINS = "android.permission.MANAGE_DEVICE_ADMINS";

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }

    public static void initAndroid(final ClassLoader classLoader) {
        try {
            final Class<?> pmServiceClass = XposedHelpers.findClass(CLASS_PACKAGE_MANAGER_SERVICE, classLoader);

            XposedHelpers.findAndHookMethod(pmServiceClass, "grantPermissionsLPw",
                    CLASS_PACKAGE_PARSER_PACKAGE, boolean.class, String.class, new XC_MethodHook() {
                        @SuppressWarnings("unchecked")
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            final String pkgName = (String) XposedHelpers.getObjectField(param.args[0], "packageName");
                            final Object extras = XposedHelpers.getObjectField(param.args[0], "mExtras");
                            final Object ps = XposedHelpers.callMethod(extras, "getPermissionsState");
                            final Object settings = XposedHelpers.getObjectField(param.thisObject, "mSettings");
                            final Object permissions = XposedHelpers.getObjectField(settings, "mPermissions");

                            // Launcher
                            if (LauncherHook.INSTANCE.getLauncherPackages().contains(pkgName)) {
                                grantPerm(ps, permissions, GET_TASKS);
                                grantPerm(ps, permissions, REMOVE_TASKS);
                                grantPerm(ps, permissions, REAL_GET_TASKS);
                                grantPerm(ps, permissions, READ_FRAME_BUFFER);
                                grantPerm(ps, permissions, GET_DETAILED_TASKS);
                                grantPerm(ps, permissions, ACCESS_INSTANT_APPS);
                                grantPerm(ps, permissions, MANAGE_ACTIVITY_STACKS);
                                grantPerm(ps, permissions, START_TASKS_FROM_RECENTS);
                                grantPerm(ps, permissions, INTERACT_ACROSS_USERS);
                                grantPerm(ps, permissions, CREATE_USERS);
                                grantPerm(ps, permissions, MANAGE_USERS);
                                grantPerm(ps, permissions, WRITE_SETTINGS);
                                grantPerm(ps, permissions, FORCE_STOP_PACKAGES);
                                grantPerm(ps, permissions, MANAGE_DEVICE_ADMINS);
                                grantPerm(ps, permissions, Manifest.permission.WRITE_SECURE_SETTINGS);
                            }

                            if (MainHook.PACKAGE_OWN.equals(pkgName)) {
                                grantPerm(ps, permissions, Manifest.permission.WRITE_SECURE_SETTINGS);
                            }
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private static void grantPerm(Object ps, Object permissions, String perm) {
        final Object p = XposedHelpers.callMethod(permissions, "get", perm);
        XposedHelpers.callMethod(ps, "grantInstallPermission", p);
    }
}
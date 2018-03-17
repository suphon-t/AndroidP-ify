/*
 * Copyright (C) 2017 Peter Gregus for GravityBox Project (C3C076@xda)
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

package xyz.paphonb.androidpify.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.os.FileObserver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SettingsManager {
    public interface FileObserverListener {
        void onFileUpdated(String path);
        void onFileAttributesChanged(String path);
    }

    private static Context mContext;
    private static SettingsManager mInstance;
    private WorldReadablePrefs mPrefsMain;
    private FileObserver mFileObserver;
    private List<FileObserverListener> mFileObserverListeners;

    private SettingsManager(Context context) {
        mContext = !context.isDeviceProtectedStorage() ? context.createDeviceProtectedStorageContext() : context;
        mFileObserverListeners = new ArrayList<>();
        mPrefsMain =  new WorldReadablePrefs(mContext, mContext.getPackageName() + "_preferences");
        mFileObserverListeners.add(mPrefsMain);

        registerFileObserver();
    }

    public static synchronized SettingsManager getInstance(Context context) {
        if (context == null && mInstance == null)
            throw new IllegalArgumentException("Context cannot be null");

        if (mInstance == null) {
            mInstance = new SettingsManager(context.getApplicationContext() != null ?
                    context.getApplicationContext() : context);
        }
        return mInstance;
    }

    public String getOrCreateUuid() {
        String uuid = mPrefsMain.getString("settings_uuid", null);
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
            mPrefsMain.edit().putString("settings_uuid", uuid).commit();
        }
        return uuid;
    }

    public void resetUuid(String uuid) {
        mPrefsMain.edit().putString("settings_uuid", uuid).commit();
    }

    public void resetUuid() {
        resetUuid(null);
    }

    public void fixFolderPermissionsAsync() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                // main dir
                File pkgFolder = mContext.getDataDir();
                if (pkgFolder.exists()) {
                    pkgFolder.setExecutable(true, false);
                    pkgFolder.setReadable(true, false);
                }
                // cache dir
                File cacheFolder = mContext.getCacheDir();
                if (cacheFolder.exists()) {
                    cacheFolder.setExecutable(true, false);
                    cacheFolder.setReadable(true, false);
                }
                // files dir
                File filesFolder = mContext.getFilesDir();
                if (filesFolder.exists()) {
                    filesFolder.setExecutable(true, false);
                    filesFolder.setReadable(true, false);
                    for (File f : filesFolder.listFiles()) {
                        f.setExecutable(true, false);
                        f.setReadable(true, false);
                    }
                }
                // prefs dir
                File prefsFodler = new File(mContext.getDataDir(), "shared_prefs");
                if (prefsFodler.exists()) {
                    prefsFodler.setExecutable(true, false);
                    prefsFodler.setReadable(true, false);
                }
            }
        });
    }

    public WorldReadablePrefs getMainPrefs() {
        return mPrefsMain;
    }

    private void registerFileObserver() {
        mFileObserver = new FileObserver(mContext.getDataDir() + "/shared_prefs",
                FileObserver.ATTRIB | FileObserver.CLOSE_WRITE) {
            @Override
            public void onEvent(int event, String path) {
                for (FileObserverListener l : mFileObserverListeners) {
                    if ((event & FileObserver.ATTRIB) != 0)
                        l.onFileAttributesChanged(path);
                    if ((event & FileObserver.CLOSE_WRITE) != 0)
                        l.onFileUpdated(path);
                }
            }
        };
        mFileObserver.startWatching();
    }
}
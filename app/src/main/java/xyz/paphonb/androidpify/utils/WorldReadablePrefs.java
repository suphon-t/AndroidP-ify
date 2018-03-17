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
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.util.Map;
import java.util.Set;

public class WorldReadablePrefs implements SharedPreferences,
        SettingsManager.FileObserverListener {
    public static final boolean DEBUG = true;

    public interface OnPreferencesCommitedListener {
        void onPreferencesCommited();
    }

    public interface OnSharedPreferenceChangeCommitedListener {
        void onSharedPreferenceChangeCommited();
    }

    private String mPrefsName;
    private Context mContext;
    private SharedPreferences mPrefs;
    private OnPreferencesCommitedListener mOnPreferencesCommitedListener;
    private OnSharedPreferenceChangeCommitedListener mOnSharedPreferenceChangeCommitedListener;
    private EditorWrapper mEditorWrapper;
    private boolean mSelfAttrChange;
    private Handler mHandler;

    public WorldReadablePrefs(Context ctx, String prefsName) {
        mContext = ctx;
        mPrefsName = prefsName;
        mPrefs = ctx.getSharedPreferences(mPrefsName, 0);
        mHandler = new Handler();
        maybePreCreateFile();
        fixPermissions(true);
    }

    @Override
    public boolean contains(String key) {
        return mPrefs.contains(key);
    }

    @Override
    public EditorWrapper edit() {
        if (mEditorWrapper == null) {
            mEditorWrapper = new EditorWrapper(mPrefs.edit());
        }
        return mEditorWrapper;
    }

    @Override
    public Map<String, ?> getAll() {
        return mPrefs.getAll();
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        return mPrefs.getBoolean(key, defValue);
    }

    @Override
    public float getFloat(String key, float defValue) {
        return mPrefs.getFloat(key, defValue);
    }

    @Override
    public int getInt(String key, int defValue) {
        return mPrefs.getInt(key, defValue);
    }

    @Override
    public long getLong(String key, long defValue) {
        return mPrefs.getLong(key, defValue);
    }

    @Override
    public String getString(String key, String defValue) {
        return mPrefs.getString(key, defValue);
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        return mPrefs.getStringSet(key, defValues);
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(
            OnSharedPreferenceChangeListener listener) {
        mPrefs.registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(
            OnSharedPreferenceChangeListener listener) {
        mPrefs.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public void setOnSharedPreferenceChangeCommitedListener(
            OnSharedPreferenceChangeCommitedListener listener) {
        mOnSharedPreferenceChangeCommitedListener = listener;
    }

    private void maybePreCreateFile() {
        try {
            File sharedPrefsFolder = new File(mContext.getDataDir().getAbsolutePath() + "/shared_prefs");
            if (!sharedPrefsFolder.exists()) {
                sharedPrefsFolder.mkdir();
                sharedPrefsFolder.setExecutable(true, false);
                sharedPrefsFolder.setReadable(true, false);
            }
            File f = new File(sharedPrefsFolder.getAbsolutePath() + "/" + mPrefsName + ".xml");
            if (!f.exists()) {
                f.createNewFile();
                f.setReadable(true, false);
            }
        } catch (Exception e) {
            Log.e("GravityBox", "Error pre-creating prefs file " + mPrefsName + ": " + e.getMessage());
        }
    }

    private void fixPermissions(boolean force) {
        File sharedPrefsFolder = new File(mContext.getDataDir().getAbsolutePath() + "/shared_prefs");
        if (sharedPrefsFolder.exists()) {
            sharedPrefsFolder.setExecutable(true, false);
            sharedPrefsFolder.setReadable(true, false);
            File f = new File(sharedPrefsFolder.getAbsolutePath() + "/" + mPrefsName + ".xml");
            if (f.exists()) {
                mSelfAttrChange = !force;
                f.setReadable(true, false);
            }
        }
    }

    private void fixPermissions() {
        fixPermissions(false);
    }

    @Override
    public void onFileAttributesChanged(String path) {
        if (path != null && path.endsWith(mPrefsName + ".xml")) {
            if (mSelfAttrChange) {
                mSelfAttrChange = false;
                if (DEBUG) Log.d("GravityBox", "onFileAttributesChanged: " + mPrefsName +
                        "; ignoring self change");
                return;
            }
            if (DEBUG) Log.d("GravityBox", "onFileAttributesChanged: " + mPrefsName +
                    "; calling fixPermissions()");
            fixPermissions();
        }
    }

    @Override
    public void onFileUpdated(String path) {
        if (path != null && path.endsWith(mPrefsName + ".xml")) {
            if (DEBUG) Log.d("GravityBox", "Prefs file updated for " + mPrefsName);
            if (mOnPreferencesCommitedListener != null) {
                postOnPreferencesCommited();
            } else if (mOnSharedPreferenceChangeCommitedListener != null) {
                postOnSharedPreferenceChangeCommited();
            }
        }
    }

    private void postOnPreferencesCommited() {
        mHandler.removeCallbacks(mPreferencesCommitedRunnable);
        mHandler.postDelayed(mPreferencesCommitedRunnable, 100);
    }

    private void postOnSharedPreferenceChangeCommited() {
        mHandler.removeCallbacks(mSharedPreferenceChangeCommitedRunnable);
        mHandler.postDelayed(mSharedPreferenceChangeCommitedRunnable, 100);
    }

    private Runnable mPreferencesCommitedRunnable = new Runnable() {
        @Override
        public void run() {
            if (mOnPreferencesCommitedListener != null) {
                mOnPreferencesCommitedListener.onPreferencesCommited();
                mOnPreferencesCommitedListener = null;
            }
        }
    };

    private Runnable mSharedPreferenceChangeCommitedRunnable = new Runnable() {
        @Override
        public void run() {
            if (mOnSharedPreferenceChangeCommitedListener != null) {
                mOnSharedPreferenceChangeCommitedListener.onSharedPreferenceChangeCommited();
            }
        }
    };

    public class EditorWrapper implements SharedPreferences.Editor {

        private SharedPreferences.Editor mEditor;

        public EditorWrapper(SharedPreferences.Editor editor) {
            mEditor = editor;
        }

        @Override
        public EditorWrapper putString(String key,
                                       String value) {
            mEditor.putString(key, value);
            return this;
        }

        @Override
        public EditorWrapper putStringSet(String key,
                                          Set<String> values) {
            mEditor.putStringSet(key, values);
            return this;
        }

        @Override
        public EditorWrapper putInt(String key,
                                    int value) {
            mEditor.putInt(key, value);
            return this;
        }

        @Override
        public EditorWrapper putLong(String key,
                                     long value) {
            mEditor.putLong(key, value);
            return this;
        }

        @Override
        public EditorWrapper putFloat(String key,
                                      float value) {
            mEditor.putFloat(key, value);
            return this;
        }

        @Override
        public EditorWrapper putBoolean(String key,
                                        boolean value) {
            mEditor.putBoolean(key, value);
            return this;
        }

        @Override
        public EditorWrapper remove(String key) {
            mEditor.remove(key);
            return this;
        }

        @Override
        public EditorWrapper clear() {
            mEditor.clear();
            return this;
        }

        @Override
        public boolean commit() {
            return commit(null);
        }

        public boolean commit(OnPreferencesCommitedListener listener) {
            if (DEBUG) Log.d("GravityBox", "Commit for " + mPrefsName);
            mOnPreferencesCommitedListener = listener;
            boolean ret = mEditor.commit();
            return ret;
        }

        @Override
        public void apply() {
            throw new UnsupportedOperationException(
                    "apply() not supported. Use commit() instead.");
        }
    }
}
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

package xyz.paphonb.androidpify.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.support.v7.preference.PreferenceRecyclerViewAccessibilityDelegate
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Toast
import xyz.paphonb.androidpify.R
import xyz.paphonb.androidpify.utils.NotificationUtils
import xyz.paphonb.androidpify.utils.SettingsManager

class SettingsActivity : BaseActivity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private val appBarElevation by lazy { resources.getDimensionPixelSize(R.dimen.app_bar_elevation) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        SettingsManager.getInstance(this).fixFolderPermissionsAsync()
        SettingsManager.getInstance(this).mainPrefs.edit().putBoolean("can_read_prefs", true).commit()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.content, LauncherSettingsFragment())
                    .commit()
        }

        updateUpButton()
    }

    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat?, pref: Preference): Boolean {
        val fragment = Fragment.instantiate(this, pref.fragment, pref.extras)
        if (fragment is DialogFragment) {
            fragment.show(supportFragmentManager, pref.key)
        } else {
            val transaction = supportFragmentManager.beginTransaction()
            title = pref.title
            transaction.setCustomAnimations(R.animator.fly_in, R.animator.fade_out, R.animator.fade_in, R.animator.fly_out)
            transaction.replace(R.id.content, fragment)
            transaction.addToBackStack("PreferenceFragment")
            transaction.commit()
            updateUpButton(true)
        }
        return true
    }

    private fun updateUpButton() {
        updateUpButton(supportFragmentManager.backStackEntryCount != 0)
    }

    private fun updateUpButton(enabled: Boolean) {
        supportActionBar!!.setDisplayHomeAsUpEnabled(enabled)
        actionBarElevation = (if (enabled) appBarElevation else 0).toFloat()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        updateUpButton()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    abstract class BaseFragment : PreferenceFragmentCompat(),
            SharedPreferences.OnSharedPreferenceChangeListener {

        private val prefs by lazy { preferenceManager.sharedPreferences }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.setStorageDeviceProtected()
            prefs.edit().putBoolean("can_read_prefs", true).apply()
            prefs.registerOnSharedPreferenceChangeListener(this)
        }

        protected fun isActivated() = false

        protected fun isPrefsFileReadable() = false

        @SuppressLint("ApplySharedPref")
        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (!isActivated()) return
            if (sharedPreferences == null || key == null || context == null) return
            NotificationUtils.getInstance(context!!).showSettingsChangedNotification(key)
        }

    }

    class LauncherSettingsFragment : BaseFragment(), Preference.OnPreferenceClickListener {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            super.onCreatePreferences(savedInstanceState, rootKey)
            addPreferencesFromResource(R.xml.preferences_home)

            val moduleNotEnabled = preferenceScreen.findPreference("module_not_enabled")
            val preferencesNotReadable = preferenceScreen.findPreference("preferences_not_readable")
            if (isActivated()) {
                preferenceScreen.removePreference(moduleNotEnabled)
                if (isPrefsFileReadable()) {
                    preferenceScreen.removePreference(preferencesNotReadable)
                } else {
                    preferencesNotReadable.onPreferenceClickListener = this
                }
            } else {
                moduleNotEnabled.onPreferenceClickListener = this
                preferenceScreen.removePreference(preferencesNotReadable)
            }
        }

        override fun setDivider(divider: Drawable) {
            super.setDivider(null)
        }

        override fun setDividerHeight(height: Int) {
            super.setDividerHeight(0)
        }

        override fun onCreateRecyclerView(inflater: LayoutInflater, parent: ViewGroup, savedInstanceState: Bundle?): RecyclerView {
            val recyclerView = inflater
                    .inflate(R.layout.settings_recycler_view, parent, false) as RecyclerView

            recyclerView.layoutManager = onCreateLayoutManager()
            recyclerView.setAccessibilityDelegateCompat(
                    PreferenceRecyclerViewAccessibilityDelegate(recyclerView))

            return recyclerView
        }

        override fun onResume() {
            super.onResume()
            activity!!.setTitle(R.string.app_name)
        }

        override fun onPreferenceClick(preference: Preference): Boolean {
            if ("module_not_enabled" == preference.key) {
                val intent = Intent()
                intent.component = ComponentName("de.robv.android.xposed.installer", "de.robv.android.xposed.installer.WelcomeActivity")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    activity!!.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                    Toast.makeText(context, R.string.xposed_installer_not_found, Toast.LENGTH_LONG).show()
                }
                return true
            }
            return false
        }
    }

    class SubSettingsFragment : BaseFragment(), Preference.OnPreferenceClickListener {

        private val title by lazy { arguments!!.getString(TITLE) }
        private val contents by lazy { arguments!!.getInt(CONTENT_RES_ID) }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            super.onCreatePreferences(savedInstanceState, rootKey)
            addPreferencesFromResource(contents)
            if (contents == R.xml.preferences_settings) {
                preferenceScreen.findPreference("use_pixel_home_button").onPreferenceClickListener = this
            }
        }

        override fun onPreferenceClick(preference: Preference): Boolean {
            if ("use_pixel_home_button" == preference.key) {
                val packageManager = context!!.packageManager
                packageManager.getLaunchIntentForPackage("xyz.paphonb.pixelnavbar")?.run {
                    startActivity(this)
                } ?: try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=xyz.paphonb.pixelnavbar")))
                } catch (t: Throwable) {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://forum.xda-developers.com/xposed/modules/xposed-pixel-navigation-bar-pixel-t3502987")))
                    } catch (t2: Throwable) {
                        Toast.makeText(context, R.string.please_install_pixel_navbar, Toast.LENGTH_LONG).show()
                    }
                }
            }
            return true
        }

        override fun onResume() {
            super.onResume()
            activity!!.title = title
        }

        companion object {

            const val TITLE = "TITLE"
            const val CONTENT_RES_ID = "content_res_id"
        }

    }

}

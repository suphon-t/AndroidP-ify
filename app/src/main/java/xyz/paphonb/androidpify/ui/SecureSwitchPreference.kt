package xyz.paphonb.androidpify.ui

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.support.v14.preference.SwitchPreference
import android.util.AttributeSet

class SecureSwitchPreference(context: Context, attrs: AttributeSet) : SwitchPreference(context, attrs) {

    private val hasPermission get() = context
            .checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    init {
        isEnabled = hasPermission
    }

    override fun onSetInitialValue(restoreValue: Boolean, defaultValue: Any?) {
        isChecked = getPersistedBoolean(mChecked)
    }

    override fun getPersistedBoolean(defaultReturnValue: Boolean): Boolean {
        return Settings.Secure.getInt(context.contentResolver, key, if (defaultReturnValue) 1 else 0) != 0
    }

    override fun persistBoolean(value: Boolean): Boolean {
        return hasPermission && Settings.Secure.putInt(context.contentResolver, key, if (value) 1 else 0)
    }
}
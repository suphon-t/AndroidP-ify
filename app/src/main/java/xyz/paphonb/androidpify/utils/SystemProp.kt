package xyz.paphonb.androidpify.utils

import de.robv.android.xposed.XposedHelpers.callStaticMethod
import de.robv.android.xposed.XposedHelpers.findClass

object SystemProp {

    // Get the value for the given key
    // @param key: key to lookup
    // @return null if the key isn't found
    operator fun get(key: String): String? {
        return try {
            val classSystemProperties = findClass("android.os.SystemProperties", null)
            callStaticMethod(classSystemProperties, "get", key) as String
        } catch (t: Throwable) {
            null
        }
    }

    // Get the value for the given key
    // @param key: key to lookup
    // @param def: default value to return
    // @return if the key isn't found, return def if it isn't null, or an empty string otherwise
    operator fun get(key: String, def: String): String {
        return try {
            val classSystemProperties = findClass("android.os.SystemProperties", null)
            callStaticMethod(classSystemProperties, "get", key, def) as String
        } catch (t: Throwable) {
            def
        }
    }

    // Get the value for the given key, and return as an integer
    // @param key: key to lookup
    // @param def: default value to return
    // @return the key parsed as an integer, or def if the key isn't found or cannot be parsed
    fun getInt(key: String, def: Int?): Int? {
        return try {
            val classSystemProperties = findClass("android.os.SystemProperties", null)
            callStaticMethod(classSystemProperties, "getInt", key, def) as Int
        } catch (t: Throwable) {
            def
        }
    }

    // Get the value for the given key, and return as a long
    // @param key: key to lookup
    // @param def: default value to return
    // @return the key parsed as a long, or def if the key isn't found or cannot be parsed
    fun getLong(key: String, def: Long?): Long? {
        return try {
            val classSystemProperties = findClass("android.os.SystemProperties", null)
            callStaticMethod(classSystemProperties, "getLong", key, def) as Long
        } catch (t: Throwable) {
            def
        }
    }

    // Get the value (case insensitive) for the given key, returned as a boolean
    // Values 'n', 'no', '0', 'false' or 'off' are considered false
    // Values 'y', 'yes', '1', 'true' or 'on' are considered true
    // If the key does not exist, or has any other value, then the default result is returned
    // @param key: key to lookup
    // @param def: default value to return
    // @return the key parsed as a boolean, or def if the key isn't found or cannot be parsed
    fun getBoolean(key: String, def: Boolean): Boolean? {
        return try {
            val classSystemProperties = findClass("android.os.SystemProperties", null)
            callStaticMethod(classSystemProperties, "getBoolean", key, def) as Boolean
        } catch (t: Throwable) {
            def
        }
    }

    // Set the value for the given key
    operator fun set(key: String, `val`: String) {
        try {
            val classSystemProperties = findClass("android.os.SystemProperties", null)
            callStaticMethod(classSystemProperties, "set", key, `val`)
        } catch (t: Throwable) {
        }

    }
}
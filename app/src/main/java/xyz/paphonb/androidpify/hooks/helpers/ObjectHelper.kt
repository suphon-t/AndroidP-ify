package xyz.paphonb.androidpify.hooks.helpers

import de.robv.android.xposed.XposedHelpers
import kotlin.reflect.KProperty

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

open class ObjectHelper<out T : Any>(protected val classLoader: ClassLoader, val target: T) {

    inner class BooleanField(fieldName: String) : AnyField<Boolean>(fieldName) {

        override fun getValue(thisRef: Any, property: KProperty<*>): Boolean {
            return field.getBoolean(target)
        }

        override fun setValue(thisRef: Any, property: KProperty<*>, value: Boolean) {
            field.setBoolean(target, value)
        }
    }

    inner class IntField(fieldName: String) : AnyField<Int>(fieldName) {

        override operator fun getValue(thisRef: Any, property: KProperty<*>): Int {
            return field.getInt(target)
        }

        override operator fun setValue(thisRef: Any, property: KProperty<*>, value: Int) {
            field.setInt(target, value)
        }
    }

    open inner class AnyField<T>(fieldName: String) {

        val field = XposedHelpers.findField(target.javaClass, fieldName)!!

        @Suppress("UNCHECKED_CAST")
        open operator fun getValue(thisRef: Any, property: KProperty<*>): T {
            return field.get(target) as T
        }

        open operator fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
            field.set(target, value)
        }
    }
}
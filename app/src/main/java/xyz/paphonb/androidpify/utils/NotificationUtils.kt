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

package xyz.paphonb.androidpify.utils

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import xyz.paphonb.androidpify.R

class NotificationUtils(val context: Context) {

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val general = NotificationChannel(
                    "general", context.getString(R.string.notif_channel_general_title),
                    NotificationManager.IMPORTANCE_HIGH)

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(general)
        }
    }

    fun showSettingsChangedNotification(key: String) {
        PreferencesList.prefLevel[key]?.let { level ->
            val restartIntent = Intent(context, NotificationReceiver::class.java)
                    .putExtra("level", level)
            val intent = PendingIntent.getBroadcast(context, level, restartIntent, 0)
            val rebootText = context.getString(
                    if (level and PreferencesList.LEVEL_ANDROID != 0) R.string.restart_android
                    else R.string.restart_systemui)
            val restartAction = NotificationCompat.Action.Builder(R.drawable.ic_stat_p,
                    rebootText, intent)
                    .build()

            NotificationManagerCompat.from(context).notify(level, NotificationCompat.Builder(context, "general")
                    .setSmallIcon(R.drawable.ic_stat_p)
                    .setContentTitle(context.getString(R.string.settings_changed))
                    .setContentText(context.getString(R.string.settings_changed_description))
                    .setColor(context.resources.getColor(R.color.colorAccent))
                    .addAction(restartAction).build())
        }
    }

    companion object {

        @SuppressLint("StaticFieldLeak")
        private var INSTANCE: NotificationUtils? = null

        fun getInstance(context: Context): NotificationUtils {
            if (INSTANCE == null) {
                INSTANCE = NotificationUtils(context.applicationContext)
            }
            return INSTANCE!!
        }
    }
}
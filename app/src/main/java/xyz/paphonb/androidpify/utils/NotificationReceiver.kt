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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationManagerCompat
import android.widget.Toast
import xyz.paphonb.androidpify.MainHook
import xyz.paphonb.androidpify.R
import xyz.paphonb.androidpify.hooks.SystemUIHook

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val level = intent.getIntExtra("level", 0)
        NotificationManagerCompat.from(context).cancel(level)
        if (level and PreferencesList.LEVEL_ANDROID != 0) {
            context.sendBroadcast(Intent(SystemUIHook.ACTION_SOFT_REBOOT)
                    .setPackage(MainHook.PACKAGE_SYSTEMUI))
        } else {
            context.sendBroadcast(Intent(SystemUIHook.ACTION_KILL_SYSTEMUI)
                    .setPackage(MainHook.PACKAGE_SYSTEMUI))
        }
        Toast.makeText(context, R.string.restart_broadcast_sent, Toast.LENGTH_SHORT).show()
    }
}

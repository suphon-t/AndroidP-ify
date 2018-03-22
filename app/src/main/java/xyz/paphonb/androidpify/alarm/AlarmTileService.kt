package xyz.paphonb.androidpify.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.text.format.DateFormat
import xyz.paphonb.androidpify.R
import xyz.paphonb.androidpify.aosp.NextAlarmController
import xyz.paphonb.androidpify.aosp.NextAlarmControllerImpl
import java.util.*



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

class AlarmTileService : TileService(), NextAlarmController.NextAlarmChangeCallback {

    private val controller by lazy { NextAlarmControllerImpl.getInstance(this) }
    private var intent: PendingIntent? = null
    private var nextAlarm: String? = null

    override fun onStartListening() {
        controller.addCallback(this)
    }

    override fun onStopListening() {
        controller.removeCallback(this)
    }

    override fun onClick() {
        unlockAndRun {
            sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
            intent?.send() ?: startActivity(getLongClickIntent())
        }
    }

    override fun onNextAlarmChanged(nextAlarm: AlarmManager.AlarmClockInfo?) {
        if (nextAlarm != null) {
            this.nextAlarm = formatNextAlarm(nextAlarm)
            this.intent = nextAlarm.showIntent
        } else {
            this.nextAlarm = null
            this.intent = null
        }
        refreshState()
    }

    private fun refreshState() {
        qsTile.apply {
            state = if (nextAlarm != null) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = nextAlarm ?: getTileLabel()
            updateTile()
        }
    }

    private fun getTileLabel(): CharSequence {
        return getString(R.string.status_bar_alarm)
    }

    private fun getLongClickIntent(): Intent {
        return Intent("android.intent.action.SET_ALARM")
    }

    private fun formatNextAlarm(info: AlarmManager.AlarmClockInfo?): String {
        val skeleton = if (DateFormat.is24HourFormat(this)) "EHm" else "Ehma"
        val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton)
        return if (info == null)
            ""
        else
            DateFormat.format(pattern, info.triggerTime).toString()
    }
}
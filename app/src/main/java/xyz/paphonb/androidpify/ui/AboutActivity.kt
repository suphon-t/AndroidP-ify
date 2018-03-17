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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import xyz.paphonb.androidpify.BuildConfig
import xyz.paphonb.androidpify.R

class AboutActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        findViewById<TextView>(R.id.versionText).text = String.format(
                resources.getString(R.string.version_text), BuildConfig.VERSION_NAME)
    }

    fun share(view: View) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, String.format(
                    resources.getString(R.string.share_text), getString(R.string.app_name)))
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, String.format(
                resources.getString(R.string.share_with), getString(R.string.app_name))))
    }

    fun gitHub(view: View) {
        startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://github.com/paphonb/AndroidP-ify")))
    }
}

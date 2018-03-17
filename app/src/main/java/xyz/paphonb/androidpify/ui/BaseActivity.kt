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
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import xyz.paphonb.androidpify.R

@SuppressLint("Registered")
open class BaseActivity : AppCompatActivity() {
    private val decorLayout by lazy { DecorLayout(this, window) }
    var actionBarElevation: Float
        get() = decorLayout.actionBarElevation
        set(value) { decorLayout.actionBarElevation = value }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.setContentView(decorLayout)

        enableLightUi()
        window.statusBarColor = 0
        window.navigationBarColor = 0

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
    }

    private fun enableLightUi() {
        var flags = window.decorView.systemUiVisibility
        flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        flags = flags or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        flags = flags or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        flags = flags or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.decorView.systemUiVisibility = flags
    }

    override fun setContentView(v: View) {
        val contentParent = decorLayout.findViewById(android.R.id.content) as ViewGroup
        contentParent.removeAllViews()
        contentParent.addView(v)
    }

    override fun setContentView(resId: Int) {
        val contentParent = decorLayout.findViewById(android.R.id.content) as ViewGroup
        contentParent.removeAllViews()
        LayoutInflater.from(this).inflate(resId, contentParent)
    }

    override fun setContentView(v: View, lp: ViewGroup.LayoutParams) {
        val contentParent = decorLayout.findViewById(android.R.id.content) as ViewGroup
        contentParent.removeAllViews()
        contentParent.addView(v, lp)
    }
}
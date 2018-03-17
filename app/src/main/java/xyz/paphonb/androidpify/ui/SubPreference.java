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

package xyz.paphonb.androidpify.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.util.AttributeSet;
import android.view.View;

import xyz.paphonb.androidpify.R;

public class SubPreference extends Preference implements View.OnLongClickListener {

    private int mContent;
    private int mLongClickContent;
    private boolean mLongClick;

    public SubPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SubPreference);
        for (int i = a.getIndexCount() - 1; i >= 0; i--) {
            int attr = a.getIndex(i);
            if (attr == R.styleable.SubPreference_content) {
                mContent = a.getResourceId(attr, 0);
            } else if (attr == R.styleable.SubPreference_longClickContent) {
                mLongClickContent = a.getResourceId(attr, 0);
            }
        }
        a.recycle();
        setFragment(SettingsActivity.SubSettingsFragment.class.getName());
    }

    @Override
    public Bundle getExtras() {
        Bundle b = new Bundle(2);
        b.putString(SettingsActivity.SubSettingsFragment.TITLE, (String) getTitle());
        b.putInt(SettingsActivity.SubSettingsFragment.CONTENT_RES_ID, getContent());
        return b;
    }

    public int getContent() {
        return mLongClick ? mLongClickContent : mContent;
    }

    @Override
    protected void onClick() {
        mLongClick = false;
        super.onClick();
    }

    @Override
    public boolean onLongClick(View view) {
        if (mLongClickContent != 0) {
            mLongClick = true;
            super.onClick();
            return true;
        } else {
            return false;
        }
    }
}
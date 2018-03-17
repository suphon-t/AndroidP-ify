/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.paphonb.androidpify;

import android.graphics.Rect;
import android.graphics.RectF;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import de.robv.android.xposed.XposedHelpers;

/**
 * An animation that controls the clip of an object. See the
 * {@link android.view.animation full package} description for details and
 * sample code.
 */
public class ClipRectAnimation extends Animation {
    private RectF mFromRect = new RectF();
    private RectF mToRect = new RectF();

    private Rect mResolvedFrom = new Rect();
    private Rect mResolvedTo = new Rect();
    private Method mSetClipRect;

    /**
     * Constructor to use when building a ClipRectAnimation from code
     *
     * @param fromClip the clip rect to animate from
     * @param toClip the clip rect to animate to
     */
    public ClipRectAnimation(RectF fromClip, RectF toClip) {
        if (fromClip == null || toClip == null) {
            throw new RuntimeException("Expected non-null animation clip rects");
        }
        mFromRect.set(fromClip);
        mToRect.set(toClip);
        initReflection();
    }

    /**
     * Constructor to use when building a ClipRectAnimation from code
     */
    public ClipRectAnimation(float fromL, float fromT, float fromR, float fromB,
                             float toL, float toT, float toR, float toB) {
        mFromRect.set(fromL, fromT, fromR, fromB);
        mToRect.set(toL, toT, toR, toB);
        initReflection();
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight) {
        super.initialize(width, height, parentWidth, parentHeight);
        mResolvedFrom.left = (int) resolveSize(RELATIVE_TO_SELF, mFromRect.left, width, parentWidth);
        mResolvedFrom.top = (int) resolveSize(RELATIVE_TO_SELF, mFromRect.top, height, parentHeight);
        mResolvedFrom.right = (int) resolveSize(RELATIVE_TO_SELF, mFromRect.right, width, parentWidth);
        mResolvedFrom.bottom = (int) resolveSize(RELATIVE_TO_SELF, mFromRect.bottom, height, parentHeight);
        mResolvedTo.left = (int) resolveSize(RELATIVE_TO_SELF, mToRect.left, width, parentWidth);
        mResolvedTo.top = (int) resolveSize(RELATIVE_TO_SELF, mToRect.top, height, parentHeight);
        mResolvedTo.right = (int) resolveSize(RELATIVE_TO_SELF, mToRect.right, width, parentWidth);
        mResolvedTo.bottom = (int) resolveSize(RELATIVE_TO_SELF, mToRect.bottom, height, parentHeight);
    }

    private void initReflection() {
        mSetClipRect = XposedHelpers.findMethodExact(Transformation.class, "setClipRect",
                int.class, int.class, int.class, int.class);
    }

    @Override
    protected void applyTransformation(float it, Transformation tr) {
        int l = mResolvedFrom.left + (int) ((mResolvedTo.left - mResolvedFrom.left) * it);
        int t = mResolvedFrom.top + (int) ((mResolvedTo.top - mResolvedFrom.top) * it);
        int r = mResolvedFrom.right + (int) ((mResolvedTo.right - mResolvedFrom.right) * it);
        int b = mResolvedFrom.bottom + (int) ((mResolvedTo.bottom - mResolvedFrom.bottom) * it);

        try {
            mSetClipRect.invoke(tr, l, t, r, b);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean willChangeTransformationMatrix() {
        return false;
    }
}
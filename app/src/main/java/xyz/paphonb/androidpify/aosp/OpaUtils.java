package xyz.paphonb.androidpify.aosp;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.res.Resources;
import android.util.ArraySet;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import de.robv.android.xposed.XposedHelpers;
import xyz.paphonb.androidpify.R;

public final class OpaUtils {
    static final Interpolator INTERPOLATOR_40_40 = new PathInterpolator(0.4f, 0.0f, 0.6f, 1.0f);
    static final Interpolator INTERPOLATOR_40_OUT = new PathInterpolator(0.4f, 0.0f, 1.0f, 1.0f);

    private static Constructor renderNodeAnimator;

    static Animator getScaleAnimatorX(View v, float factor, int duration, Interpolator interpolator) {
        Animator anim = createRenderNodeAnimator(3, factor);
        setTarget(anim, v);
        anim.setInterpolator(interpolator);
        anim.setDuration((long) duration);
        return anim;
    }

    static Animator getScaleAnimatorY(View v, float factor, int duration, Interpolator interpolator) {
        Animator anim = createRenderNodeAnimator(4, factor);
        setTarget(anim, v);
        anim.setInterpolator(interpolator);
        anim.setDuration((long) duration);
        return anim;
    }

    static Animator getDeltaAnimatorX(View v, Interpolator interpolator, float deltaX, int duration) {
        Animator anim = createRenderNodeAnimator(8, v.getX() + deltaX);
        setTarget(anim, v);
        anim.setInterpolator(interpolator);
        anim.setDuration((long) duration);
        return anim;
    }

    static Animator getDeltaAnimatorY(View v, Interpolator interpolator, float deltaY, int duration) {
        Animator anim = createRenderNodeAnimator(9, v.getY() + deltaY);
        setTarget(anim, v);
        anim.setInterpolator(interpolator);
        anim.setDuration((long) duration);
        return anim;
    }

    static Animator getTranslationAnimatorX(View v, Interpolator interpolator, int duration) {
        Animator anim = createRenderNodeAnimator(0, 0.0f);
        setTarget(anim, v);
        anim.setInterpolator(interpolator);
        anim.setDuration((long) duration);
        return anim;
    }

    static Animator getTranslationAnimatorY(View v, Interpolator interpolator, int duration) {
        Animator anim = createRenderNodeAnimator(1, 0.0f);
        setTarget(anim, v);
        anim.setInterpolator(interpolator);
        anim.setDuration((long) duration);
        return anim;
    }

    static ObjectAnimator getAlphaObjectAnimator(View v, float alpha, int duration, int delay, Interpolator interpolator) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(v, View.ALPHA, alpha);
        anim.setInterpolator(interpolator);
        anim.setDuration((long) duration);
        anim.setStartDelay((long) delay);
        return anim;
    }

    static Animator getAlphaAnimator(View v, float alpha, int duration, Interpolator interpolator) {
        return getAlphaAnimator(v, alpha, duration, 0, interpolator);
    }

    static Animator getAlphaAnimator(View v, float alpha, int duration, int startDelay, Interpolator interpolator) {
        Animator anim = createRenderNodeAnimator(11, alpha);
        setTarget(anim, v);
        anim.setInterpolator(interpolator);
        anim.setDuration((long) duration);
        anim.setStartDelay((long) startDelay);
        return anim;
    }

    static Animator getLongestAnim(ArraySet<Animator> animators) {
        long longestDuration = Long.MIN_VALUE;
        Animator longestAnim = null;
        for (int i = animators.size() - 1; i >= 0; i--) {
            Animator a = animators.valueAt(i);
            if (a.getTotalDuration() > longestDuration) {
                longestAnim = a;
                longestDuration = a.getTotalDuration();
            }
        }
        return longestAnim;
    }
    
    private static void setTarget(Animator animator, View view) {
        XposedHelpers.callMethod(animator, "setTarget", new Class[]{View.class}, view);
    }

    static ObjectAnimator getScaleObjectAnimator(View v, float factor, int duration, Interpolator interpolator) {
        PropertyValuesHolder[] propertyValuesHolder = new PropertyValuesHolder[2];
        propertyValuesHolder[0] = PropertyValuesHolder.ofFloat(View.SCALE_X, factor);
        propertyValuesHolder[1] = PropertyValuesHolder.ofFloat(View.SCALE_Y, factor);
        ObjectAnimator scaleAnimator = ObjectAnimator.ofPropertyValuesHolder(v, propertyValuesHolder);
        scaleAnimator.setDuration((long) duration);
        scaleAnimator.setInterpolator(interpolator);
        return scaleAnimator;
    }

    static ObjectAnimator getTranslationObjectAnimatorY(View v, Interpolator interpolator, float deltaY, float startY, int duration) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(v, View.Y, startY, startY + deltaY);
        anim.setInterpolator(interpolator);
        anim.setDuration((long) duration);
        return anim;
    }

    static ObjectAnimator getTranslationObjectAnimatorX(View v, Interpolator interpolator, float deltaX, float startX, int duration) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(v, View.X, startX, startX + deltaX);
        anim.setInterpolator(interpolator);
        anim.setDuration((long) duration);
        return anim;
    }

    static float getPxVal(Resources resources, int id) {
        return (float) resources.getDimensionPixelOffset(id);
    }

    static float getDeltaDiamondPositionTopX() {
        return 0.0f;
    }

    static float getDeltaDiamondPositionTopY(Resources resources) {
        return -getPxVal(resources, R.dimen.opa_diamond_translation);
    }

    static float getDeltaDiamondPositionLeftX(Resources resources) {
        return -getPxVal(resources, R.dimen.opa_diamond_translation);
    }

    static float getDeltaDiamondPositionLeftY() {
        return 0.0f;
    }

    static float getDeltaDiamondPositionRightX(Resources resources) {
        return getPxVal(resources, R.dimen.opa_diamond_translation);
    }

    static float getDeltaDiamondPositionRightY() {
        return 0.0f;
    }

    static float getDeltaDiamondPositionBottomX() {
        return 0.0f;
    }

    static float getDeltaDiamondPositionBottomY(Resources resources) {
        return getPxVal(resources, R.dimen.opa_diamond_translation);
    }

    private static Animator createRenderNodeAnimator(int a, float b) {
        if (renderNodeAnimator == null) {
            Class classRenderNodeAnimator = XposedHelpers.findClass("android.view.RenderNodeAnimator", View.class.getClassLoader());
            renderNodeAnimator = XposedHelpers.findConstructorExact(classRenderNodeAnimator, int.class, float.class);
        }
        try {
            return (Animator) renderNodeAnimator.newInstance(a, b);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Can't instantiate RenderNodeAnimator", e);
        }
    }
}
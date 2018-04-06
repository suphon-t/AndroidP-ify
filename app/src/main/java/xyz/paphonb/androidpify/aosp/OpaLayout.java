package xyz.paphonb.androidpify.aosp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.ArrayList;

import de.robv.android.xposed.XposedHelpers;
import xyz.paphonb.androidpify.MainHook;
import xyz.paphonb.androidpify.R;
import xyz.paphonb.androidpify.utils.Interpolators;
import xyz.paphonb.androidpify.utils.ResourceUtils;

public class OpaLayout extends FrameLayout implements ButtonInterface {
    private final Interpolator HOME_DISAPPEAR_INTERPOLATOR = new PathInterpolator(0.65f, 0.0f, 1.0f, 1.0f);
    private final ArrayList<View> mAnimatedViews = new ArrayList<>();
    private int mAnimationState = 0;
    private View mBlue;
    private View mBottom;
    private final Runnable mCheckLongPress = new Runnable() {
        public void run() {
            if (OpaLayout.this.mIsPressed) {
                OpaLayout.this.mLongClicked = true;
            }
        }
    };
    private final ArraySet<Animator> mCurrentAnimators = new ArraySet<>();
    private final Interpolator mDiamondInterpolator = new PathInterpolator(0.2f, 0.0f, 0.2f, 1.0f);
    private long mGestureAnimationSetDuration;
    private AnimatorSet mGestureAnimatorSet;
    private AnimatorSet mGestureLineSet;
    private int mGestureState = 0;
    private View mGreen;
    private ImageView mHalo;
    private ImageView mHome;
    private boolean mIsPressed;
    private boolean mIsVertical;
    private View mLeft;
    private boolean mLongClicked;
    private boolean mOpaEnabled;
    private View mRed;
    private Resources mResources;
    private final Runnable mRetract = new Runnable() {
        public void run() {
            OpaLayout.this.cancelCurrentAnimation();
            OpaLayout.this.startRetractAnimation();
        }
    };
    private View mRight;
    private long mStartTime;
    private View mTop;
    private ImageView mWhite;
    private ImageView mWhiteCutout;
    private View mYellow;
    private Class<?> classKeyButtonDrawable;

    public OpaLayout(Context context) {
        super(context);
        init();
    }

    public OpaLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OpaLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public OpaLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        classKeyButtonDrawable = XposedHelpers.findClass(
                "com.android.systemui.statusbar.policy.KeyButtonDrawable",
                getContext().getClassLoader());
        setClipChildren(false);
        setClipToPadding(false);
        LayoutInflater.from(ResourceUtils.getInstance(getContext()).getContext())
                .inflate(R.layout.home, this);
    }

    public void setHome(ImageView home) {
        setId(home.getId());
        home.setId(R.id.home_button);
        setLayoutParams(new LayoutParams(home.getLayoutParams().width, home.getLayoutParams().height));
        home.getLayoutParams().width = LayoutParams.MATCH_PARENT;
        home.setImageDrawable(null);
        addView(home);

        mResources = ResourceUtils.getInstance(getContext()).getResources();
        mBlue = findViewById(R.id.blue);
        mRed = findViewById(R.id.red);
        mYellow = findViewById(R.id.yellow);
        mGreen = findViewById(R.id.green);
        mWhite = findViewById(R.id.white);
        mWhiteCutout = findViewById(R.id.white_cutout);
        mHalo = findViewById(R.id.halo);
        mHome = findViewById(R.id.home_button);
        mHalo.setImageDrawable(createKeyButtonDrawable(getHaloDrawableForTheme("DualToneLightTheme"), getHaloDrawableForTheme("DualToneDarkTheme")));
        Paint cutoutPaint = new Paint();
        cutoutPaint.setXfermode(new PorterDuffXfermode(Mode.DST_OUT));
        mWhiteCutout.setLayerType(2, cutoutPaint);
        mAnimatedViews.add(mBlue);
        mAnimatedViews.add(mRed);
        mAnimatedViews.add(mYellow);
        mAnimatedViews.add(mGreen);
        mAnimatedViews.add(mWhite);
        mAnimatedViews.add(mWhiteCutout);
        mAnimatedViews.add(mHalo);
        setOpaEnabled(true);
        setVertical(false);
    }

    private Drawable createKeyButtonDrawable(Drawable light, Drawable dark) {
        return (Drawable) XposedHelpers.callStaticMethod(classKeyButtonDrawable, "create", new Class[]{Drawable.class, Drawable.class}, light, dark);
    }

    private Drawable getHaloDrawableForTheme(String theme) {
        Context context = getContext();
        Context ctw = new ContextThemeWrapper(context, context.getResources().getIdentifier(theme, "style", MainHook.PACKAGE_SYSTEMUI));
        int singleToneColor = context.getResources().getIdentifier("singleToneColor", "attr", MainHook.PACKAGE_SYSTEMUI);
        int color = getAttrColor(ctw, singleToneColor);
        GradientDrawable drawable = (GradientDrawable) mResources.getDrawable(R.drawable.halo);
        drawable.mutate();
        drawable.setColor(color);
        return drawable;
    }

    public static int getAttrColor(Context context, int attr) {
        TypedArray ta = context.obtainStyledAttributes(new int[]{attr});
        int colorAccent = ta.getColor(0, 0);
        ta.recycle();
        return colorAccent;
    }

    public void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility != 0) {
            cancelCurrentAnimation();
            skipToStartingValue();
        }
    }

    public void setOnLongClickListener(final OnLongClickListener l) {
        mHome.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return l != null && l.onLongClick(mHome);
            }
        });
    }

    public void setOnTouchListener(OnTouchListener l) {
        XposedHelpers.callMethod(mHome, "setOnTouchListener", l);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ValueAnimator.areAnimatorsEnabled() && mGestureState == 0) {
            int action = ev.getAction();
            boolean z = true;
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (!mCurrentAnimators.isEmpty()) {
                        if (mAnimationState != 2) {
                            return false;
                        }
                        endCurrentAnimation();
                    }
                    mStartTime = SystemClock.elapsedRealtime();
                    mLongClicked = false;
                    mIsPressed = true;
                    startDiamondAnimation();
                    removeCallbacks(mCheckLongPress);
                    postDelayed(mCheckLongPress, (long) ViewConfiguration.getLongPressTimeout());
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (mAnimationState == 1) {
                        long targetTime = 100 - (SystemClock.elapsedRealtime() - mStartTime);
                        removeCallbacks(mRetract);
                        postDelayed(mRetract, targetTime);
                        removeCallbacks(mCheckLongPress);
                        return false;
                    }
                    if (!mIsPressed || mLongClicked) {
                        z = false;
                    }
                    boolean doRetract = z;
                    mIsPressed = false;
                    if (doRetract) {
                        mRetract.run();
                    }
                    break;
                default:
                    break;
            }
            return false;
        }
        return false;
    }

    public void setImageDrawable(Drawable drawable) {
        mWhite.setImageDrawable(drawable);
        mWhiteCutout.setImageDrawable(drawable);
    }

    public void abortCurrentGesture() {
        XposedHelpers.callMethod(mHome, "abortCurrentGesture");
    }

    private void startDiamondAnimation() {
        if (isAttachedToWindow()) {
            mCurrentAnimators.clear();
            setDotsVisible();
            mCurrentAnimators.addAll(getDiamondAnimatorSet());
            mAnimationState = 1;
            startAll(mCurrentAnimators);
            return;
        }
        skipToStartingValue();
    }

    private void startRetractAnimation() {
        if (isAttachedToWindow()) {
            mCurrentAnimators.clear();
            mCurrentAnimators.addAll(getRetractAnimatorSet());
            mAnimationState = 2;
            startAll(mCurrentAnimators);
            return;
        }
        skipToStartingValue();
    }

    private void startLineAnimation() {
        if (isAttachedToWindow()) {
            mCurrentAnimators.clear();
            mCurrentAnimators.addAll(getLineAnimatorSet());
            mAnimationState = 3;
            startAll(mCurrentAnimators);
            return;
        }
        skipToStartingValue();
    }

    private void startCollapseAnimation() {
        if (isAttachedToWindow()) {
            mCurrentAnimators.clear();
            mCurrentAnimators.addAll(getCollapseAnimatorSet());
            mAnimationState = 3;
            startAll(mCurrentAnimators);
            return;
        }
        skipToStartingValue();
    }

    private void startAll(ArraySet<Animator> animators) {
        for (int i = animators.size() - 1; i >= 0; i--) {
            animators.valueAt(i).start();
        }
    }

    private ArraySet<Animator> getDiamondAnimatorSet() {
        ArraySet<Animator> animators = new ArraySet();
        animators.add(OpaUtils.getDeltaAnimatorY(mTop, mDiamondInterpolator, -OpaUtils.getPxVal(mResources, R.dimen.opa_diamond_translation), 200));
        animators.add(OpaUtils.getScaleAnimatorX(mTop, 0.8f, 200, Interpolators.FAST_OUT_SLOW_IN));
        animators.add(OpaUtils.getScaleAnimatorY(mTop, 0.8f, 200, Interpolators.FAST_OUT_SLOW_IN));
        animators.add(OpaUtils.getDeltaAnimatorY(mBottom, mDiamondInterpolator, OpaUtils.getPxVal(mResources, R.dimen.opa_diamond_translation), 200));
        animators.add(OpaUtils.getScaleAnimatorX(mBottom, 0.8f, 200, Interpolators.FAST_OUT_SLOW_IN));
        animators.add(OpaUtils.getScaleAnimatorY(mBottom, 0.8f, 200, Interpolators.FAST_OUT_SLOW_IN));
        animators.add(OpaUtils.getDeltaAnimatorX(mLeft, mDiamondInterpolator, -OpaUtils.getPxVal(mResources, R.dimen.opa_diamond_translation), 200));
        animators.add(OpaUtils.getScaleAnimatorX(mLeft, 0.8f, 200, Interpolators.FAST_OUT_SLOW_IN));
        animators.add(OpaUtils.getScaleAnimatorY(mLeft, 0.8f, 200, Interpolators.FAST_OUT_SLOW_IN));
        animators.add(OpaUtils.getDeltaAnimatorX(mRight, mDiamondInterpolator, OpaUtils.getPxVal(mResources, R.dimen.opa_diamond_translation), 200));
        animators.add(OpaUtils.getScaleAnimatorX(mRight, 0.8f, 200, Interpolators.FAST_OUT_SLOW_IN));
        animators.add(OpaUtils.getScaleAnimatorY(mRight, 0.8f, 200, Interpolators.FAST_OUT_SLOW_IN));
        animators.add(OpaUtils.getScaleAnimatorX(mWhite, 0.625f, 200, Interpolators.FAST_OUT_SLOW_IN));
        animators.add(OpaUtils.getScaleAnimatorY(mWhite, 0.625f, 200, Interpolators.FAST_OUT_SLOW_IN));
        animators.add(OpaUtils.getScaleAnimatorX(mWhiteCutout, 0.625f, 200, Interpolators.FAST_OUT_SLOW_IN));
        animators.add(OpaUtils.getScaleAnimatorY(mWhiteCutout, 0.625f, 200, Interpolators.FAST_OUT_SLOW_IN));
        animators.add(OpaUtils.getScaleAnimatorX(mHalo, 0.47619048f, 100, Interpolators.FAST_OUT_SLOW_IN));
        animators.add(OpaUtils.getScaleAnimatorY(mHalo, 0.47619048f, 100, Interpolators.FAST_OUT_SLOW_IN));
        animators.add(OpaUtils.getAlphaAnimator(mHalo, 0.0f, 100, Interpolators.FAST_OUT_SLOW_IN));
        getLongestAnim(animators).addListener(new AnimatorListenerAdapter() {
            public void onAnimationCancel(Animator animation) {
                OpaLayout.this.mCurrentAnimators.clear();
            }

            public void onAnimationEnd(Animator animation) {
                OpaLayout.this.startLineAnimation();
            }
        });
        return animators;
    }

    private ArraySet<Animator> getRetractAnimatorSet() {
        ArraySet<Animator> animators = new ArraySet();
        animators.add(OpaUtils.getTranslationAnimatorX(mRed, OpaUtils.INTERPOLATOR_40_OUT, 190));
        animators.add(OpaUtils.getTranslationAnimatorY(mRed, OpaUtils.INTERPOLATOR_40_OUT, 190));
        animators.add(OpaUtils.getScaleAnimatorX(mRed, 1.0f, 190, OpaUtils.INTERPOLATOR_40_OUT));
        animators.add(OpaUtils.getScaleAnimatorY(mRed, 1.0f, 190, OpaUtils.INTERPOLATOR_40_OUT));
        animators.add(OpaUtils.getTranslationAnimatorX(mBlue, OpaUtils.INTERPOLATOR_40_OUT, 190));
        animators.add(OpaUtils.getTranslationAnimatorY(mBlue, OpaUtils.INTERPOLATOR_40_OUT, 190));
        animators.add(OpaUtils.getScaleAnimatorX(mBlue, 1.0f, 190, OpaUtils.INTERPOLATOR_40_OUT));
        animators.add(OpaUtils.getScaleAnimatorY(mBlue, 1.0f, 190, OpaUtils.INTERPOLATOR_40_OUT));
        animators.add(OpaUtils.getTranslationAnimatorX(mGreen, OpaUtils.INTERPOLATOR_40_OUT, 190));
        animators.add(OpaUtils.getTranslationAnimatorY(mGreen, OpaUtils.INTERPOLATOR_40_OUT, 190));
        animators.add(OpaUtils.getScaleAnimatorX(mGreen, 1.0f, 190, OpaUtils.INTERPOLATOR_40_OUT));
        animators.add(OpaUtils.getScaleAnimatorY(mGreen, 1.0f, 190, OpaUtils.INTERPOLATOR_40_OUT));
        animators.add(OpaUtils.getTranslationAnimatorX(mYellow, OpaUtils.INTERPOLATOR_40_OUT, 190));
        animators.add(OpaUtils.getTranslationAnimatorY(mYellow, OpaUtils.INTERPOLATOR_40_OUT, 190));
        animators.add(OpaUtils.getScaleAnimatorX(mYellow, 1.0f, 190, OpaUtils.INTERPOLATOR_40_OUT));
        animators.add(OpaUtils.getScaleAnimatorY(mYellow, 1.0f, 190, OpaUtils.INTERPOLATOR_40_OUT));
        animators.add(OpaUtils.getScaleAnimatorX(mWhite, 1.0f, 190, OpaUtils.INTERPOLATOR_40_OUT));
        animators.add(OpaUtils.getScaleAnimatorY(mWhite, 1.0f, 190, OpaUtils.INTERPOLATOR_40_OUT));
        animators.add(OpaUtils.getScaleAnimatorX(mWhiteCutout, 1.0f, 190, OpaUtils.INTERPOLATOR_40_OUT));
        animators.add(OpaUtils.getScaleAnimatorY(mWhiteCutout, 1.0f, 190, OpaUtils.INTERPOLATOR_40_OUT));
        animators.add(OpaUtils.getScaleAnimatorX(mHalo, 1.0f, 190, Interpolators.FAST_OUT_SLOW_IN));
        animators.add(OpaUtils.getScaleAnimatorY(mHalo, 1.0f, 190, Interpolators.FAST_OUT_SLOW_IN));
        animators.add(OpaUtils.getAlphaAnimator(mHalo, 1.0f, 190, Interpolators.FAST_OUT_SLOW_IN));
        getLongestAnim(animators).addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                OpaLayout.this.mCurrentAnimators.clear();
                OpaLayout.this.skipToStartingValue();
            }
        });
        return animators;
    }

    private ArraySet<Animator> getCollapseAnimatorSet() {
        Animator translationAnimatorY;
        ArraySet<Animator> animators = new ArraySet();
        if (mIsVertical) {
            translationAnimatorY = OpaUtils.getTranslationAnimatorY(mRed, OpaUtils.INTERPOLATOR_40_OUT, 133);
        } else {
            translationAnimatorY = OpaUtils.getTranslationAnimatorX(mRed, OpaUtils.INTERPOLATOR_40_OUT, 133);
        }
        animators.add(translationAnimatorY);
        animators.add(OpaUtils.getScaleAnimatorX(mRed, 1.0f, 200, OpaUtils.INTERPOLATOR_40_OUT));
        animators.add(OpaUtils.getScaleAnimatorY(mRed, 1.0f, 200, OpaUtils.INTERPOLATOR_40_OUT));
        if (mIsVertical) {
            translationAnimatorY = OpaUtils.getTranslationAnimatorY(mBlue, OpaUtils.INTERPOLATOR_40_OUT, 150);
        } else {
            translationAnimatorY = OpaUtils.getTranslationAnimatorX(mBlue, OpaUtils.INTERPOLATOR_40_OUT, 150);
        }
        animators.add(translationAnimatorY);
        animators.add(OpaUtils.getScaleAnimatorX(mBlue, 1.0f, 200, OpaUtils.INTERPOLATOR_40_OUT));
        animators.add(OpaUtils.getScaleAnimatorY(mBlue, 1.0f, 200, OpaUtils.INTERPOLATOR_40_OUT));
        if (mIsVertical) {
            translationAnimatorY = OpaUtils.getTranslationAnimatorY(mYellow, OpaUtils.INTERPOLATOR_40_OUT, 133);
        } else {
            translationAnimatorY = OpaUtils.getTranslationAnimatorX(mYellow, OpaUtils.INTERPOLATOR_40_OUT, 133);
        }
        animators.add(translationAnimatorY);
        animators.add(OpaUtils.getScaleAnimatorX(mYellow, 1.0f, 200, OpaUtils.INTERPOLATOR_40_OUT));
        animators.add(OpaUtils.getScaleAnimatorY(mYellow, 1.0f, 200, OpaUtils.INTERPOLATOR_40_OUT));
        if (mIsVertical) {
            translationAnimatorY = OpaUtils.getTranslationAnimatorY(mGreen, OpaUtils.INTERPOLATOR_40_OUT, 150);
        } else {
            translationAnimatorY = OpaUtils.getTranslationAnimatorX(mGreen, OpaUtils.INTERPOLATOR_40_OUT, 150);
        }
        animators.add(translationAnimatorY);
        animators.add(OpaUtils.getScaleAnimatorX(mGreen, 1.0f, 200, OpaUtils.INTERPOLATOR_40_OUT));
        animators.add(OpaUtils.getScaleAnimatorY(mGreen, 1.0f, 200, OpaUtils.INTERPOLATOR_40_OUT));
        Animator homeScaleX = OpaUtils.getScaleAnimatorX(mWhite, 1.0f, 150, Interpolators.FAST_OUT_SLOW_IN);
        Animator homeScaleY = OpaUtils.getScaleAnimatorY(mWhite, 1.0f, 150, Interpolators.FAST_OUT_SLOW_IN);
        Animator homeCutoutScaleX = OpaUtils.getScaleAnimatorX(mWhiteCutout, 1.0f, 150, Interpolators.FAST_OUT_SLOW_IN);
        Animator homeCutoutScaleY = OpaUtils.getScaleAnimatorY(mWhiteCutout, 1.0f, 150, Interpolators.FAST_OUT_SLOW_IN);
        Animator haloScaleX = OpaUtils.getScaleAnimatorX(mHalo, 1.0f, 150, Interpolators.FAST_OUT_SLOW_IN);
        Animator haloScaleY = OpaUtils.getScaleAnimatorY(mHalo, 1.0f, 150, Interpolators.FAST_OUT_SLOW_IN);
        Animator haloAlpha = OpaUtils.getAlphaAnimator(mHalo, 1.0f, 150, Interpolators.FAST_OUT_SLOW_IN);
        homeScaleX.setStartDelay(33);
        homeScaleY.setStartDelay(33);
        homeCutoutScaleX.setStartDelay(33);
        homeCutoutScaleY.setStartDelay(33);
        haloScaleX.setStartDelay(33);
        haloScaleY.setStartDelay(33);
        haloAlpha.setStartDelay(33);
        animators.add(homeScaleX);
        animators.add(homeScaleY);
        animators.add(homeCutoutScaleX);
        animators.add(homeCutoutScaleY);
        animators.add(haloScaleX);
        animators.add(haloScaleY);
        animators.add(haloAlpha);
        getLongestAnim(animators).addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                OpaLayout.this.mCurrentAnimators.clear();
                OpaLayout.this.skipToStartingValue();
            }
        });
        return animators;
    }

    private ArraySet<Animator> getLineAnimatorSet() {
        ArraySet<Animator> animators = new ArraySet();
        if (mIsVertical) {
            animators.add(OpaUtils.getDeltaAnimatorY(mRed, Interpolators.FAST_OUT_SLOW_IN, OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_ry), 225));
            animators.add(OpaUtils.getDeltaAnimatorX(mRed, Interpolators.FAST_OUT_SLOW_IN, OpaUtils.getPxVal(mResources, R.dimen.opa_line_y_translation), 133));
            animators.add(OpaUtils.getDeltaAnimatorY(mBlue, Interpolators.FAST_OUT_SLOW_IN, OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_bg), 225));
            animators.add(OpaUtils.getDeltaAnimatorY(mYellow, Interpolators.FAST_OUT_SLOW_IN, -OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_ry), 225));
            animators.add(OpaUtils.getDeltaAnimatorX(mYellow, Interpolators.FAST_OUT_SLOW_IN, -OpaUtils.getPxVal(mResources, R.dimen.opa_line_y_translation), 133));
            animators.add(OpaUtils.getDeltaAnimatorY(mGreen, Interpolators.FAST_OUT_SLOW_IN, -OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_bg), 225));
        } else {
            animators.add(OpaUtils.getDeltaAnimatorX(mRed, Interpolators.FAST_OUT_SLOW_IN, -OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_ry), 225));
            animators.add(OpaUtils.getDeltaAnimatorY(mRed, Interpolators.FAST_OUT_SLOW_IN, OpaUtils.getPxVal(mResources, R.dimen.opa_line_y_translation), 133));
            animators.add(OpaUtils.getDeltaAnimatorX(mBlue, Interpolators.FAST_OUT_SLOW_IN, -OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_bg), 225));
            animators.add(OpaUtils.getDeltaAnimatorX(mYellow, Interpolators.FAST_OUT_SLOW_IN, OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_ry), 225));
            animators.add(OpaUtils.getDeltaAnimatorY(mYellow, Interpolators.FAST_OUT_SLOW_IN, -OpaUtils.getPxVal(mResources, R.dimen.opa_line_y_translation), 133));
            animators.add(OpaUtils.getDeltaAnimatorX(mGreen, Interpolators.FAST_OUT_SLOW_IN, OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_bg), 225));
        }
        animators.add(OpaUtils.getScaleAnimatorX(mWhite, 0.0f, 83, HOME_DISAPPEAR_INTERPOLATOR));
        animators.add(OpaUtils.getScaleAnimatorY(mWhite, 0.0f, 83, HOME_DISAPPEAR_INTERPOLATOR));
        animators.add(OpaUtils.getScaleAnimatorX(mWhiteCutout, 0.0f, 83, HOME_DISAPPEAR_INTERPOLATOR));
        animators.add(OpaUtils.getScaleAnimatorY(mWhiteCutout, 0.0f, 83, HOME_DISAPPEAR_INTERPOLATOR));
        animators.add(OpaUtils.getScaleAnimatorX(mHalo, 0.0f, 83, HOME_DISAPPEAR_INTERPOLATOR));
        animators.add(OpaUtils.getScaleAnimatorY(mHalo, 0.0f, 83, HOME_DISAPPEAR_INTERPOLATOR));
        getLongestAnim(animators).addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                OpaLayout.this.startCollapseAnimation();
            }

            public void onAnimationCancel(Animator animation) {
                OpaLayout.this.mCurrentAnimators.clear();
            }
        });
        return animators;
    }

    public boolean getOpaEnabled() {
        return mOpaEnabled;
    }

    public void setOpaEnabled(boolean enabled) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Setting opa enabled to ");
        stringBuilder.append(enabled);
        Log.i("OpaLayout", stringBuilder.toString());
        mOpaEnabled = enabled;
        int visibility = 0;
        if (!enabled) {
            visibility = 4;
        }
        mBlue.setVisibility(visibility);
        mRed.setVisibility(visibility);
        mYellow.setVisibility(visibility);
        mGreen.setVisibility(visibility);
        mHalo.setVisibility(visibility);
    }

    private void cancelCurrentAnimation() {
        if (!mCurrentAnimators.isEmpty()) {
            for (int i = mCurrentAnimators.size() - 1; i >= 0; i--) {
                Animator a = (Animator) mCurrentAnimators.valueAt(i);
                a.removeAllListeners();
                a.cancel();
            }
            mCurrentAnimators.clear();
            mAnimationState = 0;
        }
        if (mGestureAnimatorSet != null) {
            mGestureAnimatorSet.cancel();
            mGestureState = 0;
        }
    }

    private void endCurrentAnimation() {
        if (!mCurrentAnimators.isEmpty()) {
            for (int i = mCurrentAnimators.size() - 1; i >= 0; i--) {
                Animator a = (Animator) mCurrentAnimators.valueAt(i);
                a.removeAllListeners();
                a.end();
            }
            mCurrentAnimators.clear();
        }
        mAnimationState = 0;
    }

    private Animator getLongestAnim(ArraySet<Animator> animators) {
        long longestDuration = Long.MIN_VALUE;
        Animator longestAnim = null;
        for (int i = animators.size() - 1; i >= 0; i--) {
            Animator a = (Animator) animators.valueAt(i);
            if (a.getTotalDuration() > longestDuration) {
                longestAnim = a;
                longestDuration = a.getTotalDuration();
            }
        }
        return longestAnim;
    }

    private void setDotsVisible() {
        int size = mAnimatedViews.size();
        for (int i = 0; i < size; i++) {
            ((View) mAnimatedViews.get(i)).setAlpha(1.0f);
        }
    }

    private void skipToStartingValue() {
        int size = mAnimatedViews.size();
        for (int i = 0; i < size; i++) {
            View v = (View) mAnimatedViews.get(i);
            v.setScaleY(1.0f);
            v.setScaleX(1.0f);
            v.setTranslationY(0.0f);
            v.setTranslationX(0.0f);
            v.setAlpha(0.0f);
        }
        mHalo.setAlpha(1.0f);
        mWhite.setAlpha(1.0f);
        mWhiteCutout.setAlpha(1.0f);
        mAnimationState = 0;
        mGestureState = 0;
    }

    public void setVertical(boolean vertical) {
        if (!(mIsVertical == vertical || mGestureAnimatorSet == null)) {
            mGestureAnimatorSet.cancel();
            mGestureAnimatorSet = null;
            skipToStartingValue();
        }
        mIsVertical = vertical;
        if (mIsVertical) {
            mTop = mGreen;
            mBottom = mBlue;
            mRight = mYellow;
            mLeft = mRed;

            mWhite.setPadding(mHome.getPaddingBottom(), mHome.getPaddingRight(), mHome.getPaddingBottom(), mHome.getPaddingLeft());
            mWhiteCutout.setPadding(mHome.getPaddingBottom(), mHome.getPaddingRight(), mHome.getPaddingBottom(), mHome.getPaddingLeft());
        } else {
            mTop = mRed;
            mBottom = mYellow;
            mLeft = mBlue;
            mRight = mGreen;

            mWhite.setPadding(mHome.getPaddingLeft(), mHome.getPaddingTop(), mHome.getPaddingRight(), mHome.getPaddingBottom());
            mWhiteCutout.setPadding(mHome.getPaddingLeft(), mHome.getPaddingTop(), mHome.getPaddingRight(), mHome.getPaddingBottom());
        }
    }

    public void setDarkIntensity(float intensity) {
        if (classKeyButtonDrawable.isInstance(mWhite.getDrawable())) {
            XposedHelpers.callMethod(mWhite.getDrawable(), "setDarkIntensity", intensity);
        }
        XposedHelpers.callMethod(mHalo.getDrawable(), "setDarkIntensity", intensity);
        mWhite.invalidate();
        mHalo.invalidate();
        XposedHelpers.callMethod(mHome, "setDarkIntensity", intensity);
    }

    public void onRelease() {
        if (mAnimationState == 0 && mGestureState == 1) {
            if (mGestureAnimatorSet != null) {
                mGestureAnimatorSet.cancel();
            }
            mGestureState = 0;
            startRetractAnimation();
        }
    }

    public void onProgress(float progress, int stage) {
        if (mGestureState != 2) {
            if (isAttachedToWindow()) {
                if (mAnimationState == 2) {
                    endCurrentAnimation();
                }
                if (mAnimationState == 0) {
                    if (mGestureAnimatorSet == null) {
                        mGestureAnimatorSet = getGestureAnimatorSet();
                        mGestureAnimationSetDuration = mGestureAnimatorSet.getTotalDuration();
                    }
                    mGestureAnimatorSet.setCurrentPlayTime((long) (((float) (mGestureAnimationSetDuration - 1)) * progress));
                    if (progress == 0.0f) {
                        mGestureState = 0;
                    } else {
                        mGestureState = 1;
                    }
                }
            }
        }
    }

    public void onResolve() {
        if (mAnimationState == 0) {
            if (mGestureState != 1 || mGestureAnimatorSet == null || mGestureAnimatorSet.isStarted()) {
                skipToStartingValue();
            } else {
                mGestureAnimatorSet.start();
                mGestureState = 2;
            }
        }
    }

    private AnimatorSet getGestureAnimatorSet() {
        if (mGestureLineSet != null) {
            mGestureLineSet.removeAllListeners();
            mGestureLineSet.cancel();
            return mGestureLineSet;
        }
        mGestureLineSet = new AnimatorSet();
        ObjectAnimator homeAnimator = OpaUtils.getScaleObjectAnimator(mWhite, 0.0f, 100, OpaUtils.INTERPOLATOR_40_OUT);
        ObjectAnimator homeCutoutAnimator = OpaUtils.getScaleObjectAnimator(mWhiteCutout, 0.0f, 100, OpaUtils.INTERPOLATOR_40_OUT);
        ObjectAnimator haloAnimator = OpaUtils.getScaleObjectAnimator(mHalo, 0.0f, 100, OpaUtils.INTERPOLATOR_40_OUT);
        homeAnimator.setStartDelay(50);
        homeCutoutAnimator.setStartDelay(50);
        mGestureLineSet.play(homeAnimator).with(homeCutoutAnimator).with(haloAnimator);
        mGestureLineSet.play(OpaUtils.getScaleObjectAnimator(mTop, 0.8f, 200, Interpolators.FAST_OUT_SLOW_IN)).with(homeAnimator).with(OpaUtils.getAlphaObjectAnimator(mRed, 1.0f, 50, 130, Interpolators.LINEAR)).with(OpaUtils.getAlphaObjectAnimator(mYellow, 1.0f, 50, 130, Interpolators.LINEAR)).with(OpaUtils.getAlphaObjectAnimator(mBlue, 1.0f, 50, 113, Interpolators.LINEAR)).with(OpaUtils.getAlphaObjectAnimator(mGreen, 1.0f, 50, 113, Interpolators.LINEAR)).with(OpaUtils.getScaleObjectAnimator(mBottom, 0.8f, 200, Interpolators.FAST_OUT_SLOW_IN)).with(OpaUtils.getScaleObjectAnimator(mLeft, 0.8f, 200, Interpolators.FAST_OUT_SLOW_IN)).with(OpaUtils.getScaleObjectAnimator(mRight, 0.8f, 200, Interpolators.FAST_OUT_SLOW_IN));
        Animator redAnimator;
        if (mIsVertical) {
            redAnimator = OpaUtils.getTranslationObjectAnimatorY(mRed, OpaUtils.INTERPOLATOR_40_40, OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_ry), mRed.getY() + OpaUtils.getDeltaDiamondPositionLeftY(), 350);
            redAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    OpaLayout.this.startCollapseAnimation();
                }
            });
            mGestureLineSet.play(redAnimator).with(haloAnimator).with(OpaUtils.getTranslationObjectAnimatorY(mBlue, OpaUtils.INTERPOLATOR_40_40, OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_bg), mBlue.getY() + OpaUtils.getDeltaDiamondPositionBottomY(mResources), 350)).with(OpaUtils.getTranslationObjectAnimatorY(mYellow, OpaUtils.INTERPOLATOR_40_40, -OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_ry), mYellow.getY() + OpaUtils.getDeltaDiamondPositionRightY(), 350)).with(OpaUtils.getTranslationObjectAnimatorY(mGreen, OpaUtils.INTERPOLATOR_40_40, -OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_bg), mGreen.getY() + OpaUtils.getDeltaDiamondPositionTopY(mResources), 350));
        } else {
            redAnimator = OpaUtils.getTranslationObjectAnimatorX(mRed, OpaUtils.INTERPOLATOR_40_40, -OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_ry), mRed.getX() + OpaUtils.getDeltaDiamondPositionTopX(), 350);
            redAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    OpaLayout.this.startCollapseAnimation();
                }
            });
            mGestureLineSet.play(redAnimator).with(homeAnimator).with(OpaUtils.getTranslationObjectAnimatorX(mBlue, OpaUtils.INTERPOLATOR_40_40, -OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_bg), mBlue.getX() + OpaUtils.getDeltaDiamondPositionLeftX(mResources), 350)).with(OpaUtils.getTranslationObjectAnimatorX(mYellow, OpaUtils.INTERPOLATOR_40_40, OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_ry), mYellow.getX() + OpaUtils.getDeltaDiamondPositionBottomX(), 350)).with(OpaUtils.getTranslationObjectAnimatorX(mGreen, OpaUtils.INTERPOLATOR_40_40, OpaUtils.getPxVal(mResources, R.dimen.opa_line_x_trans_bg), mGreen.getX() + OpaUtils.getDeltaDiamondPositionRightX(mResources), 350));
        }
        return mGestureLineSet;
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int haloWidth = mResources.getDimensionPixelSize(R.dimen.halo_diameter);
        int haloHeight = haloWidth;
        if (haloWidth % 2 != width % 2) {
            ++haloWidth;
        }
        if (haloHeight % 2 != height % 2) {
            ++haloHeight;
        }
        int widthSpec = MeasureSpec.makeMeasureSpec(haloWidth, MeasureSpec.EXACTLY);
        int heightSpec = MeasureSpec.makeMeasureSpec(haloHeight, MeasureSpec.EXACTLY);
        measureChild(mHalo, widthSpec, heightSpec);
    }
}
package xyz.paphonb.androidpify.aosp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.widget.NestedScrollView;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;

public class QSScrollLayout extends NestedScrollView {
    private int mLastMotionY;
    private final int mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop() / 2;

    public QSScrollLayout(Context context, View... children) {
        super(context);
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        for (View view : children) {
            linearLayout.addView(view);
        }
        addView(linearLayout);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!canScrollVertically(1)) {
            if (!canScrollVertically(-1)) {
                return false;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!canScrollVertically(1)) {
            if (!canScrollVertically(-1)) {
                return false;
            }
        }
        return super.onTouchEvent(ev);
    }

    public boolean shouldIntercept(MotionEvent ev) {
        if (ev.getY() > ((float) (getBottom()))) {
            return false;
        }
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mLastMotionY = (int) ev.getY();
        } else if (ev.getActionMasked() == 2) {
            if (mLastMotionY >= 0 && Math.abs(ev.getY() - ((float) mLastMotionY)) > ((float) mTouchSlop) && canScrollVertically(1)) {
                requestParentDisallowInterceptTouchEvent(true);
                mLastMotionY = (int) ev.getY();
                return true;
            }
        } else if (ev.getActionMasked() == 3 || ev.getActionMasked() == 1) {
            mLastMotionY = -1;
            requestParentDisallowInterceptTouchEvent(false);
        }
        return false;
    }

    private void requestParentDisallowInterceptTouchEvent(boolean disallowIntercept) {
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallowIntercept);
        }
    }
}
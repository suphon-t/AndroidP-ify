package xyz.paphonb.androidpify.aosp;

import android.content.Context;
import android.util.Log;
import android.view.View;

import de.robv.android.xposed.XposedHelpers;
import xyz.paphonb.androidpify.R;

@SuppressWarnings("unused")
public class StatusIconContainer extends AlphaOptimizedLinearLayout {

    private Class<?> classStatusBarIconView;
    private Class<?> classViewState;

    public StatusIconContainer(Context context, ClassLoader classLoader) {
        super(context, null);
        classStatusBarIconView = XposedHelpers.findClass("com.android.systemui.statusbar.StatusBarIconView", classLoader);
        classViewState = XposedHelpers.findClass("com.android.systemui.statusbar.stack.ViewState", classLoader);
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        float midY = ((float) getHeight()) / 2.0f;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            int width = child.getMeasuredWidth();
            int height = child.getMeasuredHeight();
            int top = (int) (midY - (((float) height) / 2.0f));
            child.layout(0, top, width, top + height);
        }
        resetViewStates();
        calculateIconTranslations();
        applyIconStates();
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            measureChild(getChildAt(i), widthMeasureSpec, heightMeasureSpec);
        }
    }

    public void onViewAdded(View child) {
        super.onViewAdded(child);
        child.setTag(R.id.status_bar_view_state_tag, XposedHelpers.newInstance(classViewState));
    }

    public void onViewRemoved(View child) {
        super.onViewRemoved(child);
        child.setTag(R.id.status_bar_view_state_tag, null);
    }

    private void calculateIconTranslations() {
        int i;
        float width = (float) getWidth();
        float translationX = width;
        float contentStart = (float) getPaddingStart();
        int childCount = getChildCount();
        int firstUnderflowIndex = -1;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("calculateIconTransitions: start=");
        stringBuilder.append(translationX);
        Log.d("StatusIconContainer", stringBuilder.toString());
        for (i = childCount - 1; i >= 0; i--) {
            Object childState;
            View child = getChildAt(i);
            if (classStatusBarIconView.isInstance(child)) {
                childState = getViewStateFromChild(child);
                if (childState != null) {
                    if (XposedHelpers.getBooleanField(XposedHelpers.callMethod(child, "getStatusBarIcon"), "visible")) {
                        float xTranslation = translationX - ((float) child.getWidth());
                        XposedHelpers.setFloatField(childState, "xTranslation", xTranslation);
                        if (xTranslation < contentStart && firstUnderflowIndex == -1) {
                            firstUnderflowIndex = i;
                        }
                        translationX -= (float) child.getWidth();
                    } else {
                        XposedHelpers.setBooleanField(childState, "hidden", true);
                    }
                }
            }
        }
        i = 0;
        if (firstUnderflowIndex != -1) {
            Object childState;
            for (int i2 = 0; i2 <= firstUnderflowIndex; i2++) {
                childState = getViewStateFromChild(getChildAt(i2));
                if (childState != null) {
                    XposedHelpers.setBooleanField(childState, "hidden", true);
                }
            }
        }
        if ((boolean) XposedHelpers.callMethod(this, "isLayoutRtl")) {
            while (i < childCount) {
                View child2 = getChildAt(i);
                Object state = getViewStateFromChild(child2);
                float xTranslation = (width - XposedHelpers.getFloatField(state, "xTranslation")) - ((float) child2.getWidth());
                XposedHelpers.setFloatField(state, "xTranslation", xTranslation);
                i++;
            }
        }
    }

    private void applyIconStates() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            Object vs = getViewStateFromChild(child);
            if (vs != null) {
                XposedHelpers.callMethod(vs, "applyToView", child);
            }
        }
    }

    private void resetViewStates() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            Object vs = getViewStateFromChild(child);
            if (vs != null) {
                XposedHelpers.callMethod(vs, "initFrom", child);
                XposedHelpers.setFloatField(vs, "alpha", 1.0f);
                if (classStatusBarIconView.isInstance(child)) {
                    boolean visible = !XposedHelpers.getBooleanField(XposedHelpers.callMethod(child, "getStatusBarIcon"), "visible");
                    XposedHelpers.setBooleanField(vs, "hidden", visible);
                } else {
                    XposedHelpers.setBooleanField(vs, "hidden", false);
                }
            }
        }
    }

    private static Object getViewStateFromChild(View child) {
        return child.getTag(R.id.status_bar_view_state_tag);
    }
}
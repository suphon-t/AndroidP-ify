package xyz.paphonb.androidpify.aosp;

import android.annotation.SuppressLint;
import android.content.*;
import android.graphics.Rect;
import android.os.*;
import android.os.IBinder.DeathRecipient;
import android.util.Log;
import com.android.systemui.shared.recents.IOverviewProxy;
import com.android.systemui.shared.recents.ISystemUiProxy;
import com.android.systemui.shared.system.GraphicBufferCompat;
import de.robv.android.xposed.XposedHelpers;
import xyz.paphonb.androidpify.MainHook;
import xyz.paphonb.androidpify.utils.ConfigUtils;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class OverviewProxyService implements CallbackController<OverviewProxyService.OverviewProxyListener> {
    private int mConnectionBackoffAttempts;
    private final List<OverviewProxyListener> mConnectionCallbacks = new ArrayList<>();
    private final Runnable mConnectionRunnable = new Runnable() {
        @Override
        public void run() {
            internalConnectToCurrentUser();
        }
    };
    private final Context mContext;
    private final Handler mHandler;
    private int mInteractionFlags;
    private boolean mIsEnabled;
    private final BroadcastReceiver mLauncherStateChangedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            OverviewProxyService.this.updateEnabledState();
            if (!OverviewProxyService.this.isEnabled()) {
                OverviewProxyService.this.mInteractionFlags = 0;
                Prefs.remove(OverviewProxyService.this.mContext, "QuickStepInteractionFlags");
            }
            OverviewProxyService.this.startConnectionToCurrentUser();
        }
    };
    private CharSequence mOnboardingText;
    private IOverviewProxy mOverviewProxy;
    private final ServiceConnection mOverviewServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service != null) {
                OverviewProxyService.this.mConnectionBackoffAttempts = 0;
                OverviewProxyService.this.mOverviewProxy = IOverviewProxy.Stub.asInterface(service);
                try {
                    service.linkToDeath(OverviewProxyService.this.mOverviewServiceDeathRcpt, 0);
                } catch (RemoteException e) {
                    Log.e("OverviewProxyService", "Lost connection to launcher service", e);
                }
                try {
                    OverviewProxyService.this.mOverviewProxy.onBind(OverviewProxyService.this.mSysUiProxy);
                } catch (RemoteException e2) {
                    Log.e("OverviewProxyService", "Failed to call onBind()", e2);
                }
                OverviewProxyService.this.notifyConnectionChanged();
            }
        }

        public void onServiceDisconnected(ComponentName name) {
        }
    };
    private final DeathRecipient mOverviewServiceDeathRcpt = new DeathRecipient() {
        @Override
        public void binderDied() {
            startConnectionToCurrentUser();
        }
    };
    private final Intent mQuickStepIntent;
    private final ComponentName mRecentsComponentName;
    private ISystemUiProxy mSysUiProxy = new ISystemUiProxy.Stub() {
        public GraphicBufferCompat screenshot(Rect sourceCrop, int width, int height, int minLayer, int maxLayer, boolean useIdentityTransform, int rotation) {
            long token = Binder.clearCallingIdentity();
            Class<?> surfaceControl = XposedHelpers.findClass("android.view.SurfaceControl", Binder.class.getClassLoader());
            try {
                GraphicBufferCompat graphicBufferCompat = new GraphicBufferCompat(
                        XposedHelpers.callStaticMethod(surfaceControl, "screenshotToBuffer", sourceCrop, width, height,
                                minLayer, maxLayer, useIdentityTransform, rotation));
                return graphicBufferCompat;
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void startScreenPinning(final int taskId) {
            long token = Binder.clearCallingIdentity();
            try {
                OverviewProxyService.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Class<?> StatusBar = XposedHelpers.findClass("com.android.systemui.statusbar.phone.StatusBar", mContext.getClassLoader());
                        Object statusBar = XposedHelpers.callMethod(mContext, "getComponent", StatusBar);
                        if (statusBar != null) {
                            XposedHelpers.callMethod(statusBar, "showScreenPinningRequest", taskId, false);
                        }
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void onSplitScreenInvoked() {
            long token = Binder.clearCallingIdentity();
            try {

            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        public void setRecentsOnboardingText(CharSequence text) {
            OverviewProxyService.this.mOnboardingText = text;
        }

        public void setInteractionState(final int flags) {
            long token = Binder.clearCallingIdentity();
            try {
                if (OverviewProxyService.this.mInteractionFlags != flags) {
                    OverviewProxyService.this.mInteractionFlags = flags;
                    OverviewProxyService.this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            setInteractionState(flags);
                        }
                    });
                }
                Prefs.putInt(OverviewProxyService.this.mContext, "QuickStepInteractionFlags", OverviewProxyService.this.mInteractionFlags);
                Binder.restoreCallingIdentity(token);
            } catch (Throwable th) {
                Prefs.putInt(OverviewProxyService.this.mContext, "QuickStepInteractionFlags", OverviewProxyService.this.mInteractionFlags);
                Binder.restoreCallingIdentity(token);
            }
        }
    };

    public interface OverviewProxyListener {
        void onConnectionChanged(boolean isConnected);

        void onQuickStepStarted();

        void onInteractionFlagsChanged(int flags);
    }

    public OverviewProxyService(Context context) {
        this.mContext = context;
        this.mHandler = new Handler();
        this.mConnectionBackoffAttempts = 0;
        this.mRecentsComponentName = getRecentsComponent();
        this.mQuickStepIntent = new Intent("android.intent.action.QUICKSTEP_SERVICE").setPackage(this.mRecentsComponentName.getPackageName());
        this.mInteractionFlags = Prefs.getInt(this.mContext, "QuickStepInteractionFlags", 0);
//        if (SystemServicesProxy.getInstance(context).isSystemUser(this.mDeviceProvisionedController.getCurrentUser())) {
            updateEnabledState();
//            this.mDeviceProvisionedController.addCallback(this.mDeviceProvisionedCallback);
            IntentFilter filter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
            filter.addDataScheme("package");
            filter.addDataSchemeSpecificPart(this.mRecentsComponentName.getPackageName(), 0);
            filter.addAction("android.intent.action.PACKAGE_CHANGED");
            this.mContext.registerReceiver(this.mLauncherStateChangedReceiver, filter);
//        }
    }

    private ComponentName getRecentsComponent() {
        String proxyPackage = ConfigUtils.INSTANCE.getMisc().getProxyOverviewPackage();
        if ("lcci".equals(proxyPackage)) {
            return new ComponentName(MainHook.PACKAGE_LAWNCHAIR, "com.android.quickstep.RecentsActivity");
        } else if ("opl".equals(proxyPackage)) {
            return new ComponentName(MainHook.PACKAGE_OP_LAUNCHER, "net.oneplus.quickstep.RecentsActivity");
        } else {
            return new ComponentName(MainHook.PACKAGE_LAUNCHER, "com.android.quickstep.RecentsActivity");
        }
    }

    public void startConnectionToCurrentUser() {
        if (this.mHandler.getLooper() != Looper.myLooper()) {
            this.mHandler.post(this.mConnectionRunnable);
        } else {
            internalConnectToCurrentUser();
        }
    }

    private void internalConnectToCurrentUser() {
        disconnectFromLauncherService();
        if (isEnabled()) {
            this.mHandler.removeCallbacks(this.mConnectionRunnable);
            boolean bound = false;
            try {
                bound = this.mContext.bindService(new Intent("android.intent.action.QUICKSTEP_SERVICE")
                        .setPackage(this.mRecentsComponentName.getPackageName()), this.mOverviewServiceConnection, Context.BIND_AUTO_CREATE);
            } catch (SecurityException e) {
                Log.e("OverviewProxyService", "Unable to bind because of security error", e);
            }
            if (!bound) {
                this.mHandler.postDelayed(this.mConnectionRunnable, (long) Math.scalb(5000.0f, this.mConnectionBackoffAttempts));
                this.mConnectionBackoffAttempts++;
            }
        }
    }

    public void addCallback(OverviewProxyListener listener) {
        this.mConnectionCallbacks.add(listener);
        listener.onConnectionChanged(this.mOverviewProxy != null);
        listener.onInteractionFlagsChanged(this.mInteractionFlags);
    }

    public void removeCallback(OverviewProxyListener listener) {
        this.mConnectionCallbacks.remove(listener);
    }

    public boolean shouldShowSwipeUpUI() {
        return isEnabled() && (this.mInteractionFlags & 1) == 0;
    }

    public boolean isEnabled() {
        return this.mIsEnabled;
    }

    public IOverviewProxy getProxy() {
        return this.mOverviewProxy;
    }

    public CharSequence getOnboardingText() {
        return this.mOnboardingText;
    }

    public int getInteractionFlags() {
        return this.mInteractionFlags;
    }

    private void disconnectFromLauncherService() {
        if (this.mOverviewProxy != null) {
            this.mOverviewProxy.asBinder().unlinkToDeath(this.mOverviewServiceDeathRcpt, 0);
            this.mContext.unbindService(this.mOverviewServiceConnection);
            this.mOverviewProxy = null;
            notifyConnectionChanged();
        }
    }

    private void notifyConnectionChanged() {
        for (int i = this.mConnectionCallbacks.size() - 1; i >= 0; i--) {
            ((OverviewProxyListener) this.mConnectionCallbacks.get(i)).onConnectionChanged(this.mOverviewProxy != null);
        }
    }

    public void notifyQuickStepStarted() {
        for (int i = this.mConnectionCallbacks.size() - 1; i >= 0; i--) {
            ((OverviewProxyListener) this.mConnectionCallbacks.get(i)).onQuickStepStarted();
        }
    }

    @SuppressLint("WrongConstant")
    private void updateEnabledState() {
        this.mIsEnabled = this.mContext.getPackageManager().resolveService(this.mQuickStepIntent, 262144) != null;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("OverviewProxyService state:");
        pw.print("  mConnectionBackoffAttempts=");
        pw.println(this.mConnectionBackoffAttempts);
        pw.print("  isCurrentUserSetup=");
        pw.print("  isConnected=");
        pw.println(this.mOverviewProxy != null);
    }
}
package com.android.systemui.shared.recents;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.MotionEvent;

public interface IOverviewProxy extends IInterface {

    public static abstract class Stub extends Binder implements IOverviewProxy {

        private static class Proxy implements IOverviewProxy {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            public void onBind(ISystemUiProxy sysUiProxy) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.shared.recents.IOverviewProxy");
                    _data.writeStrongBinder(sysUiProxy != null ? sysUiProxy.asBinder() : null);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onPreMotionEvent(int downHitTarget) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.shared.recents.IOverviewProxy");
                    _data.writeInt(downHitTarget);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onMotionEvent(MotionEvent event) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.shared.recents.IOverviewProxy");
                    if (event != null) {
                        _data.writeInt(1);
                        event.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onQuickScrubStart() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.shared.recents.IOverviewProxy");
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onQuickScrubEnd() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.shared.recents.IOverviewProxy");
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onQuickScrubProgress(float progress) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.shared.recents.IOverviewProxy");
                    _data.writeFloat(progress);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onOverviewToggle() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.shared.recents.IOverviewProxy");
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onOverviewShown(boolean triggeredFromAltTab) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.shared.recents.IOverviewProxy");
                    _data.writeInt(triggeredFromAltTab ? 1 : 0);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onOverviewHidden(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.shared.recents.IOverviewProxy");
                    _data.writeInt(triggeredFromAltTab ? 1 : 0);
                    _data.writeInt(triggeredFromHomeKey ? 1 : 0);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onQuickStep(MotionEvent event) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.shared.recents.IOverviewProxy");
                    if (event != null) {
                        _data.writeInt(1);
                        event.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        public static IOverviewProxy asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface("com.android.systemui.shared.recents.IOverviewProxy");
            if (iin == null || !(iin instanceof IOverviewProxy)) {
                return new Proxy(obj);
            }
            return (IOverviewProxy) iin;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = "com.android.systemui.shared.recents.IOverviewProxy";
            if (code != 1598968902) {
                MotionEvent _arg0 = null;
                boolean _arg1 = false;
                switch (code) {
                    case 1:
                        data.enforceInterface(descriptor);
                        onBind(com.android.systemui.shared.recents.ISystemUiProxy.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    case 2:
                        data.enforceInterface(descriptor);
                        onPreMotionEvent(data.readInt());
                        return true;
                    case 3:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (MotionEvent) MotionEvent.CREATOR.createFromParcel(data);
                        }
                        onMotionEvent(_arg0);
                        return true;
                    case 4:
                        data.enforceInterface(descriptor);
                        onQuickScrubStart();
                        return true;
                    case 5:
                        data.enforceInterface(descriptor);
                        onQuickScrubEnd();
                        return true;
                    case 6:
                        data.enforceInterface(descriptor);
                        onQuickScrubProgress(data.readFloat());
                        return true;
                    case 7:
                        data.enforceInterface(descriptor);
                        onOverviewToggle();
                        return true;
                    case 8:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        onOverviewShown(_arg1);
                        return true;
                    case 9:
                        data.enforceInterface(descriptor);
                        boolean _arg02 = data.readInt() != 0;
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        onOverviewHidden(_arg02, _arg1);
                        return true;
                    case 10:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (MotionEvent) MotionEvent.CREATOR.createFromParcel(data);
                        }
                        onQuickStep(_arg0);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    void onBind(ISystemUiProxy iSystemUiProxy) throws RemoteException;

    void onMotionEvent(MotionEvent motionEvent) throws RemoteException;

    void onOverviewHidden(boolean z, boolean z2) throws RemoteException;

    void onOverviewShown(boolean z) throws RemoteException;

    void onOverviewToggle() throws RemoteException;

    void onPreMotionEvent(int i) throws RemoteException;

    void onQuickScrubEnd() throws RemoteException;

    void onQuickScrubProgress(float f) throws RemoteException;

    void onQuickScrubStart() throws RemoteException;

    void onQuickStep(MotionEvent motionEvent) throws RemoteException;
}
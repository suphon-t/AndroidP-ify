package com.android.systemui.shared.recents;

import android.graphics.Rect;
import android.os.*;
import android.text.TextUtils;
import com.android.systemui.shared.system.GraphicBufferCompat;

public interface ISystemUiProxy extends IInterface {

    public static abstract class Stub extends Binder implements ISystemUiProxy {

        private static class Proxy implements ISystemUiProxy {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            public GraphicBufferCompat screenshot(Rect sourceCrop, int width, int height, int minLayer, int maxLayer, boolean useIdentityTransform, int rotation) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    GraphicBufferCompat _result;
                    _data.writeInterfaceToken("com.android.systemui.shared.recents.ISystemUiProxy");
                    if (sourceCrop != null) {
                        _data.writeInt(1);
                        sourceCrop.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(width);
                    _data.writeInt(height);
                    _data.writeInt(minLayer);
                    _data.writeInt(maxLayer);
                    _data.writeInt(useIdentityTransform ? 1 : 0);
                    _data.writeInt(rotation);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (GraphicBufferCompat) GraphicBufferCompat.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                    return null;
                }
            }

            public void startScreenPinning(int taskId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.shared.recents.ISystemUiProxy");
                    _data.writeInt(taskId);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setRecentsOnboardingText(CharSequence text) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.shared.recents.ISystemUiProxy");
                    if (text != null) {
                        _data.writeInt(1);
                        TextUtils.writeToParcel(text, _data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setInteractionState(int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.shared.recents.ISystemUiProxy");
                    _data.writeInt(flags);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onSplitScreenInvoked() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.shared.recents.ISystemUiProxy");
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, "com.android.systemui.shared.recents.ISystemUiProxy");
        }

        public static ISystemUiProxy asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface("com.android.systemui.shared.recents.ISystemUiProxy");
            if (iin == null || !(iin instanceof ISystemUiProxy)) {
                return new Proxy(obj);
            }
            return (ISystemUiProxy) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            Stub stub = this;
            Parcel parcel = data;
            Parcel parcel2 = reply;
            String descriptor = "com.android.systemui.shared.recents.ISystemUiProxy";
            Rect _arg0 = null;
            switch (code) {
                case 1:
                    parcel.enforceInterface(descriptor);
                    if (data.readInt() != 0) {
                        _arg0 = (Rect) Rect.CREATOR.createFromParcel(parcel);
                    }
                    GraphicBufferCompat _result = screenshot(_arg0, data.readInt(), data.readInt(), data.readInt(), data.readInt(), data.readInt() != 0, data.readInt());
                    reply.writeNoException();
                    if (_result != null) {
                        parcel2.writeInt(1);
                        _result.writeToParcel(parcel2, Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 2:
                    parcel.enforceInterface(descriptor);
                    startScreenPinning(data.readInt());
                    reply.writeNoException();
                    return true;
                case 4:
                    CharSequence _arg02 = null;
                    parcel.enforceInterface(descriptor);
                    if (data.readInt() != 0) {
                        _arg02 = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
                    }
                    setRecentsOnboardingText(_arg02);
                    reply.writeNoException();
                    return true;
                case 5:
                    parcel.enforceInterface(descriptor);
                    setInteractionState(data.readInt());
                    reply.writeNoException();
                    return true;
                case 6:
                    parcel.enforceInterface(descriptor);
                    onSplitScreenInvoked();
                    reply.writeNoException();
                    return true;
                case 1598968902:
                    parcel2.writeString(descriptor);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }
    }

    void onSplitScreenInvoked() throws RemoteException;

    GraphicBufferCompat screenshot(Rect rect, int i, int i2, int i3, int i4, boolean z, int i5) throws RemoteException;

    void setInteractionState(int i) throws RemoteException;

    void setRecentsOnboardingText(CharSequence charSequence) throws RemoteException;

    void startScreenPinning(int i) throws RemoteException;
}
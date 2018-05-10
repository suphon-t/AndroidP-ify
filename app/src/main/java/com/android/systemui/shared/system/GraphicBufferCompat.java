package com.android.systemui.shared.system;

import android.os.Parcel;
import android.os.Parcelable;
import de.robv.android.xposed.XposedHelpers;

public class GraphicBufferCompat implements Parcelable {
    public static final Creator<GraphicBufferCompat> CREATOR = new Creator<GraphicBufferCompat>() {
        public GraphicBufferCompat createFromParcel(Parcel in) {
            return new GraphicBufferCompat(in);
        }

        public GraphicBufferCompat[] newArray(int size) {
            return new GraphicBufferCompat[size];
        }
    };
    private Object mBuffer;

    public GraphicBufferCompat(Object buffer) {
        this.mBuffer = buffer;
    }

    public GraphicBufferCompat(Parcel in) {
        Class<?> graphicBuffer = XposedHelpers.findClass("android.graphics.GraphicBuffer", Parcel.class.getClassLoader());
        Parcelable.Creator creator = (Parcelable.Creator) XposedHelpers.getStaticObjectField(graphicBuffer, "CREATOR");
        this.mBuffer = creator.createFromParcel(in);
    }

    public void writeToParcel(Parcel dest, int flags) {
        XposedHelpers.callMethod(mBuffer, "writeToParcel", dest, flags);
    }

    public int describeContents() {
        return 0;
    }
}
package com.isee_project.travelsafe;

import android.os.Parcel;
import android.os.Parcelable;

public class Break implements Parcelable {

    private LatLng breakLocation;
    private String breakDuration;

    protected Break(Parcel in) {
        breakLocation = in.readParcelable(LatLng.class.getClassLoader());
        breakDuration = in.readString();
    }

    public static final Creator<Break> CREATOR = new Creator<Break>() {
        @Override
        public Break createFromParcel(Parcel in) {
            return new Break(in);
        }

        @Override
        public Break[] newArray(int size) {
            return new Break[size];
        }
    };

    public Break() {

    }

    public String getBreakDuration() {
        return breakDuration;
    }

    public LatLng getBreakLocation() {
        return breakLocation;
    }

    public void setBreakDuration(String breakDuration) {
        this.breakDuration = breakDuration;
    }

    public void setBreakLocation(LatLng breakLocation) {
        this.breakLocation = breakLocation;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(breakLocation, flags);
        dest.writeString(breakDuration);
    }
}

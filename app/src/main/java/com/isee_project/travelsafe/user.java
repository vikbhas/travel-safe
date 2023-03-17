package com.isee_project.travelsafe;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.HashMap;

public class user implements Parcelable {
    private String name;
    private String email;
    private String dp;
    private ArrayList<String> contacts;
    private String ward;
    private HashMap<String,String> preferences = new HashMap<String, String>();


    public user() {}

    public user(String name, String email, String dp, ArrayList<String> contacts, String ward, HashMap<String,String> preferences) {
        this.name = name;
        this.email = email;
        this.dp = dp;
        this.contacts = contacts;
        this.ward = ward;
        this.preferences = preferences;
    }

    protected user(Parcel in) {
        name = in.readString();
        email = in.readString();
        dp = in.readString();
        contacts = in.createStringArrayList();
        ward = in.readString();
        in.readMap(preferences,String.class.getClassLoader());
    }

    public static final Creator<user> CREATOR = new Creator<user>() {
        @Override
        public user createFromParcel(Parcel in) {
            return new user(in);
        }

        @Override
        public user[] newArray(int size) {
            return new user[size];
        }
    };

    public String getName() { return this.name; }
    public String getEmail() { return this.email; }
    public String getDp() { return this.dp; }
    public String encodedEmail() { return this.email.replace(".","%2E").replace("_","%5F").replace("@","%40");}
    public ArrayList<String> getFriends() { return contacts; }
    public String getWard() { return this.ward; }
    public HashMap<String, String> getPreferences() { return preferences; }

    public void setName(String name) { this.name = name;}
    public void setEmail(String email) { this.email = email; }
    public void setDp(String dp) { this.dp = dp;   }
    public void setFriends(ArrayList<String> friends) { this.contacts = friends; }
    public void setWard(String ward) { this.ward = ward; }
    public void setPreferences(HashMap<String, String> preferences) { this.preferences = preferences; }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(email);
        dest.writeString(dp);
        dest.writeStringList(contacts);
        dest.writeString(ward);
        dest.writeMap(preferences);
    }
}

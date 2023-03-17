package com.isee_project.travelsafe;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.isee_project.travelsafe.R;

public class NoInternetHelper {

    Context mContext;
    boolean internetConnected;

    public NoInternetHelper(Context context) {
        this.mContext = context;
    }

    public boolean isInternetConnected() {
        final ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        internetConnected = activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
        return internetConnected;
    }

    public void checkInternet() {
        isInternetConnected();
        if(internetConnected) {
            TextView noInternetAlert = (TextView)((Activity) mContext).findViewById(R.id.noInternetAlert);
            noInternetAlert.setVisibility(View.GONE);
        }
        else {
            TextView noInternetAlert = (TextView)((Activity) mContext).findViewById(R.id.noInternetAlert);
            noInternetAlert.setVisibility(View.VISIBLE);
        }
    }

    public BroadcastReceiver noInternetBroadcastCallback = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            internetConnected = intent.getBooleanExtra("InternetStatus", false);
            checkInternet();
        }
    };
}

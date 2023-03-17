package com.isee_project.travelsafe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import static android.content.Context.MODE_PRIVATE;

public class ETAReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        String wardName = intent.getStringExtra("wardName");
        SharedPreferences sharedPref = context.getSharedPreferences("travelsafe", MODE_PRIVATE);
        boolean atDestination =  sharedPref.getBoolean("atDestination", false);

    }
}

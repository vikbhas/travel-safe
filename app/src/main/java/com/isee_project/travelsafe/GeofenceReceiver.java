package com.isee_project.travelsafe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;


public class GeofenceReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "Geofence Triggered", Toast.LENGTH_SHORT).show();

    }

}

package com.isee_project.travelsafe;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import static android.content.Context.MODE_PRIVATE;
import static com.isee_project.travelsafe.App.CHANNEL_ID;

import com.isee_project.travelsafe.R;

public class WardETAReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String ward = intent.getStringExtra("ward");
        SharedPreferences sharedPref = context.getSharedPreferences("travelsafe", MODE_PRIVATE);
        boolean wardAtDestination =  sharedPref.getBoolean("wardAtDestination", false);
        if(!wardAtDestination) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setContentTitle("Journey alert")
                    .setContentText("You were supposed to be at the destination by now.")
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setSmallIcon(R.drawable.logo_travelsafe);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            SharedPreferences.Editor editor = context.getSharedPreferences("travelsafe", MODE_PRIVATE).edit();
            editor.putBoolean("wardAtDestination", true);
            editor.commit();
        }
    }
}

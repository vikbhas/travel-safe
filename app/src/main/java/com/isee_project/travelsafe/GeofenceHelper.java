package com.isee_project.travelsafe;

import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.maps.model.LatLng;

public class GeofenceHelper extends ContextWrapper {

    PendingIntent pendingIntent;

    public GeofenceHelper(Context base) {
        super(base);
    }

    public GeofencingRequest getGeofenceRequest(Geofence geofence) {
        return new GeofencingRequest.Builder()
                .addGeofence(geofence)
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .build();
    }

    public Geofence getGeofence(String ID, LatLng location, float radius, int transitionTypes) {
        return new Geofence.Builder()
                .setCircularRegion(location.latitude, location.longitude, radius)
                .setRequestId(ID)
                .setTransitionTypes(transitionTypes)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build();
    }

    public PendingIntent getPendingIntent() {
        if(pendingIntent != null) {
            return pendingIntent;
        }

        Intent intent = new Intent(this, GeofenceReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(this, 2607, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }

    public String getErrorString(Exception e) {
        return e.getLocalizedMessage();
    }
}

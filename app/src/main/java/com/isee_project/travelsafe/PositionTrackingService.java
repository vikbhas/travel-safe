package com.isee_project.travelsafe;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.isee_project.travelsafe.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.PolyUtil;
import com.google.maps.model.EncodedPolyline;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.isee_project.travelsafe.App.CHANNEL_ID;

public class PositionTrackingService extends Service {

    private static final String EXTRA_STARTED_FROM_NOTIFICATION = "PositionTrackingService.startedFromNotification";
    private static final int NOTIFICATION_ID = 12345678;
    private static final float SAFE_REACH_THRESHOLD = 50f;
    private LocationCallback mLocationCallback;
    private FusedLocationProviderClient mFusedLocationClient;
    private Handler mServiceHandler;
    private LocationRequest mLocationRequest;
    private Location mLocation;
    private NotificationManager mNotificationManager;
    private boolean mChangingConfiguration = false;
    private final IBinder mBinder = new LocalBinder();

    private static long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
    private static long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;


    private Location destination = new Location(LocationManager.GPS_PROVIDER);
    private Location sourceLocation = new Location(LocationManager.GPS_PROVIDER);
    private Location destinationLocation = new Location(LocationManager.GPS_PROVIDER);

    private user userDetails;
    private journey journey;

    private String email ;

    private EncodedPolyline sourceDestinationEncodedPolyline;
    List<LatLng> sourceDestinationPolylineList;

    private boolean routeDeviation;
	private long breakDuration ;

	private boolean reachedDestination = false;

    public PositionTrackingService() { }

    @Override
    public void onCreate() {

        SharedPreferences sharedPreferences = getSharedPreferences("travelsafe", MODE_PRIVATE);
        UPDATE_INTERVAL_IN_MILLISECONDS = Integer.parseInt(sharedPreferences.getString("locationUpdateFrequency", "1"))*1000;
        FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS;

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onNewLocation(locationResult.getLastLocation());
            }
        };

        createLocationRequest();
        getLastLocation();

        HandlerThread handlerThread = new HandlerThread("PositionTrackingService");
        handlerThread.start();
        mServiceHandler = new Handler(handlerThread.getLooper());
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            NotificationChannel mChannel =
                    new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);

            mNotificationManager.createNotificationChannel(mChannel);
        }

        LocalBroadcastManager.getInstance(PositionTrackingService.this).registerReceiver(stopActivity,
                new IntentFilter("stopJourneyBroadcast"));
        LocalBroadcastManager.getInstance(PositionTrackingService.this).registerReceiver(pauseJourneyActivity,
                new IntentFilter("pauseJourneyBroadcast"));
        LocalBroadcastManager.getInstance(PositionTrackingService.this).registerReceiver(playJourneyActivity,
                new IntentFilter("playJourneyBroadcast"));
    }

    private BroadcastReceiver stopActivity = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopEverything(context);
        }
    };
    private BroadcastReceiver pauseJourneyActivity = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Break breakPoint = new Break();
            breakPoint.setBreakLocation(new com.isee_project.travelsafe.LatLng(mLocation.getLatitude(),mLocation.getLongitude()));
            breakDuration = System.currentTimeMillis()/1000;
            FirebaseDatabase.getInstance().getReference("journeys").child(userDetails.encodedEmail()).child("breakList").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()){
                        GenericTypeIndicator<ArrayList<Break>> genericTypeIndicator = new GenericTypeIndicator<ArrayList<Break>>() {};
                        ArrayList<Break> breakList = dataSnapshot.getValue(genericTypeIndicator);
                        breakList.add(breakPoint);
                        FirebaseDatabase.getInstance().getReference("journeys").child(userDetails.encodedEmail()).child("breakList").setValue(breakList);
                    }
                    else {
                        ArrayList<Break> breakList = new ArrayList<Break>();
                        breakList.add(breakPoint);
                        FirebaseDatabase.getInstance().getReference("journeys").child(userDetails.encodedEmail()).child("breakList").setValue(breakList);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

        }
    };
    private BroadcastReceiver playJourneyActivity = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            FirebaseDatabase.getInstance().getReference("journeys").child(userDetails.encodedEmail()).child("breakList").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()){
                        GenericTypeIndicator<ArrayList<Break>> genericTypeIndicator = new GenericTypeIndicator<ArrayList<Break>>() {};
                        ArrayList<Break> breakList = dataSnapshot.getValue(genericTypeIndicator);
                        DecimalFormat df = new DecimalFormat("0.00");
                        breakList.get(breakList.size()-1).setBreakDuration(df.format((((System.currentTimeMillis()/1000)-breakDuration)/60.0)));
                        FirebaseDatabase.getInstance().getReference("journeys").child(userDetails.encodedEmail()).child("breakList").setValue(breakList);
                        Intent intent = new Intent("playJourneyDBBroadcast");
                        intent.putExtra("journeyDuration",(System.currentTimeMillis()/1000)-breakDuration);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });


        }
    };


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        boolean startedFromNotification = intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION,
                false);


        if (startedFromNotification) {
            removeLocationUpdates();
            stopSelf();
        }
        startForeground(NOTIFICATION_ID, getNotification());

        return START_NOT_STICKY;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mChangingConfiguration = true;
    }

    @Override
    public IBinder onBind(Intent intent) {

        userDetails = intent.getParcelableExtra("userDetails");
        journey = intent.getParcelableExtra("journey");
        email = journey.getWard();

        sourceDestinationPolylineList = new ArrayList<LatLng>();
        sourceDestinationEncodedPolyline = new EncodedPolyline(journey.getSourceDestinationEncodedPolyline());
        List<com.google.maps.model.LatLng> coords1 = sourceDestinationEncodedPolyline.decodePath();
        for (com.google.maps.model.LatLng coord1 : coords1) {
            sourceDestinationPolylineList.add(new LatLng(coord1.lat, coord1.lng));
        }


        destination.setLatitude(journey.getDestination().getLatitude());
        destination.setLongitude(journey.getDestination().getLongitude());
        sourceLocation.setLatitude(journey.getSource().getLatitude());
        sourceLocation.setLongitude(journey.getSource().getLongitude());
        destinationLocation.setLatitude(journey.getDestination().getLatitude());
        destinationLocation.setLongitude(journey.getDestination().getLongitude());

        stopForeground(true);
        mChangingConfiguration = false;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        stopForeground(true);
        mChangingConfiguration = false;
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (!mChangingConfiguration && requestingLocationUpdates(this)) {
            startForeground(NOTIFICATION_ID, getNotification());
        }
        return true;
    }

    @Override
    public void onDestroy() {
        mServiceHandler.removeCallbacksAndMessages(null);
    }


    public void requestLocationUpdates() {

        setRequestingLocationUpdates(this, true);
        startService(new Intent(getApplicationContext(), PositionTrackingService.class));
        try {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback, Looper.myLooper());
        } catch (SecurityException unlikely) {
            setRequestingLocationUpdates(this, false);
            Log.e("PositionTrackingService", "Lost location permission. Could not request updates. " + unlikely);
        }
    }

    private Notification getNotification() {
        Intent intent = new Intent(this, PositionTrackingService.class);

        CharSequence text = getLocationText(mLocation);

        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);

        PendingIntent servicePendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, IntroductionActivity.class), 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.locationTracking_journeyInProgress))
                .setContentText(getString(R.string.locationTracking_locationIsBeingTracked))
                .setPriority(Notification.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.user_location);

        return builder.build();
    }

    static String getLocationText(Location location) {
        return location == null ? "Unknown location" :
                "(" + location.getLatitude() + ", " + location.getLongitude() + ")";
    }
    static String getLocationTitle(Context context) {
        return "Locaiton tracking";
    }

    static boolean requestingLocationUpdates(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("requesting_locaction_updates", false);
    }

    static void setRequestingLocationUpdates(Context context, boolean requestingLocationUpdates) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean("requesting_locaction_update", requestingLocationUpdates)
                .apply();
    }

    public void removeLocationUpdates() {
        try {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            setRequestingLocationUpdates(this, false);
            stopSelf();
        } catch (SecurityException unlikely) {
            setRequestingLocationUpdates(this, true);

        }
    }

    private void getLastLocation() {
        try {
            mFusedLocationClient.getLastLocation()
                    .addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                mLocation = task.getResult();
                            } else {
                                Log.w("PositionTrackingService", "Failed to get location.");
                            }
                        }
                    });
        } catch (SecurityException unlikely) {
            Log.e("PositionTrackingService", "Lost location permission." + unlikely);
        }
    }

    private void onNewLocation(Location location) {

        mLocation = location;

        Intent intent = new Intent("PositionTrackingService.broadcast");
        intent.putExtra("PositionTrackingService.location", location);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("journeys/" + email);

        myRef.child("currentLocation").setValue(new LatLng(location.getLatitude(), location.getLongitude()));

        float distance = location.distanceTo(destination);
        float distance25P = sourceLocation.distanceTo(destinationLocation)*0.75F;
        float distance50P = sourceLocation.distanceTo(destinationLocation)*0.50F;
        float distance75P = sourceLocation.distanceTo(destinationLocation)*0.25F;
        if(distance < SAFE_REACH_THRESHOLD) {

            myRef.child("reachedDestination").setValue("true");
            reachedDestination = true;

            String message = "I have reached " + journey.getDestinationName() + " safely.";
            if(journey.getPhoneContacts()!= null)
                for(int i=0;i<journey.getPhoneContacts().size();i++){

                    String number = journey.getPhoneContacts().get(i);

                    SmsManager mySmsManager = SmsManager.getDefault();
                    mySmsManager.sendTextMessage(number,null, message, null, null);
                }

            for(String guardian:journey.getGuardiansList()) {
                database.getReference("users").child(guardian).child("ward").removeValue();
            }
            stopEverything(this);
            if (userDetails.getPreferences().get("journeyFinished").equals("true")) {
                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.user_location)
                        .setContentTitle(getString(R.string.locationTracking_journeyFinished))
                        .setContentText(getString(R.string.locationTracking_journeyHasComeToEnd))
                        .setPriority(Notification.PRIORITY_HIGH);
                mNotificationManager.notify(3, builder.build());
            }
        }
        if(distance < distance75P) {
            myRef.child("distance75P").setValue("true");
        }
        if(distance < distance50P) {
            myRef.child("distance50P").setValue("true");
        }
        if(distance < distance25P) {
            myRef.child("distance25P").setValue("true");
        }

        if(PolyUtil.isLocationOnPath(new LatLng(location.getLatitude(), location.getLongitude()), sourceDestinationPolylineList, false, 500)) {
            myRef.child("routeDeviation").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()) {
                        if(dataSnapshot.getValue(String.class).equals("true")) {
                            routeDeviation = false;
                            myRef.child("routeDeviation").setValue("false");

                            if (userDetails.getPreferences().get("journeyRouteDeviated").equals("true")) {
                                NotificationCompat.Builder builder = new NotificationCompat.Builder(PositionTrackingService.this, CHANNEL_ID)
                                        .setSmallIcon(R.drawable.user_location)
                                        .setContentTitle(getString(R.string.locationTracking_routeDeviation))
                                        .setContentText(getString(R.string.locationTracking_backToSelectedRoute))
                                        .setPriority(Notification.PRIORITY_HIGH);
                                mNotificationManager.notify(8, builder.build());
                            }
                        }
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
        else {
            if(!routeDeviation) {
                routeDeviation = true;
                myRef.child("routeDeviation").setValue("true");
                if (userDetails.getPreferences().get("journeyRouteDeviated").equals("true")) {
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                            .setSmallIcon(R.drawable.user_location)
                            .setContentTitle(getString(R.string.locationTracking_routeDeviation))
                            .setContentText(getString(R.string.locationTracking_deviatedFromSelectedRoute))
                            .setPriority(Notification.PRIORITY_HIGH);
                    mNotificationManager.notify(7, builder.build());
                }
            }
        }
    }

    public void stopEverything(Context context) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("journeys").child(userDetails.encodedEmail());
        Calendar calendar = Calendar.getInstance();
        Date currentTime = calendar.getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");
        String dateTime = dateFormat.format(currentTime);
        myRef.child("journeyCompleted").setValue("true");
		SharedPreferences.Editor editor = context.getSharedPreferences("travelsafe", MODE_PRIVATE).edit();
        editor.putBoolean("wardAtDestination", false);
        editor.commit();
        journey.setJourneyCompleted("true");
        journey.setPreviousJourneys(null);
        myRef.child("previousJourneys").child(dateTime.toString()).setValue(journey);
        if(journey.getGuardiansList()!=null)
        for(String guardian:journey.getGuardiansList()) {
            database.getReference("users").child(guardian).child("ward").removeValue();
        }
        if(!reachedDestination) {
            String message = "The journey had been aborted. Something is wrong, Please contact me.";
            if (journey.getPhoneContacts() != null)
                for (int i = 0; i < journey.getPhoneContacts().size(); i++) {
                    try {

                        String number = journey.getPhoneContacts().get(i);

                        SmsManager mySmsManager = SmsManager.getDefault();
                        mySmsManager.sendTextMessage(number, null, message, null, null);
                    } catch (Exception e) {
                        Log.i("FollowerAddedService", e.getLocalizedMessage());
                    }
                }
        }
        FirebaseDatabase.getInstance().getReference("journeys").child(userDetails.encodedEmail()).child("guardiansList").removeValue();
        FirebaseDatabase.getInstance().getReference("journeys").child(userDetails.encodedEmail()).child("phoneContacts").removeValue();
        FirebaseDatabase.getInstance().getReference("journeys").child(userDetails.encodedEmail()).child("contactNames").removeValue();

        Intent intent = new Intent("doneJourneyBroadcast");
        if(!reachedDestination) {
            intent.putExtra("finishLocation", new com.isee_project.travelsafe.LatLng(mLocation.getLatitude(), mLocation.getLongitude()));
        }
        intent.putExtra("journeyDone", true);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        removeLocationUpdates();
        stopForeground(true);
        mNotificationManager.cancel(NOTIFICATION_ID);
        stopSelf();
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public class LocalBinder extends Binder {
        PositionTrackingService getService() {
            return PositionTrackingService.this;
        }
    }

    public boolean serviceIsRunningInForeground(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(
                Integer.MAX_VALUE)) {
            if (getClass().getName().equals(service.service.getClassName())) {
                if (service.foreground) {
                    return true;
                }
            }
        }
        return false;
    }

}

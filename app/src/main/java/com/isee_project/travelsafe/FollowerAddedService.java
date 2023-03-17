package com.isee_project.travelsafe;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.isee_project.travelsafe.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import static com.isee_project.travelsafe.App.CHANNEL_ID;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class FollowerAddedService extends IntentService {

    String ward = "";
    AlarmManager alarmMgr;
    Intent broadcastIntent;
    PendingIntent alarmIntent;
    String reachedDestination = "false";
    String ETA = "";
    journey userJourney;
    user userDetails;
    journey journey;

    boolean routeDeviationFirstTime = true;

    private NotificationManager mNotificationManager;

    ValueEventListener preferencesVEL, initialCheckVEL, routeDeviationVEL, breakListVEL, SOSDistanceVEL, journeyCompletedVEL, userJourneyCompletedVEL, reachedDestinationVEL;
    boolean preferencesListening, initialCheckListening, routeDeviationListening, breakListListening, SOSDistanceListening, journeyCompletedListening, userJourneyCompletedListening, reachedDestinationListening;

    public FollowerAddedService() {
        super("FollowerAddedService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent,flags,startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        userDetails = intent.getParcelableExtra("userDetails");

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        BroadcastReceiver stopActivity = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(alarmIntent!=null) {
                    alarmMgr.cancel(alarmIntent);
                }
            }
        };

        LocalBroadcastManager.getInstance(FollowerAddedService.this).registerReceiver(stopActivity,
                new IntentFilter("stopJourneyBroadcast"));

        preferencesVEL = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    GenericTypeIndicator<HashMap<String,String>> genericTypeIndicator = new GenericTypeIndicator<HashMap<String, String>>() {};
                    userDetails.setPreferences(dataSnapshot.getValue(genericTypeIndicator));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        FirebaseDatabase.getInstance().getReference("users").child(userDetails.encodedEmail()).child("preferences").addValueEventListener(preferencesVEL);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("users").child(userDetails.encodedEmail()).child("ward");

        initialCheckVEL = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {

                    ward = dataSnapshot.getValue(String.class);
                    Intent intent = new Intent("guardAddedBroadcast");
                    intent.putExtra("ward", ward);
                    LocalBroadcastManager.getInstance(FollowerAddedService.this).sendBroadcast(intent);

                    database.getReference("journeys").child(ward).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists()) {
                                userJourney = dataSnapshot.getValue(journey.class);
                                ETA = userJourney.getETA();

                                alarmMgr = (AlarmManager)FollowerAddedService.this.getSystemService(Context.ALARM_SERVICE);
                                Intent intent = new Intent(FollowerAddedService.this, ETAReceiver.class);
                                intent.putExtra("wardName", userJourney.getWardName());

                                intent.putExtra("ward", userJourney.getWard());
                                alarmIntent = PendingIntent.getBroadcast(FollowerAddedService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");
                                Date date = new Date();
                                try {
                                    date = format.parse(ETA);

                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                                alarmMgr.set(AlarmManager.RTC,date.getTime(),alarmIntent);

                                routeDeviationVEL = new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if(dataSnapshot.exists() && !routeDeviationFirstTime) {
                                            try {
                                                if (dataSnapshot.getValue(String.class).equals("true")) {
                                                    NotificationCompat.Builder builder = new NotificationCompat.Builder(FollowerAddedService.this, CHANNEL_ID)
                                                            .setSmallIcon(R.drawable.logo_travelsafe)
                                                            .setContentTitle(getString(R.string.guardianAdded_routeDeviation))
                                                            .setContentText(userJourney.getWardName() + " " + getString(R.string.guardianAdded_deviatedFromSelectedRoute))
                                                            .setPriority(Notification.PRIORITY_HIGH);
                                                    mNotificationManager.notify(15, builder.build());

                                                } else {
                                                    NotificationCompat.Builder builder = new NotificationCompat.Builder(FollowerAddedService.this, CHANNEL_ID)
                                                            .setSmallIcon(R.drawable.logo_travelsafe)
                                                            .setContentTitle(getString(R.string.guardianAdded_routeDeviation))
                                                            .setContentText(userJourney.getWardName() + " " + getString(R.string.guardianAdded_backToSelectedRoute))
                                                            .setPriority(Notification.PRIORITY_HIGH);
                                                    mNotificationManager.notify(14, builder.build());
                                                }
                                            } catch(Exception e) {
                                                Log.i("FollowerAddedService", e.getLocalizedMessage());
                                            }
                                        }
                                        else {
                                            routeDeviationFirstTime = false;
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                };

                                FirebaseDatabase.getInstance().getReference("journeys").child(ward).child("routeDeviation").addValueEventListener(routeDeviationVEL);

                                breakListVEL = new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if(dataSnapshot.exists()) {
                                            GenericTypeIndicator<ArrayList<Break>> genericTypeIndicator = new GenericTypeIndicator<ArrayList<Break>>() {};
                                            ArrayList<Break> breakArrayList = dataSnapshot.getValue(genericTypeIndicator);
                                            if(breakArrayList.get(breakArrayList.size()-1).getBreakDuration()==null) {
                                                alarmMgr.cancel(alarmIntent);
                                                SharedPreferences sharedPref = getSharedPreferences("travelsafe", Context.MODE_PRIVATE);
                                                if (userDetails.getPreferences().get("journeyPaused").equals("true")) {
                                                    NotificationCompat.Builder builder = new NotificationCompat.Builder(FollowerAddedService.this, CHANNEL_ID)
                                                            .setContentTitle(getString(R.string.guardianAdded_journeyPaused))
                                                            .setContentText(userJourney.getWardName() + " " + getString((R.string.guardianAdded_takingBreak)))
                                                            .setPriority(Notification.PRIORITY_HIGH)
                                                            .setSmallIcon(R.drawable.logo_travelsafe);


                                                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(FollowerAddedService.this);
                                                    notificationManager.notify(12, builder.build());
                                                }
                                            }
                                            else {
                                                FirebaseDatabase.getInstance().getReference("journeys").child(ward).child("eta").addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                        if(dataSnapshot.exists()) {
                                                            SharedPreferences sharedPref = getSharedPreferences("travelsafe", Context.MODE_PRIVATE);
                                                            if (userDetails.getPreferences().get("journeyResumed").equals("true")) {
                                                                NotificationCompat.Builder builder = new NotificationCompat.Builder(FollowerAddedService.this, CHANNEL_ID)
                                                                        .setContentTitle(getString(R.string.guardianAdded_journeyResumed))
                                                                        .setContentText(userJourney.getWardName() + " " + getString(R.string.guardianAdded_resumedJourney))
                                                                        .setPriority(Notification.PRIORITY_HIGH)
                                                                        .setSmallIcon(R.drawable.logo_travelsafe);

                                                                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(FollowerAddedService.this);
                                                                notificationManager.notify(13, builder.build());
                                                                String ETA = dataSnapshot.getValue(String.class);
                                                            }

                                                            alarmMgr = (AlarmManager) FollowerAddedService.this.getSystemService(Context.ALARM_SERVICE);
                                                            broadcastIntent = new Intent(FollowerAddedService.this, ETAReceiver.class);
                                                            broadcastIntent.putExtra("userJourney", userJourney);
                                                            broadcastIntent.putExtra("ward", userJourney.getWard());
                                                            broadcastIntent.putExtra("wardName", userJourney.getWardName());
                                                            alarmIntent = PendingIntent.getBroadcast(FollowerAddedService.this, 0, broadcastIntent, 0);
                                                            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");

                                                            Date date = new Date();
                                                            Calendar cal = Calendar.getInstance();
                                                            try {
                                                                date = format.parse(ETA);
                                                                cal.setTime(date);
                                                                cal.add(Calendar.SECOND, (int)(Double.parseDouble(breakArrayList.get(breakArrayList.size()-1).getBreakDuration())*60));

                                                            } catch (ParseException e) {
                                                                e.printStackTrace();
                                                            }
                                                            alarmMgr.set(AlarmManager.RTC, date.getTime(), alarmIntent);
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                                    }
                                                });
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                };

                                FirebaseDatabase.getInstance().getReference("journeys").child(ward).child("breakList").addValueEventListener(breakListVEL);

                                DatabaseReference reference1 = FirebaseDatabase.getInstance().getReference("journeys").child(ward);

                                SOSDistanceVEL = new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        journey  = dataSnapshot.getValue(journey.class);
                                        assert journey != null;
                                        boolean SOS = journey.getSOS();
                                        String distance25P = journey.getDistance25P();
                                        String distance50P = journey.getDistance50P();
                                        String distance75P = journey.getDistance75P();
                                        if(SOS)
                                        {
                                            reference1.child("sos").setValue(false);
                                            NotificationCompat.Builder builder = new NotificationCompat.Builder(FollowerAddedService.this, CHANNEL_ID)
                                                    .setContentTitle(getString(R.string.guardianAdded_warning))
                                                    .setContentText(journey.getWardName() + " " + getString(R.string.guardianAdded_wardInDanger))
                                                    .setPriority(Notification.PRIORITY_HIGH)
                                                    .setSmallIcon(R.drawable.logo_travelsafe);

                                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(FollowerAddedService.this);
                                            notificationManager.notify(7, builder.build());
                                        }
                                        SharedPreferences sharedPref = getSharedPreferences("travelsafe", Context.MODE_PRIVATE);
                                        boolean distance25PPreference =  sharedPref.getBoolean("distance25P", false);
                                        boolean distance50PPreference =  sharedPref.getBoolean("distance50P", false);
                                        boolean distance75PPreference =  sharedPref.getBoolean("distance75P", false);
                                        if(distance25P.equals("true")) {
                                            if (userDetails.getPreferences().get("distance25P").equals("true") && !distance25PPreference) {
                                                NotificationCompat.Builder builder = new NotificationCompat.Builder(FollowerAddedService.this, CHANNEL_ID)
                                                        .setSmallIcon(R.drawable.user_location)
                                                        .setContentTitle(getString(R.string.locationTrakcing_journeyProgress))
                                                        .setContentText(journey.getWardName() + " " + getString(R.string.locationTracking_distance25P))
                                                        .setPriority(Notification.PRIORITY_HIGH);
                                                mNotificationManager.notify(3, builder.build());

                                                SharedPreferences.Editor editor = getSharedPreferences("travelsafe", MODE_PRIVATE).edit();
                                                editor.putBoolean("distance25P", true);
                                                editor.commit();
                                            }
                                        }
                                        if(distance50P.equals("true")){
                                            if (userDetails.getPreferences().get("distance50P").equals("true") && !distance50PPreference) {
                                                NotificationCompat.Builder builder = new NotificationCompat.Builder(FollowerAddedService.this, CHANNEL_ID)
                                                        .setSmallIcon(R.drawable.user_location)
                                                        .setContentTitle(getString(R.string.locationTrakcing_journeyProgress))
                                                        .setContentText(journey.getWardName() + " " + getString(R.string.locationTracking_distance50P))
                                                        .setPriority(Notification.PRIORITY_HIGH);
                                                mNotificationManager.notify(3, builder.build());

                                                SharedPreferences.Editor editor = getSharedPreferences("travelsafe", MODE_PRIVATE).edit();
                                                editor.putBoolean("distance50P", true);
                                                editor.commit();
                                            }
                                        }
                                        if(distance75P.equals("true")){
                                            if (userDetails.getPreferences().get("distance75P").equals("true") && !distance75PPreference) {
                                                NotificationCompat.Builder builder = new NotificationCompat.Builder(FollowerAddedService.this, CHANNEL_ID)
                                                        .setSmallIcon(R.drawable.user_location)
                                                        .setContentTitle(getString(R.string.locationTrakcing_journeyProgress))
                                                        .setContentText(journey.getWardName() + " " + getString(R.string.locationTracking_distance75P))
                                                        .setPriority(Notification.PRIORITY_HIGH);
                                                mNotificationManager.notify(3, builder.build());
                                            }
                                            SharedPreferences.Editor editor = getSharedPreferences("travelsafe", MODE_PRIVATE).edit();
                                            editor.putBoolean("distance75P", true);
                                            editor.commit();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                };

                                reference1.addValueEventListener(SOSDistanceVEL);

                                reachedDestinationVEL = new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                        if(dataSnapshot.exists()) {

                                            if (dataSnapshot.getValue(String.class).equals("true")) {
                                                reachedDestination = "true";
                                                alarmMgr.cancel(alarmIntent);

                                                if (userDetails.getPreferences().get("journeySuccessful").equals("true")) {
                                                    NotificationCompat.Builder builder = new NotificationCompat.Builder(FollowerAddedService.this, CHANNEL_ID)
                                                            .setContentTitle(getString(R.string.guardianAdded_journeySuccessful))
                                                            .setContentText(userJourney.getWardName() + " " + getString(R.string.guardianAdded_wardReachedDestinationSafely))
                                                            .setPriority(Notification.PRIORITY_HIGH)
                                                            .setSmallIcon(R.drawable.logo_travelsafe);

                                                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(FollowerAddedService.this);
                                                    notificationManager.notify(5, builder.build());

                                                    SharedPreferences.Editor editor = getSharedPreferences("travelsafe", MODE_PRIVATE).edit();
                                                    editor.putBoolean("guardianAdded", false);
                                                    editor.putBoolean("atDestination", false);
                                                    editor.putBoolean("takingBreak", false);
                                                    editor.putBoolean("journeyResumed", false);
                                                    editor.putBoolean("distance25P", false);
                                                    editor.putBoolean("distance50P", false);
                                                    editor.putBoolean("distance75P", false);

                                                    if(routeDeviationVEL!=null)
                                                        FirebaseDatabase.getInstance().getReference("journeys").child(ward).child("routeDeviation").removeEventListener(routeDeviationVEL);
                                                    if(SOSDistanceVEL!=null)
                                                        FirebaseDatabase.getInstance().getReference("journeys").child(ward).removeEventListener(SOSDistanceVEL);
                                                    if(journeyCompletedVEL!=null)
                                                        FirebaseDatabase.getInstance().getReference("journeys").child(ward).child("journeyCompleted").removeEventListener(journeyCompletedVEL);
                                                    if(userJourneyCompletedVEL!=null)
                                                        FirebaseDatabase.getInstance().getReference("journeys").child(userDetails.encodedEmail()).child("journeyCompleted").removeEventListener(userJourneyCompletedVEL);
                                                    if(reachedDestinationVEL!=null)
                                                        FirebaseDatabase.getInstance().getReference("journeys").child(ward).child("reachedDestination").removeEventListener(reachedDestinationVEL);
                                                    if(breakListVEL!=null)
                                                        FirebaseDatabase.getInstance().getReference("journeys").child(ward).child("breakList").removeEventListener(breakListVEL);

                                                    editor.commit();
                                                    routeDeviationFirstTime = true;
                                                }

                                            }

                                            else {
                                                journeyCompletedVEL = new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                        if(dataSnapshot.exists()) {

                                                            if(dataSnapshot.getValue(String.class).equals("true") && reachedDestination.equals("false")) {
                                                                alarmMgr.cancel(alarmIntent);

                                                                if (userDetails.getPreferences().get("journeyStopped").equals("true")) {
                                                                    NotificationCompat.Builder builder = new NotificationCompat.Builder(FollowerAddedService.this, CHANNEL_ID)
                                                                            .setContentTitle(getString(R.string.guardianAdded_journeyStopped))
                                                                            .setContentText(userJourney.getWardName() + " " + getString(R.string.guardianAdded_wardStoppedJourney))
                                                                            .setPriority(Notification.PRIORITY_HIGH)
                                                                            .setSmallIcon(R.drawable.logo_travelsafe);

                                                                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(FollowerAddedService.this);
                                                                    notificationManager.notify(6, builder.build());

                                                                    SharedPreferences.Editor editor = getSharedPreferences("travelsafe", MODE_PRIVATE).edit();
                                                                    editor.putBoolean("guardianAdded", false);
                                                                    editor.putBoolean("atDestination", false);
                                                                    editor.putBoolean("takingBreak", false);
                                                                    editor.putBoolean("journeyResumed", false);
                                                                    editor.putBoolean("distance25P", false);
                                                                    editor.putBoolean("distance50P", false);
                                                                    editor.putBoolean("distance75P", false);

                                                                    if(routeDeviationVEL!=null)
                                                                        FirebaseDatabase.getInstance().getReference("journeys").child(ward).child("routeDeviation").removeEventListener(routeDeviationVEL);
                                                                    if(SOSDistanceVEL!=null)
                                                                        FirebaseDatabase.getInstance().getReference("journeys").child(ward).removeEventListener(SOSDistanceVEL);
                                                                    if(journeyCompletedVEL!=null)
                                                                        FirebaseDatabase.getInstance().getReference("journeys").child(ward).child("journeyCompleted").removeEventListener(journeyCompletedVEL);
                                                                    if(userJourneyCompletedVEL!=null)
                                                                        FirebaseDatabase.getInstance().getReference("journeys").child(userDetails.encodedEmail()).child("journeyCompleted").removeEventListener(userJourneyCompletedVEL);
                                                                    if(reachedDestinationVEL!=null)
                                                                        FirebaseDatabase.getInstance().getReference("journeys").child(ward).child("reachedDestination").removeEventListener(reachedDestinationVEL);
                                                                    if(breakListVEL!=null)
                                                                        FirebaseDatabase.getInstance().getReference("journeys").child(ward).child("breakList").removeEventListener(breakListVEL);

                                                                    editor.commit();
                                                                    routeDeviationFirstTime = true;
                                                                }
                                                            }
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                                    }
                                                };

                                                FirebaseDatabase.getInstance().getReference("journeys").child(ward).child("journeyCompleted").addValueEventListener(journeyCompletedVEL);

                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                };

                                FirebaseDatabase.getInstance().getReference("journeys").child(ward).child("reachedDestination").addValueEventListener(reachedDestinationVEL);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }

                    });
                }
                else {
                    userJourneyCompletedVEL = new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists()) {
                                if(dataSnapshot.getValue(String.class).equals("false")) {
                                    FirebaseDatabase.getInstance().getReference("journeys").child(userDetails.encodedEmail()).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                            journey journey = dataSnapshot.getValue(journey.class);


                                            alarmMgr = (AlarmManager) FollowerAddedService.this.getSystemService(Context.ALARM_SERVICE);
                                            broadcastIntent = new Intent(FollowerAddedService.this, WardETAReceiver.class);
                                            broadcastIntent.putExtra("ward", userDetails.encodedEmail());
                                            alarmIntent = PendingIntent.getBroadcast(FollowerAddedService.this, 0, broadcastIntent, 0);
                                            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");

                                            Date date = new Date();
                                            try {
                                                date = format.parse(journey.getETA());

                                            } catch (ParseException e) {
                                                e.printStackTrace();
                                            }
                                            alarmMgr.set(AlarmManager.RTC, date.getTime(), alarmIntent);
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                                }
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    };

                    FirebaseDatabase.getInstance().getReference("journeys").child(userDetails.encodedEmail()).child("journeyCompleted").addValueEventListener(userJourneyCompletedVEL);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };

        myRef.addValueEventListener(initialCheckVEL);

        BroadcastReceiver userLoggedOut = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if(preferencesVEL!=null)
                    FirebaseDatabase.getInstance().getReference("users").child(userDetails.encodedEmail()).child("preferences").removeEventListener(preferencesVEL);
                if(initialCheckVEL!=null)
                    FirebaseDatabase.getInstance().getReference("users").child(userDetails.encodedEmail()).child("ward").removeEventListener(initialCheckVEL);
                if(routeDeviationVEL!=null)
                    FirebaseDatabase.getInstance().getReference("journeys").child(ward).child("routeDeviation").removeEventListener(routeDeviationVEL);
                if(SOSDistanceVEL!=null)
                    FirebaseDatabase.getInstance().getReference("journeys").child(ward).removeEventListener(SOSDistanceVEL);
                if(journeyCompletedVEL!=null)
                    FirebaseDatabase.getInstance().getReference("journeys").child(ward).child("journeyCompleted").removeEventListener(journeyCompletedVEL);
                if(userJourneyCompletedVEL!=null)
                    FirebaseDatabase.getInstance().getReference("journeys").child(userDetails.encodedEmail()).child("journeyCompleted").removeEventListener(userJourneyCompletedVEL);
                if(reachedDestinationVEL!=null)
                    FirebaseDatabase.getInstance().getReference("journeys").child(ward).child("reachedDestination").removeEventListener(reachedDestinationVEL);
                if(breakListVEL!=null)
                    FirebaseDatabase.getInstance().getReference("journeys").child(ward).child("breakList").removeEventListener(breakListVEL);

                stopSelf();
            }
        };
        LocalBroadcastManager.getInstance(FollowerAddedService.this).registerReceiver(userLoggedOut,
                new IntentFilter("LogoutBroadcast"));

        LocalBroadcastManager.getInstance(FollowerAddedService.this).registerReceiver(pauseJourneyActivity,
                new IntentFilter("pauseJourneyBroadcast"));
        LocalBroadcastManager.getInstance(FollowerAddedService.this).registerReceiver(playJourneyDBActivity,
                new IntentFilter("playJourneyDBBroadcast"));
    }

    public BroadcastReceiver pauseJourneyActivity = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            alarmMgr.cancel(alarmIntent);
        }
    };
    public BroadcastReceiver playJourneyDBActivity = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long journeyDuration = intent.getLongExtra("journeyDuration",0);

            alarmMgr = (AlarmManager)FollowerAddedService.this.getSystemService(Context.ALARM_SERVICE);
            Intent broadcastIntent = new Intent(FollowerAddedService.this, WardETAReceiver.class);
            broadcastIntent.putExtra("ward", userDetails.encodedEmail());
            alarmIntent = PendingIntent.getBroadcast(FollowerAddedService.this, 0, broadcastIntent, 0);

            FirebaseDatabase.getInstance().getReference("journeys").child(userDetails.encodedEmail()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String ETA = dataSnapshot.getValue(journey.class).getETA();
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");
                    Date date = new Date();
                    Calendar cal = Calendar.getInstance();
                    try {
                        date = format.parse(ETA);
                        cal.setTime(date);
                        cal.add(Calendar.SECOND, (int) journeyDuration);
                        Log.i("FollowerAddedService","Journey ETA: " + date);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    alarmMgr.set(AlarmManager.RTC, cal.getTime().getTime(),alarmIntent);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });


        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}

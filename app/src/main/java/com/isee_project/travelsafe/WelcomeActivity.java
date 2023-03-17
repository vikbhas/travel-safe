package com.isee_project.travelsafe;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.isee_project.travelsafe.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Locale;

import static com.isee_project.travelsafe.App.CHANNEL_ID;

public class WelcomeActivity extends AppCompatActivity {

    FirebaseDatabase database = FirebaseDatabase.getInstance();

    user userDetails;
    journey journeyDetails;
    boolean pressAgaintoExit = false;
    boolean journeyActivity = false;
    String wardName;
    boolean toastCancel = true;
    boolean firstTime = false;


    @SuppressLint("ClickableViewAccessibility")
    void setJourneyActivity() {
        ((Button)findViewById(R.id.startJourney)).setText(getString(R.string.welcome_trackJourney));
        journeyActivity = true;
        ((Button) findViewById(R.id.startJourney)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent journeyIntent = new Intent(WelcomeActivity.this, JourneyActivity.class);
                journeyIntent.putExtra("userDetails", userDetails);
                journeyIntent.putExtra("userJourney", journeyDetails);
                startActivity(journeyIntent);
            }
        });

    }

    void setTrackTravelActivity() {
        ((Button)findViewById(R.id.startJourney)).setText(getString(R.string.welcome_trackWard) + " " + wardName);
        journeyActivity = true;
        ((Button)findViewById(R.id.startJourney)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent TrackTravelActivity = new Intent(WelcomeActivity.this, TrackTravelActivity.class);
                TrackTravelActivity.putExtra("userDetails", userDetails);
                TrackTravelActivity.putExtra("userJourney", journeyDetails);
                startActivity(TrackTravelActivity);
            }
        });
    }

    void setFollowerActivity() {
        ((Button)findViewById(R.id.startJourney)).setText(getString(R.string.welcome_createJourney));
        ((Button)findViewById(R.id.startJourney)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent guardIntent = new Intent(WelcomeActivity.this, FollowerActivity.class);
                guardIntent.putExtra("userDetails", userDetails);
                startActivity(guardIntent);
            }
        });
    }

    ValueEventListener journeyCheck = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            if(dataSnapshot.exists()) {
                journeyDetails =  dataSnapshot.getValue(journey.class);
                if(journeyDetails.getJourneyCompleted() == null) {
                    setFollowerActivity();
                }
                else if(journeyDetails.getJourneyCompleted().equals("false")) {
                    setJourneyActivity();
                }
                else {
                    DatabaseReference myRef = database.getReference("users").child(userDetails.encodedEmail());
                    myRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                userDetails = dataSnapshot.getValue(user.class);
                                if(userDetails.getWard()!=null) {
                                    DatabaseReference myRef = database.getReference("journeys").child(userDetails.getWard());
                                    myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            journeyDetails = dataSnapshot.getValue(journey.class);
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                                    database.getReference().child("users").child(userDetails.getWard()).addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            user ward = dataSnapshot.getValue(user.class);
                                            wardName = ward.getName();
                                            setTrackTravelActivity();

                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                                }
                                else {
                                    setFollowerActivity();
                                }
                            }
                            else {
                                setFollowerActivity();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }
            else {
                DatabaseReference myRef = database.getReference("users").child(userDetails.encodedEmail()).child("ward");
                myRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String ward = dataSnapshot.getValue(String.class);
                            DatabaseReference myRef = database.getReference("journeys").child(ward);
                            myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    journeyDetails = dataSnapshot.getValue(journey.class);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                            database.getReference().child("users").child(userDetails.getWard()).addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    user ward = dataSnapshot.getValue(user.class);
                                    wardName = ward.getName();
                                    setTrackTravelActivity();

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                        }
                        else {
                            setFollowerActivity();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            Log.w("signIn", "loadPost:onCancelled", databaseError.toException());
        }
    };

    @Override
    public void onBackPressed() {
        if(pressAgaintoExit)
        {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
        }
        else
        {
            pressAgaintoExit=true;
            Toast.makeText(this,getString(R.string.welcome_pressAgainToExit), Toast.LENGTH_SHORT).show();
            int intervalTime = 2200;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    pressAgaintoExit = false;
                }
            },intervalTime);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        Calendar c = Calendar.getInstance();
        int timeOfDay = c.get(Calendar.HOUR_OF_DAY);



        Locale locale = getResources().getConfiguration().locale;
        Locale.setDefault(locale);
        String currentDate = DateFormat.getDateInstance(DateFormat.FULL).format(c.getTime());


        userDetails = getIntent().getParcelableExtra("userDetails");
        journeyActivity = getIntent().getBooleanExtra("Ongoing",false);


        if(userDetails.getWard()!=null) {
            database.getReference().child("users").child(userDetails.getWard()).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    user ward = dataSnapshot.getValue(user.class);
                    wardName = ward.getName();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }


        DatabaseReference myRef = database.getReference("journeys").child(userDetails.encodedEmail());
        myRef.addListenerForSingleValueEvent(journeyCheck);

        Intent intent = new Intent(WelcomeActivity.this, FollowerAddedService.class);
        intent.putExtra("userDetails", userDetails);

        startService(intent);

        LocalBroadcastManager.getInstance(WelcomeActivity.this).registerReceiver(guardianAdded,
                new IntentFilter("guardAddedBroadcast"));

        LocalBroadcastManager.getInstance(this).registerReceiver(new NoInternetHelper(this).noInternetBroadcastCallback,
                new IntentFilter("InternetStatusBroadcast"));
        new NoInternetHelper(this).checkInternet();
    }

    private BroadcastReceiver guardianAdded = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String ward = intent.getStringExtra("ward");
            SharedPreferences sharedPref = context.getSharedPreferences("travelsafe", Context.MODE_PRIVATE);
            boolean guardianAdded =  sharedPref.getBoolean("guardianAdded", false);
            if ((userDetails.getPreferences()!=null) && (userDetails.getPreferences().get("journeyGuardianAdded").equals("true")) && !guardianAdded) {
                database.getReference().child("users").child(userDetails.getWard()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        user ward = dataSnapshot.getValue(user.class);
                        wardName = ward.getName();

                        NotificationCompat.Builder builder = new NotificationCompat.Builder(WelcomeActivity.this, CHANNEL_ID)
                                .setContentTitle("Guardian added")
                                .setContentText(wardName + " " + getString(R.string.welcomeActivity_addedAsGuardian))
                                .setPriority(Notification.PRIORITY_HIGH)
                                .setSmallIcon(R.drawable.logo_travelsafe);

                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(WelcomeActivity.this);
                        notificationManager.notify(1, builder.build());

                        SharedPreferences.Editor editor = context.getSharedPreferences("travelsafe", MODE_PRIVATE).edit();
                        editor.putBoolean("guardianAdded", true);
                        editor.commit();

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

            }
        }
    };
}

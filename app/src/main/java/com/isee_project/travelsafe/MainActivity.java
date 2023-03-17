package com.isee_project.travelsafe;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.isee_project.travelsafe.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    Animation fadeInAnim;
    ImageView travelsafeLogo;

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference("users");
    FirebaseAuth mAuth;

    user userDetails = new user();
    boolean userLoggedIn = false;
    SharedPreferences sharedPref;

    String selectedLanguage = "en";

    public static final int INTRO_ACTIVITY_DELAY = 3025;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(new NoInternetHelper(this).isInternetConnected()) {
            mAuth = FirebaseAuth.getInstance();

            fadeInAnim = AnimationUtils.loadAnimation(this, R.anim.animation_fade_in);
            fadeInAnim.setFillAfter(true);
            travelsafeLogo = findViewById(R.id.logo);
        Intent intent = new Intent(MainActivity.this, NoInternetService.class);
        startService(intent);

        SharedPreferences prefs = getSharedPreferences("travelsafe", MODE_PRIVATE);
        selectedLanguage = prefs.getString("language", "en");
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setLocale(this, selectedLanguage);
        }

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    SharedPreferences.Editor editor = getSharedPreferences("travelsafe", MODE_PRIVATE).edit();
                    editor.putBoolean("guardianAdded", false);
                    editor.putBoolean("atDestination", false);
                    editor.putBoolean("wardAtDestination", false);
                    editor.putBoolean("startedJourney", false);
                    editor.putBoolean("takingBreak", false);
                    editor.putBoolean("journeyResumed", false);
                    editor.putBoolean("distance25P", false);
                    editor.putBoolean("distance50P", false);
                    editor.putBoolean("distance75P", false);
					editor.commit();
                    if (userLoggedIn) {
                        Intent welcomeIntent = new Intent(MainActivity.this, WelcomeActivity.class);
                        welcomeIntent.putExtra("userDetails", userDetails);
                        startActivity(welcomeIntent);
                    } else {
                        Intent introIntent = new Intent(MainActivity.this, IntroductionActivity.class);
                        startActivity(introIntent);
                    }
                }
            }, INTRO_ACTIVITY_DELAY);


        fadeInAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                sharedPref = MainActivity.this.getSharedPreferences("travelsafe", Context.MODE_PRIVATE);
                userLoggedIn =  sharedPref.getBoolean("staySignedIn", false);

                if(userLoggedIn) {
                    if (mAuth.getCurrentUser() != null) {
                        userDetails.setEmail(mAuth.getCurrentUser().getEmail());
                        userDetails.setName(mAuth.getCurrentUser().getDisplayName());
                        database.getReference("users").child(userDetails.encodedEmail()).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    userDetails = dataSnapshot.getValue(user.class);
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
            public void onAnimationEnd(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

                }
            });
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(new NoInternetHelper(this).noInternetBroadcastCallback,
                new IntentFilter("InternetStatusBroadcast"));
        new NoInternetHelper(this).checkInternet();

    }

    public void setLocale(Activity context, String languageCode) {
        Locale locale;
        locale = new Locale(languageCode);
        Configuration config = new Configuration(context.getResources().getConfiguration());
        Locale.setDefault(locale);
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
            config.setLocale(locale);
        }
        else {
            config.locale = locale;
        }
        context.getBaseContext().getResources().updateConfiguration(config,
                context.getBaseContext().getResources().getDisplayMetrics());
    }
}

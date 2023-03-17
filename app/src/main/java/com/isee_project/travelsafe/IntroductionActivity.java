package com.isee_project.travelsafe;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.isee_project.travelsafe.R;

import java.util.Locale;

public class IntroductionActivity extends AppCompatActivity {

    String[] languages = new String[]{"EN", "DE"};
    String selectedLanguage = "en";
    Spinner dropdown;
    int mPosition = 0;
    boolean pressAgaintoExit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        SharedPreferences prefs = getSharedPreferences("travelsafe", MODE_PRIVATE);
        selectedLanguage = prefs.getString("language", "en");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O){
            selectedLanguage = Locale.getDefault().getLanguage();
            mPosition = selectedLanguage.equals("de") ? 1 : 0;

        }

        Button singIn = (Button)findViewById(R.id.mainSignIn);
        singIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(IntroductionActivity.this, LogInActivity.class));
            }
        });

        final LinearLayout signUp = (LinearLayout)findViewById(R.id.signUp);
        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signUpIntent = new Intent(IntroductionActivity.this, SignUpActivity.class);
                startActivity(signUpIntent);
            }
        });

        dropdown = findViewById(R.id.languageSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.layout_language_spinner, languages);

        dropdown.setAdapter(adapter);
        if(selectedLanguage.equals("de")) {

            dropdown.setTag("1");
            dropdown.setSelection(1);
        }
        else {

            dropdown.setTag("0");
            dropdown.setSelection(0);
        }

        ((Spinner)findViewById(R.id.languageSpinner)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    if ((!dropdown.getTag().toString().equals(Integer.toString(position))) && (mPosition == position)) {
                        if (!(Locale.getDefault().getLanguage().equals(languages[position].toLowerCase()))) {
                            Toast.makeText(IntroductionActivity.this, getString(R.string.toast_noLanguageSwitchingSupport), Toast.LENGTH_SHORT).show();
                        }
                    }
                    if ((dropdown.getTag().toString().equals(Integer.toString(position))) && (mPosition == position)) {
                        mPosition = (mPosition==0) ? 1 : 0;
                    }

                    selectedLanguage = Locale.getDefault().getLanguage();
                    if(selectedLanguage.equals("de")) {

                        dropdown.setTag("1");
                        dropdown.setSelection(1);
                    }
                    else {

                        dropdown.setTag("0");
                        dropdown.setSelection(0);
                    }
                } else {
                    if (position == 0) {

                        selectedLanguage = "en";
                        setLocale(IntroductionActivity.this, "en");
                    } else {

                        selectedLanguage = "de";
                        setLocale(IntroductionActivity.this, "de");
                    }
                }

            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(new NoInternetHelper(this).noInternetBroadcastCallback,
                new IntentFilter("InternetStatusBroadcast"));
        new NoInternetHelper(this).checkInternet();
    }

    public void setLocale(Activity context, String languageCode) {
        Locale locale;
        locale = new Locale(languageCode);
        Configuration config = new Configuration(context.getResources().getConfiguration());
        Locale.setDefault(locale);
        config.setLocale(locale);
        context.getBaseContext().getResources().updateConfiguration(config,
                context.getBaseContext().getResources().getDisplayMetrics());
        SharedPreferences prefs = getSharedPreferences("travelsafe", MODE_PRIVATE);
        if(!selectedLanguage.equals(prefs.getString("language", "en"))) {
            SharedPreferences.Editor editor = context.getSharedPreferences("travelsafe", MODE_PRIVATE).edit();
            editor.putString("language", languageCode);
            editor.apply();
            Intent intent = context.getIntent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            context.finish();
            context.startActivity(intent);

        }
    }

    @Override
    protected void onResume() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O){
            selectedLanguage = Locale.getDefault().getLanguage();
            if (selectedLanguage.equals("en")){
                dropdown.setTag("0");
            }
            else {
                dropdown.setTag("1");
            }
        }
        super.onResume();
    }

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
}

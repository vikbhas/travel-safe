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
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.isee_project.travelsafe.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.Locale;

public class SignUpActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    user userDetails = new user();
    String userPassword;

    String[] languages = new String[]{"EN", "DE"};
    String selectedLanguage = "en";

    Spinner dropdown;
    int mPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        SharedPreferences prefs = getSharedPreferences("travelsafe", MODE_PRIVATE);
        selectedLanguage = prefs.getString("language", "en");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O){
            selectedLanguage = Locale.getDefault().getLanguage();
            mPosition = selectedLanguage.equals("de") ? 1 : 0;
        }

        mAuth = FirebaseAuth.getInstance();

        ImageView backButton = (ImageView) findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(findViewById(android.R.id.content).getRootView().getWindowToken(), 0);
                startActivity(new Intent(SignUpActivity.this, IntroductionActivity.class));
            }
        });

        LinearLayout signIn = (LinearLayout) findViewById(R.id.signInLinLayout);
        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SignUpActivity.this, LogInActivity.class));
            }
        });

        Button signUp = (Button) findViewById(R.id.signUp);
        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(findViewById(android.R.id.content).getRootView().getWindowToken(), 0);
                if(((EditText) findViewById(R.id.name)).getText().toString().isEmpty()) {
                    ((TextView) findViewById(R.id.signUpValidator)).setText(getString(R.string.signUp_enterName));
                }
                else if(((EditText) findViewById(R.id.email)).getText().toString().isEmpty()) {
                    ((TextView) findViewById(R.id.signUpValidator)).setText(getString(R.string.signUp_enterEmail));
                }
                else if(((EditText) findViewById(R.id.password)).getText().toString().isEmpty()) {
                    ((TextView) findViewById(R.id.signUpValidator)).setText(getString(R.string.stignUp_enterPassword));
                }
                else {
                    userDetails.setName(((EditText) findViewById(R.id.name)).getText().toString());
                    userDetails.setEmail(((EditText) findViewById(R.id.email)).getText().toString().toLowerCase());
                    userPassword = ((EditText) findViewById(R.id.password)).getText().toString();
                    mAuth.createUserWithEmailAndPassword(userDetails.getEmail(), userPassword)
                            .addOnCompleteListener(SignUpActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {

                                        Log.d("SignUpActivity", "createUserWithEmail:success");

                                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                                .setDisplayName(userDetails.getName())
                                                .build();
                                        mAuth.getCurrentUser().updateProfile(profileUpdates)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if (task.isSuccessful()) {
                                                            mAuth.getCurrentUser().sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if(task.isSuccessful()) {
                                                                        Intent signUpIntent = new Intent(SignUpActivity.this, LogInActivity.class);
                                                                        signUpIntent.putExtra("signUpSuccessful", true);
                                                                        startActivity(signUpIntent);
                                                                    }
                                                                    else {
                                                                        ((TextView) findViewById(R.id.signUpValidator)).setText(getString(R.string.signUp_signUpError1) + "\n" +getString(R.string.signUp_signUpError2));
                                                                    }
                                                                }
                                                            });
                                                        }
                                                        else {
                                                            Log.d("SignUpActivity", "Updating User profile failed.");
                                                        }
                                                    }
                                                });
                                    } else {
                                        Log.w("SignUpActivity", "createUserWithEmail:failure" + task.getException().getClass());
                                        if (task.getException().getClass() == FirebaseAuthWeakPasswordException.class) {
                                            ((TextView) findViewById(R.id.signUpValidator)).setText(((FirebaseAuthWeakPasswordException) task.getException()).getReason());
                                        }
                                        else if (task.getException().getClass() == FirebaseAuthInvalidCredentialsException.class) {
                                            ((TextView) findViewById(R.id.signUpValidator)).setText(task.getException().getLocalizedMessage());
                                        }
                                        else if (task.getException().getClass() == FirebaseAuthUserCollisionException.class) {
                                            ((TextView) findViewById(R.id.signUpValidator)).setText(task.getException().getLocalizedMessage());
                                        }
                                        else if (task.getException().getClass() == FirebaseNetworkException.class) {
                                            ((TextView) findViewById(R.id.signUpValidator)).setText(getString(R.string.signUp_noInternet));
                                        }
                                    }
                                }
                            });
                }
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
                            Log.i("SignUpActivity", "OnItemSelected called: API24 user wants to change language");
                            Toast.makeText(SignUpActivity.this, getString(R.string.toast_noLanguageSwitchingSupport), Toast.LENGTH_SHORT).show();
                        }
                    }
                    if ((dropdown.getTag().toString().equals(Integer.toString(position))) && (mPosition == position)) {
                        mPosition = (mPosition==0) ? 1 : 0;
                    }
                    Log.i("SignUpActivity", "Tag: " + dropdown.getTag().toString());
                    Log.i("SignUpActivity", "position: " + position);
                    Log.i("SignUpActivity", "mPosition: " + mPosition);
                    selectedLanguage = Locale.getDefault().getLanguage();
                    if(selectedLanguage.equals("de")) {
                        Log.i("SignUpActivity", "OnItemSelected called: Updating spinner to show " + selectedLanguage);
                        dropdown.setTag("1");
                        dropdown.setSelection(1);
                    }
                    else {
                        dropdown.setTag("0");
                        dropdown.setSelection(0);
                    }
                }
                else {
                    if (position == 0) {
                        selectedLanguage = "en";
                        setLocale(SignUpActivity.this, "en");
                    } else {
                        selectedLanguage = "de";
                        setLocale(SignUpActivity.this, "de");
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
}

package com.isee_project.travelsafe;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.isee_project.travelsafe.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Comparator;

public class PhoneContactsActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    PhoneContactsRecyclerViewAdapter adapter;
    ArrayList<PhoneContact> phoneContactsList;
    ArrayList<String> contactNames = new ArrayList<>();
    ArrayList<PhoneContact> filteredList;
    PhoneContact contact;
    long journeyDuration;
    user userDetails;
    journey userJourney;
    DatabaseReference reference = FirebaseDatabase.getInstance().getReference("journeys");
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_contacts);
        userDetails = getIntent().getParcelableExtra("userDetails");
        userJourney = getIntent().getParcelableExtra("userJourney");
        journeyDuration = getIntent().getLongExtra("journeyDuration",0);
        recyclerView = (RecyclerView) findViewById(R.id.phoneContactsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        phoneContactsList = new ArrayList<>();
        adapter = new PhoneContactsRecyclerViewAdapter(PhoneContactsActivity.this, phoneContactsList, userDetails.encodedEmail());
        recyclerView.setAdapter(adapter);;

        if(ContextCompat.checkSelfPermission(PhoneContactsActivity.this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            ContentResolver resolver = getContentResolver();
            @SuppressLint("Recycle") Cursor cursor = resolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

            while(cursor.moveToNext()) {
                ArrayList<String> duplicates = new ArrayList<>();
                String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                String photo = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI));
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                Cursor phoneCursor = resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[] {id}, null);


                while(phoneCursor.moveToNext()) {
                    String phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                    if(!duplicates.contains(phoneNumber)) {
                        PhoneContact contact = new PhoneContact(name, phoneNumber, photo);
                        phoneContactsList.add(contact);
                        adapter.notifyDataSetChanged();
                    }
                    duplicates.add(phoneNumber);

                }
                phoneContactsList.sort(Comparator.comparing(PhoneContact::getName));
            }
        }

        else {
            Toast.makeText(this, R.string.phoneContacts_permission, Toast.LENGTH_SHORT).show();
        }


        ((EditText) findViewById(R.id.searchPhone)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                Log.i("TextChangedListener","Entered");
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String filterPattern = ((EditText) findViewById(R.id.searchPhone)).getText().toString();
                filteredList = new ArrayList<>();
                Log.i("TextChangedListener",filterPattern);
                if(filterPattern.equals("")) {
                    filteredList.addAll(phoneContactsList);
                }
                else {
                    for(PhoneContact contact : phoneContactsList) {
                        if(contact.getName().toLowerCase().contains(filterPattern) || contact.getName().contains(filterPattern) || contact.getPhoneNumber().contains(filterPattern))
                            filteredList.add(contact);
                    }
                }
                adapter = new PhoneContactsRecyclerViewAdapter(PhoneContactsActivity.this,filteredList,userDetails.encodedEmail());
                recyclerView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
            }

        });

        ((Button) findViewById(R.id.selectContacts)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reference.child(userDetails.encodedEmail()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        journey currentJourney = dataSnapshot.getValue(journey.class);
                        if(currentJourney.getPhoneContacts()!=null) {
                            ArrayList<String> guardiansList = currentJourney.getPhoneContacts();
                            userJourney.setPhoneContacts(guardiansList);
                            for(int i = 0; i<phoneContactsList.size(); i++) {
                                contact = phoneContactsList.get(i);
                                for(int j=0;j<guardiansList.size();j++)
                                    if(guardiansList.get(j).equals(contact.getPhoneNumber()))
                                        contactNames.add(contact.getName());
                                userJourney.setContactNames(contactNames);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });
                Intent selectGuardiansIntent = new Intent(PhoneContactsActivity.this, SelectFollowerActivity.class);
                selectGuardiansIntent.putExtra("userJourney",userJourney);
                selectGuardiansIntent.putExtra("userDetails",userDetails);
                selectGuardiansIntent.putExtra("contactNames",contactNames);
                selectGuardiansIntent.putExtra("journeyDuration", journeyDuration);
                startActivity(selectGuardiansIntent);
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent selectGuardiansIntent = new Intent(PhoneContactsActivity.this, SelectFollowerActivity.class);
        selectGuardiansIntent.putExtra("userJourney",userJourney);
        selectGuardiansIntent.putExtra("userDetails",userDetails);
        selectGuardiansIntent.putExtra("contactNames",contactNames);
        startActivity(selectGuardiansIntent);
    }
}
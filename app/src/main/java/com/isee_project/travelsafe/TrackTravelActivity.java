package com.isee_project.travelsafe;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.Notification;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.isee_project.travelsafe.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.GeoApiContext;
import com.google.maps.model.EncodedPolyline;

import java.util.ArrayList;
import java.util.List;

import static com.isee_project.travelsafe.App.CHANNEL_ID;

public class TrackTravelActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    String apiKey = "";

    LatLng currentLocation, destinationLocation, sourceLocation;
    Marker currentMarker, sourceMarker, destinationMarker;

    user userDetails;
    journey journey;
    int count = 0;

    private FusedLocationProviderClient fusedLocationClient;

    GeoApiContext geoApiContext = new GeoApiContext.Builder()
            .apiKey(apiKey)
            .build();

    EncodedPolyline sourceDestinationEncodedPolyline;
    Polyline sourceDestinationPolyline;

    private String destinationName;
    private String sourceName;

    ArrayList<Break> breakList = new ArrayList<Break>();
    ArrayList<Marker> breakMarkersList = new ArrayList<Marker>();
	
	ArrayList<String> checkpointsList;
    ArrayList<Marker> checkpointMarkersList;

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(TrackTravelActivity.this,WelcomeActivity.class);
        intent.putExtra("Ongoing",true);
        intent.putExtra("userDetails",userDetails);
        startActivity(intent);

    }
	

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_journey);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.journeyMap);
        mapFragment.getMapAsync(this);

        Intent i = getIntent();
        userDetails = i.getParcelableExtra("userDetails");
        journey = i.getParcelableExtra("userJourney");
        sourceName = journey.getSourceName();
        destinationName = journey.getDestinationName();

        sourceLocation = new LatLng(journey.getSource().getLatitude(), journey.getSource().getLongitude());
        destinationLocation = new LatLng(journey.getDestination().getLatitude(), journey.getDestination().getLongitude());
        if(journey.getGPSTracking().equals("true")) {
            currentLocation = new LatLng(journey.getCurrentLocation().getLatitude(), journey.getCurrentLocation().getLongitude());
        }
        sourceDestinationEncodedPolyline = new EncodedPolyline(journey.getSourceDestinationEncodedPolyline());

        ((TextView) findViewById(R.id.journeySource)).setText(sourceName);
        ((TextView) findViewById(R.id.journeyDestination)).setText(destinationName);
        ((TextView) findViewById(R.id.eta)).setText(getString(R.string.trackJourney_eta) + " "+journey.getETA());

        ((TextView) findViewById(R.id.eta)).setText(getString(R.string.journey_eta) + " " + journey.getETA());
        if(journey.getModeOfTransport().equals("driving")) {
            ((ImageButton)findViewById(R.id.modeOfTransport)).setBackground(getDrawable(R.drawable.driving_fin));
        }
        else if(journey.getModeOfTransport().equals("bicycling")) {
            ((ImageButton)findViewById(R.id.modeOfTransport)).setBackground(getDrawable(R.drawable.cycling_fin));
        }
        else if(journey.getModeOfTransport().equals("walking")) {
            ((ImageButton)findViewById(R.id.modeOfTransport)).setBackground(getDrawable(R.drawable.walking_fin));
        }

        FirebaseDatabase.getInstance().getReference("journeys").child(journey.getWard()).child("journeyCompleted").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    if(dataSnapshot.getValue(String.class).equals("true")) {
                        ((Button)findViewById(R.id.stopTracking)).setText(getString(R.string.trackJourney_goHome));
                        ((Button)findViewById(R.id.stopTracking)).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(TrackTravelActivity.this, WelcomeActivity.class);
                                intent.putExtra("userDetails", userDetails);
                                startActivity(intent);
                            }
                        });
                        FirebaseDatabase.getInstance().getReference("journeys").child(journey.getWard()).child("finishLocation").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if(dataSnapshot.exists()) {
                                    Log.i("TrackTravelActivity", "Adding stop location");
                                    com.isee_project.travelsafe.LatLng finishLocation = dataSnapshot.getValue(com.isee_project.travelsafe.LatLng.class);
                                    mMap.addMarker(new MarkerOptions().position(new LatLng(finishLocation.getLatitude(), finishLocation.getLongitude())).title("Stopped location").icon(BitmapDescriptorFactory.fromResource(R.drawable.finish_marker)));
                                    currentMarker.remove();
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
        });


        ((ImageButton) findViewById(R.id.Guardianslist)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                count++;
                FragmentManager fragmentManager = getSupportFragmentManager();
                ShowFollowersFragment fragment = new ShowFollowersFragment(userDetails.getWard());
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction().replace(R.id.guardianslistfragment, fragment);
                fragmentTransaction.commit();
                if(count%2 == 0)
                    fragmentTransaction.hide(fragment);
            }
        });

        ((Button) findViewById(R.id.sos)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(TrackTravelActivity.this, getString(R.string.trackJourney_noOngoingJourney), Toast.LENGTH_SHORT).show();
            }
        });

        ((Button) findViewById(R.id.sos)).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(TrackTravelActivity.this, getString(R.string.trackJourney_noOngoingJourney), Toast.LENGTH_SHORT).show();
                return true;
            }
        });


        ((Button) findViewById(R.id.stopTracking)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference myRef = database.getReference("journeys").child(journey.getWard()).child("guardiansList");
                myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()) {
                            ArrayList<String> guardiansList = (ArrayList<String>) dataSnapshot.getValue();
                            if(guardiansList.size()==1) {
                                ((TextView) findViewById(R.id.stopTrackingError)).setText(getString(R.string.trackJourney_onlyGuardian));
                            }
                            else {
                                guardiansList.remove(userDetails.encodedEmail());
                                myRef.setValue(guardiansList);
                                FirebaseDatabase.getInstance().getReference("users").child(userDetails.encodedEmail()).child("ward").removeValue();
                                Toast.makeText(TrackTravelActivity.this, getString(R.string.trackJourney_stoppedTracking), Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(TrackTravelActivity.this, WelcomeActivity.class);
                                intent.putExtra("userDetails", userDetails);
                                startActivity(intent);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        });

        FirebaseDatabase.getInstance().getReference("journeys").child(userDetails.getWard()).child("eta").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    ((TextView)findViewById(R.id.eta)).setText(getString(R.string.trackJourney_eta) + " " + dataSnapshot.getValue(String.class));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(new NoInternetHelper(this).noInternetBroadcastCallback,
                new IntentFilter("InternetStatusBroadcast"));
        new NoInternetHelper(this).checkInternet();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setMinZoomPreference(0.0f);
        mMap.setMaxZoomPreference(21.0f);

        mMap.moveCamera(CameraUpdateFactory.zoomTo(17f));

        if(journey.getGPSTracking().equals("true")) {
            setWardLocation();
        }
        else {
            setCheckpointsList();
        }
        setSourceDestination();
		setBreakMarkers();
    }

    private void setCheckpointsList() {
        FirebaseDatabase.getInstance().getReference("journeys").child(journey.getWard()).child("checkpointsList").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    if (userDetails.getPreferences().get("journeyCheckpointAdded").equals("true")) {
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(TrackTravelActivity.this, CHANNEL_ID)
                                .setContentTitle("Checkpoint added")
                                .setContentText(journey.getWardName() + " " + getString(R.string.TrackTravelActivity_checkPointAdded))
                                .setPriority(Notification.PRIORITY_HIGH)
                                .setSmallIcon(R.drawable.logo_travelsafe);

                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(TrackTravelActivity.this);
                        notificationManager.notify(10, builder.build());
                    }

                    if(checkpointMarkersList!=null) {
                        for (Marker checkpointMarker : checkpointMarkersList) {
                            checkpointMarker.remove();
                        }
                    }
                    checkpointMarkersList = new ArrayList<Marker>();
                    GenericTypeIndicator<ArrayList<String>> genericTypeIndicator = new GenericTypeIndicator<ArrayList<String>>(){};
                    checkpointsList = dataSnapshot.getValue(genericTypeIndicator);
                    for(String checkpoint: checkpointsList) {
                        checkpointMarkersList.add(mMap.addMarker(new MarkerOptions().position(new LatLng(Double.parseDouble(checkpoint.split(",")[0]), Double.parseDouble(checkpoint.split(",")[1]))).title("Checkpoint").icon(BitmapDescriptorFactory.fromResource(R.drawable.checkpoint_marker))));
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setWardLocation() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("journeys").child(journey.getWard()).child("currentLocation");

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                com.isee_project.travelsafe.LatLng currentLocation = dataSnapshot.getValue(com.isee_project.travelsafe.LatLng.class);
                if(currentMarker!=null) {
                    currentMarker.remove();
                }
                currentMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude())).title("Ward location").icon(BitmapDescriptorFactory.fromResource(R.drawable.user_location)));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private void setSourceDestination() {
        if(sourceMarker!=null){
            sourceMarker.remove();
        }
        destinationMarker = mMap.addMarker(new MarkerOptions().position(destinationLocation).title("Destination").icon(BitmapDescriptorFactory.fromResource(R.drawable.location)));
        sourceMarker = mMap.addMarker(new MarkerOptions().position(sourceLocation).title("Source").icon(BitmapDescriptorFactory.fromResource(R.drawable.location)));
        drawRoute(sourceLocation, destinationLocation);
        if(journey.getGPSTracking().equals("true")) {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
        }
        else {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(sourceLocation));
        }
    }

    public void drawRoute(LatLng sourceLocation, LatLng destinationLocation) {
        List<LatLng> path = new ArrayList();
        if(sourceDestinationPolyline!=null) {
            sourceDestinationPolyline.remove();
        }
        List<com.google.maps.model.LatLng> coords1 = sourceDestinationEncodedPolyline.decodePath();
        for (com.google.maps.model.LatLng coord1 : coords1) {
            path.add(new LatLng(coord1.lat, coord1.lng));
        }
        if (path.size() > 0) {
            PolylineOptions opts = new PolylineOptions().addAll(path).color(0xff6a2d57).width(10);
            sourceDestinationPolyline = mMap.addPolyline(opts);
        }
    }

    public void setBreakMarkers() {
        FirebaseDatabase.getInstance().getReference("journeys").child(userDetails.getWard()).child("breakList").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    GenericTypeIndicator<ArrayList<Break>> genericTypeIndicator = new GenericTypeIndicator<ArrayList<Break>>() {};
                    breakList = dataSnapshot.getValue(genericTypeIndicator);
                    for(Marker breakMarker: breakMarkersList) {
                        breakMarker.remove();
                    }
                    breakMarkersList = new ArrayList<Marker>();
                    for(Break break1: breakList) {
                        if (break1.getBreakDuration()!=null){
                            breakMarkersList.add(mMap.addMarker(new MarkerOptions().position(new LatLng(break1.getBreakLocation().getLatitude(),break1.getBreakLocation().getLongitude())).title(break1.getBreakDuration()+" min").icon(BitmapDescriptorFactory.fromResource(R.drawable.break_time_marker))));
                        }
                        else {
                            breakMarkersList.add(mMap.addMarker(new MarkerOptions().position(new LatLng(break1.getBreakLocation().getLatitude(),break1.getBreakLocation().getLongitude())).title("Ongoing").icon(BitmapDescriptorFactory.fromResource(R.drawable.break_time_marker))));
                        }
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}

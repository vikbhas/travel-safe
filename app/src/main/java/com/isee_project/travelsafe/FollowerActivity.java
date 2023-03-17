package com.isee_project.travelsafe;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import com.isee_project.travelsafe.R;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AddressComponents;
import com.google.android.libraries.places.api.model.OpeningHours;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlusCode;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.maps.DirectionsApi;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.EncodedPolyline;
import com.google.maps.model.TravelMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static com.google.android.libraries.places.api.Places.createClient;

public class FollowerActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    String apiKey = "AIzaSyCcyeDCrnJQrLo1C-VrIawEZxrZt-oknyg";

    private FusedLocationProviderClient fusedLocationClient;
    LatLng currentLocation, destinationLocation, sourceLocation;
    Marker currentMarker, sourceMarker, destinationMarker;

    private SourceAutocompleteSkeleton SourceAutocompleteSkeleton = new SourceAutocompleteSkeleton();
    private DestinationAutocompleteSkeleton destinationAutocompleteSkeleton = new DestinationAutocompleteSkeleton();
    List<Place.Field> placeFields = Arrays.asList(Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ID);

    GeoApiContext geoApiContext = new GeoApiContext.Builder()
            .apiKey(apiKey)
            .build();

    EncodedPolyline sourceDestinationEncodedPolyline;
    Polyline sourceDestinationPolyline;

    journey journey = new journey();
    user userDetails;
    private Place destination;
    private Place source;
    private Long journeyDuration = 0L;
    private float userEnteredETA = 0;

    List<List<LatLng>> polylineList;
    List<Long> journeyDurationList;
    List<Polyline> sourceDestinationPolylineList;
    List<EncodedPolyline> sourceDestinationEncodedPolylineList;

    boolean routeSelected = false;
    boolean askedGPSPermission = false;

    ArrayList<String> modeOfTransport = new ArrayList<String>();
    String selectedModeOfTransport;
    TravelMode travelMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guard);

        userDetails = getIntent().getParcelableExtra("userDetails");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Places.initialize(getApplicationContext(), apiKey);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        LocalBroadcastManager.getInstance(FollowerActivity.this).registerReceiver(sourceDestinationMessageReceiver,
                new IntentFilter("locationIDBroadcast"));

        journey.setGPSTracking("true");
        ((Switch) findViewById(R.id.GPSSwitch)).setChecked(true);

        modeOfTransport.add("driving");
        modeOfTransport.add("bicycling");
        modeOfTransport.add("walking");
        selectedModeOfTransport = modeOfTransport.get(2);

        ((ImageButton)findViewById(R.id.backButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent backIntent = new Intent(FollowerActivity.this, WelcomeActivity.class);
                backIntent.putExtra("userDetails",userDetails);
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(findViewById(android.R.id.content).getRootView().getWindowToken(), 0);
                startActivity(backIntent);
            }
        });

        ((ImageButton) findViewById(R.id.driving_light)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedModeOfTransport = modeOfTransport.get(0);
                drawRoute(TravelMode.DRIVING);
                ((ImageButton)findViewById(R.id.driving_fin)).setVisibility(View.VISIBLE);
                ((ImageButton)findViewById(R.id.cycling_fin)).setVisibility(View.GONE);
                ((ImageButton)findViewById(R.id.walking_fin)).setVisibility(View.GONE);
            }
        });

        ((ImageButton) findViewById(R.id.cycling_light)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedModeOfTransport = modeOfTransport.get(1);
                drawRoute(TravelMode.BICYCLING);
                ((ImageButton)findViewById(R.id.driving_fin)).setVisibility(View.GONE);
                ((ImageButton)findViewById(R.id.cycling_fin)).setVisibility(View.VISIBLE);
                ((ImageButton)findViewById(R.id.walking_fin)).setVisibility(View.GONE);
            }
        });

        ((ImageButton) findViewById(R.id.walking_light)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedModeOfTransport = modeOfTransport.get(2);
                drawRoute(TravelMode.WALKING);
                ((ImageButton)findViewById(R.id.driving_fin)).setVisibility(View.GONE);
                ((ImageButton)findViewById(R.id.cycling_fin)).setVisibility(View.GONE);
                ((ImageButton)findViewById(R.id.walking_fin)).setVisibility(View.VISIBLE);
            }
        });

        ((TextView) findViewById(R.id.source)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                String userSourceText = ((TextView) findViewById(R.id.source)).getText().toString();
                if(userSourceText.equals(getString(R.string.guard_enterSource)) || userSourceText.equals(getString(R.string.guard_yourLocation))) {
                    userSourceText="";
                }
                if(destinationAutocompleteSkeleton.isAdded()) {
                    ft.remove(destinationAutocompleteSkeleton);
                }
                SourceAutocompleteSkeleton = SourceAutocompleteSkeleton.newInstance(userSourceText);
                ft.add(R.id.sourceAutocompletePlaceholder, SourceAutocompleteSkeleton, "SourceAutocompleteSkeleton");
                ft.commit();
            }
        });
        ((TextView) findViewById(R.id.destination)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(((TextView) findViewById(R.id.source)).getText().toString().equals(getString(R.string.guard_enterSource))) {

                    ((TextView)findViewById(R.id.selectGuardiansError)).setText(getString(R.string.guard_validSource));
                }
                else {

                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                    String userDestinationText = ((TextView) findViewById(R.id.destination)).getText().toString();
                    if (userDestinationText.equals(getString(R.string.guard_enterDestination))) {
                        userDestinationText = "";
                    }
                    if (SourceAutocompleteSkeleton.isAdded()) {
                        ft.remove(SourceAutocompleteSkeleton);
                    }
                    destinationAutocompleteSkeleton = destinationAutocompleteSkeleton.newInstance(userDestinationText);
                    ft.add(R.id.destinationAutocompletePlaceholder, destinationAutocompleteSkeleton, "destinationAutocompleteSkeleton");
                    ft.commit();
                }
            }
        });

        ((Button)findViewById(R.id.selectGuardians)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((((TextView)findViewById(R.id.source)).getText().toString().equals(getString(R.string.guard_enterSource)))) {
                    ((TextView)findViewById(R.id.selectGuardiansError)).setText(getString(R.string.guard_validSource));
                }
                else if ((((TextView)findViewById(R.id.destination)).getText().toString().equals(getString(R.string.guard_enterDestination)))) {
                    ((TextView)findViewById(R.id.selectGuardiansError)).setText(getString(R.string.guard_validDestination));
                }
                else if (!routeSelected) {
                    ((TextView)findViewById(R.id.selectGuardiansError)).setText(getString(R.string.guard_selectRoute));
                }
                else {
                    Intent intent = new Intent(FollowerActivity.this,SelectFollowerActivity.class);
                    String ward = userDetails.encodedEmail();
                    journey.setWard(ward);
                    journey.setWardName(userDetails.getName());
                    journey.setSource(new com.isee_project.travelsafe.LatLng(sourceLocation.latitude,sourceLocation.longitude));
                    journey.setDestination(new com.isee_project.travelsafe.LatLng(destinationLocation.latitude,destinationLocation.longitude));
                    journey.setGPSTracking("" + ((Switch)findViewById(R.id.GPSSwitch)).isChecked());
                    if(journey.getGPSTracking().equals("true")) {
                        journey.setCurrentLocation(new com.isee_project.travelsafe.LatLng(currentLocation.latitude,currentLocation.longitude));
                    }
                    journey.setJourneyCompleted("false");
                    journey.setReachedDestination("false");
                    journey.setRouteDeviation("false");
                    journey.setSourceName(source.getName());
                    journey.setDestinationName(destination.getName());
                    journey.setSourceDestinationEncodedPolyline(sourceDestinationEncodedPolyline.toString().substring(18, sourceDestinationEncodedPolyline.toString().length()-1));
                    journey.setModeOfTransport(selectedModeOfTransport);

                    journey.setDistance25P("false");
                    journey.setDistance50P("false");
                    journey.setDistance75P("false");

                    intent.putExtra("userDetails", userDetails);
                    intent.putExtra("userJourney", journey);
                    intent.putExtra("journeyDuration", journeyDuration);
                    intent.putExtra("userEnteredETA", userEnteredETA);
                    startActivity(intent);
                }
            }
        });

        ((Switch) findViewById(R.id.GPSSwitch)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(((Switch) findViewById(R.id.GPSSwitch)).isChecked()) {

                    mMap.clear();
                    sourceLocation=null;
                    destinationLocation=null;
                    sourceMarker=null;
                    destinationMarker=null;
                    ((TextView)findViewById(R.id.source)).setText(getString(R.string.guard_enterSource));
                    ((TextView)findViewById(R.id.destination)).setText(getString(R.string.guard_enterDestination));
                     ((ImageButton)findViewById(R.id.AddETA)).setVisibility(View.GONE);
                    journeyDuration = 0L;
                    setUserLocation();
                }
                else {

                    askedGPSPermission = false;
                    mMap.setMyLocationEnabled(false);
                    journey.setCurrentLocation(null);
                    sourceLocation=null;
                    sourceMarker=null;
                    destinationLocation=null;
                    destinationMarker=null;
                    mMap.clear();
                    ((TextView)findViewById(R.id.source)).setText(getString(R.string.guard_enterSource));
                    ((TextView)findViewById(R.id.destination)).setText(getString(R.string.guard_enterDestination));
                    ((TextView)findViewById(R.id.ETA)).setText("");
                    ((ImageButton)findViewById(R.id.AddETA)).setVisibility(View.GONE);
                    journeyDuration = 0L;
                }
            }
        });

        ((ImageButton) findViewById(R.id.AddETA)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(((LinearLayout) findViewById(R.id.ETAInput)).getVisibility()==View.GONE) {
                    ((LinearLayout) findViewById(R.id.ETAInput)).setVisibility(View.VISIBLE);
                }
                else {
                    ((LinearLayout) findViewById(R.id.ETAInput)).setVisibility(View.GONE);
                }
            }
        });

        ((ImageButton) findViewById(R.id.ETAEntered)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(((EditText) findViewById(R.id.userETAInput)).getText().toString().length()!=0) {
                    userEnteredETA = Float.parseFloat(((EditText) findViewById(R.id.userETAInput)).getText().toString());
                    ((TextView) findViewById(R.id.ETA)).setText(getString(R.string.guard_eta) + " " + String.format("%.2f", (journeyDuration / 60.0) + userEnteredETA) + " min");
                    ((LinearLayout) findViewById(R.id.ETAInput)).setVisibility(View.GONE);
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(findViewById(android.R.id.content).getRootView().getWindowToken(), 0);
                }
            }
        });

        ((EditText) findViewById(R.id.userETAInput)).setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    userEnteredETA = Float.parseFloat(((EditText) findViewById(R.id.userETAInput)).getText().toString());
                    ((TextView) findViewById(R.id.ETA)).setText(getString(R.string.guard_eta) + " " + String.format("%.2f", (journeyDuration / 60.0) + userEnteredETA) + " min");
                    ((LinearLayout) findViewById(R.id.ETAInput)).setVisibility(View.GONE);
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(findViewById(android.R.id.content).getRootView().getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(new NoInternetHelper(this).noInternetBroadcastCallback,
                new IntentFilter("InternetStatusBroadcast"));
        new NoInternetHelper(this).checkInternet();
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setMinZoomPreference(0.0f);
        mMap.setMaxZoomPreference(21.0f);

        mMap.moveCamera(CameraUpdateFactory.zoomTo(17f));

        mMap.setOnPolylineClickListener(new GoogleMap.OnPolylineClickListener() {
            @Override
            public void onPolylineClick(Polyline polyline) {
                for(int i = 0; i< sourceDestinationPolylineList.size(); i++) {
                    if(sourceDestinationPolylineList.get(i).equals(polyline)) {
                        routeSelected = true;
                        sourceDestinationPolylineList.get(i).setColor(0xFF2D6A53);
                        sourceDestinationPolylineList.get(i).setWidth(12);
                        sourceDestinationPolylineList.get(i).setZIndex(1);
                        sourceDestinationPolyline = sourceDestinationPolylineList.get(i);
                        sourceDestinationEncodedPolyline = sourceDestinationEncodedPolylineList.get(i);

                        journeyDuration = journeyDurationList.get(i);
                        ((TextView) findViewById(R.id.ETA)).setText(getString(R.string.guard_eta) + " " + String.format("%.2f", (journeyDuration / 60.0) + userEnteredETA) + " min");
                        ((ImageButton) findViewById(R.id.AddETA)).setVisibility(View.VISIBLE);
                        ((LinearLayout) findViewById(R.id.ETAInput)).setVisibility(View.GONE);
                    }
                    else {
                        sourceDestinationPolylineList.get(i).setColor(0xFF6A2D57);
                        sourceDestinationPolylineList.get(i).setWidth(8);
                        sourceDestinationPolylineList.get(i).setZIndex(0);
                    }
                }
            }
        });
        if(journey.getGPSTracking().equals("true")) {
            setUserLocation();
        }
        else {
            LatLng tajMahal = new LatLng(53.569931, 13.212500);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(tajMahal));
        }
    }

    void setUserLocation() {

        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.i("FollowerActivity", "GPS Permission granted by user");
            ((Switch)findViewById(R.id.GPSSwitch)).setChecked(true);
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {

                            if (location != null) {
                                Log.i("FollowerActivity", location.getLatitude() + " " + location.getLongitude());
                                currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            currentMarker = mMap.addMarker(new MarkerOptions().position(currentLocation).title("User location").icon(BitmapDescriptorFactory.fromResource(R.drawable.user_location)));
                                if(sourceMarker!=null) {
                                    sourceMarker.remove();
                                }
                                sourceMarker = mMap.addMarker(new MarkerOptions().position(currentLocation).title("Source").icon(BitmapDescriptorFactory.fromResource(R.drawable.location)));
                                ((TextView) findViewById(R.id.source)).setText(getString(R.string.guard_yourLocation));

                                Geocoder geocoder = new Geocoder(FollowerActivity.this);
                                try {
                                    List<Address> addresses = geocoder.getFromLocation(currentLocation.latitude, currentLocation.longitude, 1);
                                    Address obj = addresses.get(0);
                                    source = new Place() {
                                        @Nullable
                                        @Override
                                        public String getAddress() {
                                            return null;
                                        }

                                        @Nullable
                                        @Override
                                        public AddressComponents getAddressComponents() {
                                            return null;
                                        }

                                        @Nullable
                                        @Override
                                        public List<String> getAttributions() {
                                            return null;
                                        }

                                        @Nullable
                                        @Override
                                        public String getId() {
                                            return null;
                                        }

                                        @Nullable
                                        @Override
                                        public LatLng getLatLng() {
                                            return null;
                                        }

                                        @Nullable
                                        @Override
                                        public String getName() {
                                            return obj.getAddressLine(0).split(",")[0];
                                        }

                                        @Nullable
                                        @Override
                                        public OpeningHours getOpeningHours() {
                                            return null;
                                        }

                                        @Nullable
                                        @Override
                                        public String getPhoneNumber() {
                                            return null;
                                        }

                                        @Nullable
                                        @Override
                                        public List<PhotoMetadata> getPhotoMetadatas() {
                                            return null;
                                        }

                                        @Nullable
                                        @Override
                                        public PlusCode getPlusCode() {
                                            return null;
                                        }

                                        @Nullable
                                        @Override
                                        public Integer getPriceLevel() {
                                            return null;
                                        }

                                        @Nullable
                                        @Override
                                        public Double getRating() {
                                            return null;
                                        }

                                        @Nullable
                                        @Override
                                        public List<Type> getTypes() {
                                            return null;
                                        }

                                        @Nullable
                                        @Override
                                        public Integer getUserRatingsTotal() {
                                            return null;
                                        }

                                        @Nullable
                                        @Override
                                        public Integer getUtcOffsetMinutes() {
                                            return null;
                                        }

                                        @Nullable
                                        @Override
                                        public LatLngBounds getViewport() {
                                            return null;
                                        }

                                        @Nullable
                                        @Override
                                        public Uri getWebsiteUri() {
                                            return null;
                                        }

                                        @Override
                                        public int describeContents() {
                                            return 0;
                                        }

                                        @Override
                                        public void writeToParcel(Parcel dest, int flags) {

                                        }
                                    };
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation));
                                sourceLocation = currentLocation;
                                mMap.setMyLocationEnabled(true);
                            }
                            else {
                                Log.i("FollowerActivity","Error getting current location. Trying again...");
                                setUserLocation();
                            }
                        }
                    });
        } else if(!askedGPSPermission){

            ((Switch)findViewById(R.id.GPSSwitch)).setChecked(false);
           ActivityCompat.requestPermissions(FollowerActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
            ActivityCompat.requestPermissions(FollowerActivity.this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    2);
            askedGPSPermission = true;
            setUserLocation();
        }
        else {

            ((Switch)findViewById(R.id.GPSSwitch)).setChecked(false);
            mMap.setMyLocationEnabled(false);
            askedGPSPermission = false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 1 || requestCode == 2) {
            if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED) {

                setUserLocation();
            }
        }
    }

    public BroadcastReceiver sourceDestinationMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String locationID = intent.getStringExtra("locationID");

            FetchPlaceRequest request = FetchPlaceRequest.newInstance(locationID, placeFields);
            PlacesClient placesClient =  createClient(FollowerActivity.this);

            placesClient.fetchPlace(request).addOnSuccessListener((response) -> {
                Place place = response.getPlace();


                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();


                if(getCurrentFocus()==findViewById(R.id.sourceEditText)) {
                    ((ProgressBar)findViewById(R.id.sourceProgressBar)).setVisibility(View.VISIBLE);
                    ((TextView) findViewById(R.id.source)).setText(place.getName());
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    ((TextView)findViewById(R.id.selectGuardiansError)).setText("");
                    imm.hideSoftInputFromWindow(findViewById(android.R.id.content).getRootView().getWindowToken(), 0);

                    ft.remove(SourceAutocompleteSkeleton);

                    sourceLocation = place.getLatLng();
                    source = place;
                    if(sourceMarker!=null) {
                        sourceMarker.remove();
                    }
                    sourceMarker = mMap.addMarker(new MarkerOptions().position(sourceLocation).title("Source").icon(BitmapDescriptorFactory.fromResource(R.drawable.location)));
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(sourceLocation));
                }

                else if (getCurrentFocus()==findViewById(R.id.destinationEditText)) {
                    ft.remove(destinationAutocompleteSkeleton);
                    ((TextView) findViewById(R.id.destination)).setText(place.getName());
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    ((TextView) findViewById(R.id.selectGuardiansError)).setText("");
                    imm.hideSoftInputFromWindow(findViewById(android.R.id.content).getRootView().getWindowToken(), 0);



                    destinationLocation = place.getLatLng();
                    destination = place;
                    if (destinationMarker != null) {
                        destinationMarker.remove();
                    }

                    destinationMarker = mMap.addMarker(new MarkerOptions().position(destinationLocation).title("Destination").icon(BitmapDescriptorFactory.fromResource(R.drawable.location)));
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(destinationLocation));

                }
                if(selectedModeOfTransport.equals(modeOfTransport.get(0))) {
                    travelMode = TravelMode.DRIVING;
                }
                else if(selectedModeOfTransport.equals(modeOfTransport.get(1))) {
                    travelMode = TravelMode.BICYCLING;
                }
                else if(selectedModeOfTransport.equals(modeOfTransport.get(2))) {
                    travelMode = TravelMode.WALKING;
                }
                drawRoute(travelMode);
                ft.commitAllowingStateLoss();
                ((ProgressBar)findViewById(R.id.sourceProgressBar)).setVisibility(View.GONE);

            }).addOnFailureListener((exception) -> {
                if (exception instanceof ApiException) {
                    ApiException apiException = (ApiException) exception;
                    int statusCode = apiException.getStatusCode();x
                }
            });
        }
    };

    void getRoutes(TravelMode travelMode) {
        Log.i("FollowerActivity", "getRoute called");
        DirectionsApiRequest req = DirectionsApi.getDirections(geoApiContext, sourceLocation.latitude + "," + sourceLocation.longitude, destinationLocation.latitude + "," + destinationLocation.longitude).alternatives(true).mode(travelMode);
        try {
            polylineList = new ArrayList<List<LatLng>>();
            journeyDurationList = new ArrayList<Long>();
            sourceDestinationPolylineList = new ArrayList<Polyline>();
            sourceDestinationEncodedPolylineList = new ArrayList<EncodedPolyline>();
            DirectionsResult res = req.await();
            if (res.routes != null && res.routes.length > 0) {
                Log.i("FollowerActivity", "Number of routes: " + res.routes.length);
                for (DirectionsRoute route : res.routes) {
                    sourceDestinationEncodedPolylineList.add(route.overviewPolyline);
                    String routePolylineString = route.overviewPolyline.toString();

                    List<LatLng> path = new ArrayList();
                    List<com.google.maps.model.LatLng> coords1 = route.overviewPolyline.decodePath();
                    for (com.google.maps.model.LatLng coord1 : coords1) {
                        path.add(new LatLng(coord1.lat, coord1.lng));
                    }
                    polylineList.add(path);
                    if (route.legs != null) {
                        for (int i = 0; i < route.legs.length; i++) {
                            DirectionsLeg leg = route.legs[i];
                            Log.i("FollowerActivity", "Leg duration: " + leg.duration.inSeconds);
                            journeyDuration = leg.duration.inSeconds;
                            journeyDurationList.add(journeyDuration);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Log.e("FollowerActivity", ex.getLocalizedMessage());
        }
    }

    void drawRoute(TravelMode travelMode) {
        if((sourceLocation!=null) && (destinationLocation!=null)) {
            Log.i("FollowerActivity", "sourceLocation and destinationLocation not null");
            if (sourceDestinationPolylineList != null) {
                if (sourceDestinationPolylineList.size() > 0) {
                    for (Polyline sourceDestinationPolyline1 : sourceDestinationPolylineList) {
                        sourceDestinationPolyline1.remove();
                    }
                    if(sourceDestinationPolyline!=null) {
                        sourceDestinationPolyline.remove();
                    }
                }
                else if(sourceDestinationPolyline != null) {
                    sourceDestinationPolyline.remove();
                }
            }
            else if(sourceDestinationPolyline != null) {
                sourceDestinationPolyline.remove();
            }

            getRoutes(travelMode);

            if (polylineList.size() > 1) {
                routeSelected = false;

                ((TextView) findViewById(R.id.ETA)).setText(polylineList.size() + " " + getString(R.string.guard_routesFound));
                ((ImageButton) findViewById(R.id.AddETA)).setVisibility(View.GONE);
                ((LinearLayout) findViewById(R.id.ETAInput)).setVisibility(View.GONE);

                for (List<LatLng> polyline : polylineList) {

                    PolylineOptions opts = new PolylineOptions().addAll(polyline).color(0xFF6A2D57).width(8);
                    Polyline sourceDestinationPolyline = mMap.addPolyline(opts);
                    sourceDestinationPolyline.setClickable(true);
                    sourceDestinationPolylineList.add(sourceDestinationPolyline);
                }
            } else if (polylineList.size() > 0) {
                routeSelected = true;
                PolylineOptions opts = new PolylineOptions().addAll(polylineList.get(0)).color(0xFF6A2D57).width(8);
                sourceDestinationPolyline = mMap.addPolyline(opts);
                journeyDuration = journeyDurationList.get(0);
                sourceDestinationEncodedPolyline = sourceDestinationEncodedPolylineList.get(0);
                ((TextView) findViewById(R.id.ETA)).setText(getString(R.string.guard_eta) + " " + String.format("%.2f", (journeyDuration / 60.0)) + " min");
                ((ImageButton) findViewById(R.id.AddETA)).setVisibility(View.VISIBLE);
                ((LinearLayout) findViewById(R.id.ETAInput)).setVisibility(View.GONE);

            }
        }
    }

    @Override
    public void onBackPressed() {
        Intent backIntent = new Intent(FollowerActivity.this, WelcomeActivity.class);
        backIntent.putExtra("userDetails",userDetails);
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(findViewById(android.R.id.content).getRootView().getWindowToken(), 0);
        startActivity(backIntent);
    }
}
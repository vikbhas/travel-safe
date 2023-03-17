package com.isee_project.travelsafe;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.HashMap;

public class journey implements Parcelable {

    private String ward;
    private String wardName;
    private ArrayList<String> guardiansList;
    private LatLng source;
    private LatLng destination;
    private LatLng currentLocation;
    private String reachedDestination;
    private String journeyCompleted;
    private String sourceName;
    private String destinationName;
    private String ETA;
    private String sourceDestinationEncodedPolyline;
	private String routeDeviation;
    private String GPSTracking;
    private ArrayList<String> checkpointsList;
    private boolean SOS;
    private ArrayList<Break> breakList;
    private ArrayList<String> phoneContacts;
    private String modeOfTransport;
    private LatLng finishLocation;
	private String distance25P;
    private String distance50P;
    private String distance75P;
    private HashMap<String,journey> previousJourneys;
    private ArrayList<String> contactNames;

    public HashMap<String, journey> getPreviousJourneys() {
        return previousJourneys;
    }

    public void setPreviousJourneys(HashMap<String, journey> previousJourneys) {
        this.previousJourneys = previousJourneys;
    }

    public ArrayList<String> getPhoneContacts() {
        return phoneContacts;
    }

    public void setPhoneContacts(ArrayList<String> phoneContacts) {
        this.phoneContacts = phoneContacts;
    }

    public journey(){}

    public journey(String ward, String wardName, ArrayList<String> guardiansList, LatLng source, LatLng destination, LatLng currentLocation, String sourceName, String destinationName, String ETA, String sourceDestinationEncodedPolyline, ArrayList<String> checkpointsList, ArrayList<String> phoneContacts, String modeOfTransport, LatLng finishLocation, String distance25P, String distance50P, String distance75P, ArrayList<String> contactNames, HashMap<String, journey> previousJourneys) {
        this.ward = ward;
        this.guardiansList = guardiansList;
        this.source = source;
        this.destination = destination;
        this.currentLocation = currentLocation;
        this.reachedDestination = "false";
        this.journeyCompleted = "false";
        this.sourceName = sourceName;
        this.destinationName = destinationName;
        this.ETA = ETA;
        this.sourceDestinationEncodedPolyline = sourceDestinationEncodedPolyline;
        this.SOS = false;
		this.routeDeviation = "false";
        this.GPSTracking = "false";
        this.checkpointsList = checkpointsList;
		this.breakList = new ArrayList<Break>();
		this.phoneContacts = phoneContacts;
		this.modeOfTransport = modeOfTransport;
		this.finishLocation = finishLocation;
		this.distance25P = distance25P;
        this.distance50P = distance50P;
        this.distance75P = distance75P;
		this.contactNames = contactNames;
		this.previousJourneys = previousJourneys;
		this.wardName = wardName;
    }

    public journey(String ward, String wardName, ArrayList<String> guardiansList, LatLng source, LatLng destination, LatLng currentLocation, String reachedDestination, String journeyCompleted, String sourceName, String destinationName, String ETA, String sourceDestinationEncodedPolyline, String routeDeviation, String GPSTracking, ArrayList<String> checkpointsList, ArrayList<Break> breakList, ArrayList<String> phoneContacts, String modeOfTransport, LatLng finishLocation,String distance25P, String distance50P, String distance75P, ArrayList<String> contactNames, HashMap<String, journey> previousJourneys) {
        this.ward = ward;
        this.guardiansList = guardiansList;
        this.source = source;
        this.destination = destination;
        this.currentLocation = currentLocation;
        this.reachedDestination = reachedDestination;
        this.journeyCompleted = journeyCompleted;
        this.sourceName = sourceName;
        this.destinationName = destinationName;
        this.ETA = ETA;
        this.sourceDestinationEncodedPolyline = sourceDestinationEncodedPolyline;
		this.SOS = false;
        this.routeDeviation = routeDeviation;
        this.GPSTracking = GPSTracking;
        this.checkpointsList = checkpointsList;
		this.breakList = breakList;
		this.phoneContacts = phoneContacts;
		this.modeOfTransport = modeOfTransport;
		this.finishLocation = finishLocation;
		this.distance25P = distance25P;
        this.distance50P = distance50P;
        this.distance75P = distance75P;
		this.contactNames = contactNames;
		this.previousJourneys = previousJourneys;
		this.wardName = wardName;
    }

    protected journey(Parcel in) {
        ward = in.readString();
        wardName = in.readString();
        guardiansList = in.createStringArrayList();
        source = in.readParcelable(LatLng.class.getClassLoader());
        destination = in.readParcelable(LatLng.class.getClassLoader());
        currentLocation = in.readParcelable(LatLng.class.getClassLoader());
        reachedDestination = in.readString();
        journeyCompleted = in.readString();
        sourceName = in.readString();
        destinationName = in.readString();
        ETA = in.readString();
        sourceDestinationEncodedPolyline = in.readString();
        routeDeviation = in.readString();
        GPSTracking = in.readString();
        checkpointsList = in.createStringArrayList();
		breakList = in.readArrayList(Break.class.getClassLoader());
		phoneContacts = in.readArrayList(String.class.getClassLoader());
        modeOfTransport = in.readString();
        finishLocation = in.readParcelable(LatLng.class.getClassLoader());
		distance25P = in.readString();
        distance50P = in.readString();
        distance75P = in.readString();
		contactNames = in.readArrayList(String.class.getClassLoader());
        previousJourneys = in.readHashMap(journey.class.getClassLoader());
    }

    public static final Creator<journey> CREATOR = new Creator<journey>() {
        @Override
        public journey createFromParcel(Parcel in) {
            return new journey(in);
        }

        @Override
        public journey[] newArray(int size) {
            return new journey[size];
        }
    };

    public String getWard() { return ward; }
    public String getWardName() { return wardName; }
    public ArrayList<String> getGuardiansList() { return guardiansList; }
    public LatLng getSource() { return source; }
    public LatLng getDestination() { return destination; }
    public LatLng getCurrentLocation() { return currentLocation; }
    public String getReachedDestination() { return reachedDestination; }
    public String getJourneyCompleted() { return journeyCompleted; }
    public String getSourceName() { return sourceName; }
    public String getDestinationName() { return destinationName; }
    public String getETA() { return ETA; }
    public String getSourceDestinationEncodedPolyline() { return sourceDestinationEncodedPolyline;}
    public String getRouteDeviation() { return routeDeviation;}
    public String getGPSTracking() { return GPSTracking; }
    public ArrayList<String> getCheckpointsList() { return checkpointsList; }
    public boolean getSOS() { return SOS; }
    public ArrayList<Break> getBreakList() { return breakList; }
    public String getModeOfTransport() { return modeOfTransport; }
    public LatLng getFinishLocation() { return finishLocation; }
    public String getDistance25P() { return distance25P; }
    public String getDistance50P() { return distance50P; }
    public String getDistance75P() { return distance75P; }

    public void setWard(String ward) { this.ward = ward; }
    public void setWardName(String wardName) { this.wardName = wardName; }
    public void setGuardiansList(ArrayList<String> guardiansList) { this.guardiansList = guardiansList; }
    public void setSource(LatLng source) { this.source = source; }
    public void setDestination(LatLng destination) { this.destination = destination; }
    public void setCurrentLocation(LatLng currentLocation) { this.currentLocation = currentLocation; }
    public void setReachedDestination(String reachedDestination) { this.reachedDestination = reachedDestination; }
    public void setJourneyCompleted(String journeyCompleted) { this.journeyCompleted = journeyCompleted; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public void setDestinationName(String destinationName) { this.destinationName = destinationName; }
    public void setETA(String ETA) { this.ETA = ETA; }
    public void setSourceDestinationEncodedPolyline(String sourceDestinationEncodedPolyline) { this.sourceDestinationEncodedPolyline = sourceDestinationEncodedPolyline; }
    public void setRouteDeviation(String routeDeviation) { this.routeDeviation = routeDeviation; }
    public void setGPSTracking(String GPSTracking) { this.GPSTracking = GPSTracking; }
    public void setCheckpointsList(ArrayList<String> checkpointsList) { this.checkpointsList = checkpointsList; }
    public void setSOS(boolean SOS) { this.SOS = SOS;}
    public void setBreakList(ArrayList<Break> breakList) {  this.breakList = breakList;}
    public void setModeOfTransport(String modeOfTransport) { this.modeOfTransport = modeOfTransport; }
    public void setFinishLocation(LatLng finishLocation) { this.finishLocation = finishLocation; }
    public void setDistance25P(String distance25P) { this.distance25P = distance25P; }
    public void setDistance50P(String distance50P) { this.distance50P = distance50P; }
    public void setDistance75P(String distance75P) { this.distance75P = distance75P; }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(ward);
        dest.writeString(wardName);
        dest.writeStringList(guardiansList);
        dest.writeParcelable(source, flags);
        dest.writeParcelable(destination, flags);
        dest.writeParcelable(currentLocation, flags);
        dest.writeString(reachedDestination);
        dest.writeString(journeyCompleted);
        dest.writeString(sourceName);
        dest.writeString(destinationName);
        dest.writeString(ETA);
        dest.writeString(sourceDestinationEncodedPolyline);
        dest.writeString(routeDeviation);
        dest.writeString(GPSTracking);
        dest.writeStringList(checkpointsList);
		dest.writeList(breakList);
		dest.writeList(phoneContacts);
		dest.writeString(modeOfTransport);
        dest.writeParcelable(finishLocation, flags);
		dest.writeString(distance25P);
        dest.writeString(distance50P);
        dest.writeString(distance75P);
        dest.writeList(contactNames);
		dest.writeMap(previousJourneys);
    }

    public ArrayList<String> getContactNames() {
        return contactNames;
    }

    public void setContactNames(ArrayList<String> contactNames) {
        this.contactNames = contactNames;
    }
}

package com.isee_project.travelsafe;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.isee_project.travelsafe.R;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;

import static com.google.android.libraries.places.api.Places.createClient;

public class SourceAutocompleteSkeleton extends Fragment {

    private ArrayList<String> mLocationPrimaryName = new ArrayList<String>();
    private ArrayList<String> mLocationSecondaryName = new ArrayList<String>();
    private ArrayList<String> mLocationID = new ArrayList<String>();
    private String mUserSourceText = new String();

    private Fragment currentFragment = this;

    public static SourceAutocompleteSkeleton newInstance(String mUserSourceText) {

        SourceAutocompleteSkeleton SourceAutocompleteSkeleton = new SourceAutocompleteSkeleton();
        Bundle args = new Bundle();
        args.putString("mUserSourceText", mUserSourceText);
        SourceAutocompleteSkeleton.setArguments(args);

        return SourceAutocompleteSkeleton;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        if (getArguments() != null) {
            mUserSourceText = getArguments().getString("mUserSourceText");
        }
        return inflater.inflate(R.layout.fragment_source_autocomplete, parent, false);
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        ((ImageView)getView().findViewById(R.id.sourceFragmentBackButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getActivity().findViewById(android.R.id.content).getRootView().getWindowToken(), 0);
                getActivity().getSupportFragmentManager().beginTransaction().remove(currentFragment).commit();
            }
        });

        if(mUserSourceText!=null) {
            ((EditText)view.findViewById(R.id.sourceEditText)).setText(mUserSourceText);
            ((EditText)view.findViewById(R.id.sourceEditText)).requestFocus();
            ((EditText)view.findViewById(R.id.sourceEditText)).setSelection(((EditText)view.findViewById(R.id.sourceEditText)).getText().length());
            InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        }

        AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();

        ((EditText) getView().findViewById(R.id.sourceEditText)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String query = ((EditText) getView().findViewById(R.id.sourceEditText)).getText().toString();
                Log.i("FollowerActivity", query);
                PlacesClient placesClient = createClient(getView().getContext());
                FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                        .setSessionToken(token)
                        .setQuery(query)
                        .build();

                Task<FindAutocompletePredictionsResponse> task =
                        placesClient.findAutocompletePredictions(request);

                task.addOnSuccessListener((response) -> {
                    mLocationPrimaryName.clear();
                    mLocationSecondaryName.clear();
                    mLocationID.clear();
                    for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                        mLocationPrimaryName.add(prediction.getPrimaryText(null).toString());
                        mLocationSecondaryName.add(prediction.getSecondaryText(null).toString());
                        mLocationID.add(prediction.getPlaceId());
                    }
                    RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.sourceRecycleView);
                    RecyclerView.Adapter adapter = new AutofillRecyclerViewAdapter(mLocationPrimaryName, mLocationSecondaryName, mLocationID, view.getContext());
                    recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
                    recyclerView.setAdapter(adapter);
                });
                task.addOnFailureListener((exception) -> {
                    if (exception instanceof ApiException) {
                        ApiException apiException = (ApiException) exception;
                    }
                });
                task.addOnCompleteListener((response) -> {
                    Exception e = task.getException();
                    if (e instanceof ApiException) {
                        ApiException apiException = (ApiException) e;
                    }
                });
            }
        });
    }

}

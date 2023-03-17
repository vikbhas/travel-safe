package com.isee_project.travelsafe;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.isee_project.travelsafe.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class SelectFollowerRecyclerViewAdapter extends RecyclerView.Adapter<SelectFollowerRecyclerViewAdapter.ViewHolder> {
    private static final String TAG = "SelectFollowerRecyclerViewAdapter";


    Context context;
    ArrayList<String> friends;
    boolean isWard = false, isGuardian = false;
    String email;
    ArrayList<String> guardiansList = new ArrayList<>();
    ArrayList<String> phoneContactList;
    ArrayList<String> friendNames;
    String selectedGuardian;


    public SelectFollowerRecyclerViewAdapter(Context context, ArrayList<String> friends, ArrayList<String> phoneContactList, ArrayList<String> friendNames, String email)
    {
        this.context = context;
        this.friends = friends;
        this.email = email;
        this.phoneContactList = phoneContactList;
        this.friendNames = friendNames;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {


        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_select_guardian, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);

        return viewHolder;
    }


    @Override
    public void onBindViewHolder(@NonNull SelectFollowerRecyclerViewAdapter.ViewHolder holder, @SuppressLint("RecyclerView") int position) {

        Log.d(TAG, "onBindViewHolderSGRVA: called.");

        DatabaseReference reference1;
        reference1 = FirebaseDatabase.getInstance().getReference().child("users");

        if(!friendNames.isEmpty()) {
            holder.name.setText(friendNames.get(position));
            Picasso.get().load(R.drawable.dp_blue).into(holder.image);
        }

        else if(!phoneContactList.isEmpty()) {
            holder.name.setText(phoneContactList.get(position));
            Picasso.get().load(R.drawable.dp_blue).into(holder.image);
        }


        if(phoneContactList.contains(holder.name.getText().toString())) {
            holder.selectionState.setVisibility(View.INVISIBLE);
        }

        DatabaseReference reference;
        reference = FirebaseDatabase.getInstance().getReference().child("journeys").child(email.replace("#E2","%2E"));
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                journey currentJourney = dataSnapshot.getValue(journey.class);
                    if(dataSnapshot.exists())
                        if(currentJourney.getGuardiansList()!=null && position < friends.size())
                            if(currentJourney.getGuardiansList().contains(friends.get(position)))
                                holder.selectionState.setChecked(true);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        holder.parentlayout.findViewById(R.id.selectGuardianCheckbox).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseReference reference;
                reference = FirebaseDatabase.getInstance().getReference().child("journeys").child(email.replace("#E2","%2E"));
                reference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        selectedGuardian = friends.get(position);
                        journey currentJourney = dataSnapshot.getValue(journey.class);

                        if(holder.selectionState.isChecked()) {
                            reference.getParent().child(selectedGuardian).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    journey journey = dataSnapshot.getValue(com.isee_project.travelsafe.journey.class);
                                    if(dataSnapshot.exists()) {
                                        String journeyCompleted = journey.getJourneyCompleted();
                                        if(!journeyCompleted.equals("true")) {
                                            holder.selectionState.toggle();
                                            guardiansList.remove(selectedGuardian);
                                            isWard = true;
                                            Toast.makeText(context, (R.string.selectGuardianRecycleView_ongoing), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });

                            reference1.child(selectedGuardian).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    user guardian = dataSnapshot.getValue(user.class);
                                    assert guardian != null;
                                    if(guardian.getWard()!=null) {
                                        holder.selectionState.toggle();
                                        guardiansList.remove(selectedGuardian);
                                        isGuardian = true;
                                        Toast.makeText(context,(R.string.selectGuardianRecycleView_alreadyTracking), Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                            if((!(isGuardian && isWard)) && (!guardiansList.contains(selectedGuardian))){
                                guardiansList.add(selectedGuardian);
                                reference.child("guardiansList").setValue(guardiansList);
                            }
                        }

                        else if(!(holder.selectionState.isChecked()))
                        {
                            guardiansList.remove(selectedGuardian);
                            reference.child("guardiansList").setValue(guardiansList);
                        }


                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });


            }
        });
    }

    @Override
    public int getItemCount() {
        return friends.size() + phoneContactList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        CircleImageView image;
        TextView name;
        RelativeLayout parentlayout;
        public CheckBox selectionState;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image);
            name = itemView.findViewById(R.id.name);
            parentlayout = itemView.findViewById(R.id.parent_layout);
            selectionState = itemView.findViewById(R.id.selectGuardianCheckbox);

        }
    }


}

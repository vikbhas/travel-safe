package com.isee_project.travelsafe;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

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

class PhoneContactsRecyclerViewAdapter extends RecyclerView.Adapter<PhoneContactsRecyclerViewAdapter.ViewHolder> {

    Context context;
    ArrayList<PhoneContact> phoneContactsList;
    ArrayList<String> contactNames = new ArrayList<>();
    ArrayList<String> guardiansList = new ArrayList<>();
    String wardEmail;
    DatabaseReference reference = FirebaseDatabase.getInstance().getReference("journeys");

    public PhoneContactsRecyclerViewAdapter(Context context, ArrayList<PhoneContact> phoneContactsList, String wardEmail) {
        this.context = context;
        this.phoneContactsList = phoneContactsList;
        this.wardEmail = wardEmail;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_phone_contacts, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PhoneContact contact = phoneContactsList.get(position);


        holder.name.setText(contact.getName());
        holder.phoneNumber.setText(contact.getPhoneNumber());

        if(contact.getPhoto() != null) {
            Picasso.get().load(contact.getPhoto()).into(holder.photo);
        }
        else {
            holder.photo.setImageResource(R.drawable.dp_blue);
        }



        holder.checkBox.setOnCheckedChangeListener(null);

        reference.child(wardEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                journey currentJourney;
                if(dataSnapshot.exists() && dataSnapshot.getValue()!=null)
                    currentJourney = dataSnapshot.getValue(journey.class);
                else
                    currentJourney = new journey();
                if(currentJourney.getPhoneContacts() != null) {
                    guardiansList = currentJourney.getPhoneContacts();
                    if(guardiansList.contains(contact.getPhoneNumber()))
                        holder.checkBox.setChecked(true);
                    else
                        holder.checkBox.setChecked(false);
                }
                if(currentJourney.getContactNames()!=null) {
                    contactNames = currentJourney.getContactNames();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        holder.checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(holder.checkBox.isChecked()) {
                    guardiansList.add(contact.getPhoneNumber());
                    contactNames.add(contact.getName());
                }
                else {
                    guardiansList.remove(contact.getPhoneNumber());
                    contactNames.remove(contact.getName());
                }
                reference.child(wardEmail).child("phoneContacts").setValue(guardiansList);
                reference.child(wardEmail).child("contactNames").setValue(contactNames);
            }
        });



    }


    @Override
    public int getItemCount() {
        return phoneContactsList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView phoneNumber;
        CircleImageView photo;
        CheckBox checkBox;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            phoneNumber = itemView.findViewById(R.id.phoneNumber);
            photo = itemView.findViewById(R.id.image);
            checkBox = itemView.findViewById(R.id.checkBox);
        }
    }
}

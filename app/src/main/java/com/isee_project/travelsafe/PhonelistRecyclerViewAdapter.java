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


public class PhonelistRecyclerViewAdapter extends RecyclerView.Adapter<PhonelistRecyclerViewAdapter.ViewHolder> {
    private static final String TAG = "PhonelistRecyclerViewAdapter";


    Context context;
    ArrayList<user> users;
    String email;
    CheckBox friendCheck;


    int duration = Toast.LENGTH_SHORT;


    public PhonelistRecyclerViewAdapter(Context context , ArrayList<user> users,String email)
    {
        this.context = context;
        this.users = users;
        this.email = email;
    }



    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_contactslist, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);

        return viewHolder;
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder: called.");

        holder.name.setText(users.get(position).getName());

        Picasso.get().load(users.get(position).getDp()).into(holder.image);

        holder.parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseReference reference;
                reference = FirebaseDatabase.getInstance().getReference("users");
                user addedUser = users.get(position);
                reference.child(email).addListenerForSingleValueEvent(new ValueEventListener() {
                    @SuppressLint("ResourceAsColor")
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        user dbUserDetails =  dataSnapshot.getValue(user.class);
                        ArrayList<String> newFriendsList = dbUserDetails.getFriends();
                        if(newFriendsList==null) {
                            newFriendsList = new ArrayList<String>();
                            newFriendsList.add(addedUser.encodedEmail());
                            dbUserDetails.setFriends(newFriendsList);
                            reference.child(email).setValue(dbUserDetails);
                            Toast toast = Toast.makeText(context, R.string.contactsListRecyclerView_friendAdded, duration);
                            toast.show();
                        }
                        else if(newFriendsList.contains(users.get(position).encodedEmail())) {
                            Toast toast = Toast.makeText(context, R.string.contactsListRecyclerVIew_alreadyAdded, duration);
                            toast.show();

                        }
                        else {
                            newFriendsList.add(addedUser.encodedEmail());
                            dbUserDetails.setFriends(newFriendsList);
                            reference.child(email).setValue(dbUserDetails);
                            Toast toast = Toast.makeText(context, R.string.contactsListRecyclerView_friendAdded, duration);
                            toast.show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {}
                });
            }
        });



    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        CircleImageView image;
        TextView name;
        RelativeLayout parentLayout;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image);
            name = itemView.findViewById(R.id.name);
            parentLayout = itemView.findViewById(R.id.parent_layout);

        }

    }

}


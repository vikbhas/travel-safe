package com.isee_project.travelsafe;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
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

public class FriendsListRecyclerViewAdapter extends RecyclerView.Adapter<FriendsListRecyclerViewAdapter.ViewHolder> {
    private static final String TAG = "FriendsListRecyclerViewAdapter";


    Context context;
    ArrayList<String> friends;
    user user;
    String email;


    public FriendsListRecyclerViewAdapter(Context context, ArrayList<String> friends, String email)
    {
        this.context = context;
        this.friends = friends;
        this.email = email;
    }



    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_friends_list, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);

        return viewHolder;
    }



    @Override
    public void onBindViewHolder(@NonNull FriendsListRecyclerViewAdapter.ViewHolder holder, int position) {

        DatabaseReference reference;
        reference = FirebaseDatabase.getInstance().getReference().child("users");
        reference.child(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                user dbUserDetails = dataSnapshot.getValue(user.class);
                String friendEmail;
                assert dbUserDetails.getFriends() != null;
                if (!(dbUserDetails.getFriends().isEmpty())) {
                    friendEmail = dbUserDetails.getFriends().get(position);
                    DatabaseReference reference2 = reference.child(friendEmail.replace("#E2", "%2E"));
                    reference2.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            user friendDetails = dataSnapshot.getValue(user.class);
                            String friendName = friendDetails.getName();
                            holder.name.setText(friendName);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                        }
                    });

                    Picasso.get().load(dbUserDetails.getDp()).into(holder.image);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });

    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        CircleImageView image;
        TextView name;
        RelativeLayout parentlayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image);
            name = itemView.findViewById(R.id.name);
            parentlayout = itemView.findViewById(R.id.parent_layout);

        }
    }

}

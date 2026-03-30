package com.example.zappatrack.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.zappatrack.R;
import com.example.zappatrack.databaseitems.UserProfile;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying users in RecyclerView
 */
public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private List<UserProfile> users = new ArrayList<>();
    private final Context context;
    private OnUserClickListener clickListener;

    // Callback interface
    public interface OnUserClickListener {
        void onUserClick(UserProfile user);
    }

    public UserAdapter(Context context) {
        this.context = context;
    }

    public void setOnUserClickListener(OnUserClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserProfile user = users.get(position);

        // Set user info
        holder.nameText.setText(user.getName());

        // Show bio preview if available
        if (user.getBio() != null && !user.getBio().isEmpty()) {
            holder.bioText.setText(user.getBio());
            holder.bioText.setVisibility(View.VISIBLE);
        } else {
            holder.bioText.setVisibility(View.GONE);
        }

        // Show role badge if admin
        if (user.getRole() != null && user.getRole().equals("admin")) {
            holder.adminBadge.setVisibility(View.VISIBLE);
        } else {
            holder.adminBadge.setVisibility(View.GONE);
        }

        // Load profile photo
        if (holder.profileImage != null) {
            String photoUrl = user.getProfilePhotoUrl();
            holder.profileImage.setImageTintList(null); // Remove tint for photos

            if (photoUrl != null && !photoUrl.isEmpty()) {
                Picasso.get()
                        .load(photoUrl)
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .error(R.drawable.ic_launcher_foreground)
                        .into(holder.profileImage);
            } else {
                holder.profileImage.setImageResource(R.drawable.ic_launcher_foreground);
            }
        }

        // Click listener
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onUserClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    /**
     * Update the users list
     */
    public void setUsers(List<UserProfile> users) {
        this.users = users != null ? users : new ArrayList<>();
        notifyDataSetChanged();
    }

    // ViewHolder class
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView nameText;
        TextView bioText;
        TextView adminBadge;

        ViewHolder(View view) {
            super(view);
            profileImage = view.findViewById(R.id.userProfileImage);
            nameText = view.findViewById(R.id.userNameText);
            bioText = view.findViewById(R.id.userBioText);
            adminBadge = view.findViewById(R.id.adminBadge);
        }
    }
}
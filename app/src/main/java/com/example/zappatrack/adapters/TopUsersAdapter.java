package com.example.zappatrack.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.zappatrack.R;
import com.example.zappatrack.LeaderboardEntry;
import com.google.android.material.imageview.ShapeableImageView;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Adapter for top users (most lighters owned) leaderboard
 */
public class TopUsersAdapter extends BaseAdapter {
    private Context context;
    private List<LeaderboardEntry.TopUser> users;
    private LayoutInflater inflater;

    public TopUsersAdapter(Context context, List<LeaderboardEntry.TopUser> users) {
        this.context = context;
        this.users = users;
        this.inflater = LayoutInflater.from(context);
    }

    public void updateData(List<LeaderboardEntry.TopUser> users) {
        this.users = users;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return users.size();
    }

    @Override
    public Object getItem(int position) {
        return users.get(position);
    }

    @Override
    public long getItemId(int position) {
        return users.get(position).getUserId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_top_user, parent, false);
            holder = new ViewHolder();
            holder.rankText = convertView.findViewById(R.id.rankText);
            holder.profileImage = convertView.findViewById(R.id.profileImageView);
            holder.userNameText = convertView.findViewById(R.id.userNameText);
            holder.lighterCountText = convertView.findViewById(R.id.lighterCountText);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        LeaderboardEntry.TopUser user = users.get(position);

        // Set rank
        holder.rankText.setText("#" + (position + 1));

        // Set user name
        holder.userNameText.setText(user.getUserName());

        // Set lighter count
        String countText = user.getLighterCount() + " lighters";
        holder.lighterCountText.setText(countText);

        // Load profile photo
        if (user.getProfilePhotoUrl() != null && !user.getProfilePhotoUrl().isEmpty()) {
            Picasso.get()
                    .load(user.getProfilePhotoUrl())
                    .placeholder(android.R.drawable.ic_menu_myplaces)
                    .error(android.R.drawable.ic_menu_myplaces)
                    .fit()
                    .centerCrop()
                    .into(holder.profileImage);
        } else {
            holder.profileImage.setImageResource(android.R.drawable.ic_menu_myplaces);
        }

        // Set background color for top 3
        if (position == 0) {
            convertView.setBackgroundColor(0x44FFD700); // Gold tint
        } else if (position == 1) {
            convertView.setBackgroundColor(0x44C0C0C0); // Silver tint
        } else if (position == 2) {
            convertView.setBackgroundColor(0x44CD7F32); // Bronze tint
        } else {
            convertView.setBackgroundColor(0x00000000); // Transparent
        }

        return convertView;
    }

    static class ViewHolder {
        TextView rankText;
        ShapeableImageView profileImage;
        TextView userNameText;
        TextView lighterCountText;
    }
}
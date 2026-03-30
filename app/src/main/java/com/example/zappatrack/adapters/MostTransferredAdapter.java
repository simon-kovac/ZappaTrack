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

public class MostTransferredAdapter extends BaseAdapter {
    private Context context;
    private List<LeaderboardEntry.TransferredLighter> lighters;
    private LayoutInflater inflater;

    public MostTransferredAdapter(Context context, List<LeaderboardEntry.TransferredLighter> lighters) {
        this.context = context;
        this.lighters = lighters;
        this.inflater = LayoutInflater.from(context);
    }

    public void updateData(List<LeaderboardEntry.TransferredLighter> lighters) {
        this.lighters = lighters;
        notifyDataSetChanged();
    }

    @Override public int getCount() { return lighters.size(); }
    @Override public Object getItem(int position) { return lighters.get(position); }
    @Override public long getItemId(int position) { return lighters.get(position).getLighterId(); }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_transferred_lighter, parent, false);
            holder = new ViewHolder();
            holder.rankText = convertView.findViewById(R.id.rankText);
            holder.lighterImage = convertView.findViewById(R.id.lighterImageView);
            holder.lighterNameText = convertView.findViewById(R.id.lighterNameText);
            holder.transferCountText = convertView.findViewById(R.id.transferCountText);
            holder.currentOwnerText = convertView.findViewById(R.id.currentOwnerText);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        LeaderboardEntry.TransferredLighter lighter = lighters.get(position);

        holder.rankText.setText("#" + (position + 1));
        holder.lighterNameText.setText(lighter.getLighterName());
        holder.transferCountText.setText(lighter.getTransferCount() + " transfers");
        holder.currentOwnerText.setText("Owner: " + lighter.getCurrentOwnerName());

        // Načti fotku pokud existuje, jinak zobraz placeholder
        if (lighter.getImageUrl() != null && !lighter.getImageUrl().isEmpty()) {
            holder.lighterImage.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            Picasso.get()
                    .load(lighter.getImageUrl())
                    .placeholder(android.R.drawable.ic_menu_camera)
                    .error(android.R.drawable.ic_menu_camera)
                    .fit()
                    .centerCrop()
                    .into(holder.lighterImage);
        } else {
            holder.lighterImage.setScaleType(android.widget.ImageView.ScaleType.CENTER);
            holder.lighterImage.setImageResource(android.R.drawable.ic_menu_camera);
        }


        if (position == 0) {
            convertView.setBackgroundColor(0x44FFD700);
        } else if (position == 1) {
            convertView.setBackgroundColor(0x44C0C0C0);
        } else if (position == 2) {
            convertView.setBackgroundColor(0x44CD7F32);
        } else {
            convertView.setBackgroundColor(0x00000000);
        }

        return convertView;
    }

    static class ViewHolder {
        TextView rankText;
        ShapeableImageView lighterImage;
        TextView lighterNameText;
        TextView transferCountText;
        TextView currentOwnerText;
    }
}
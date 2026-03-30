package com.example.zappatrack.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.zappatrack.R;
import com.example.zappatrack.databaseitems.LighterImage;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Map;

/**
 * Adapter for displaying lighter images in a grid
 */
public class GalleryAdapter extends BaseAdapter {
    private Context context;
    private List<LighterImage> images;
    private Map<Long, String> uploaderNames;
    private LayoutInflater inflater;

    public GalleryAdapter(Context context, List<LighterImage> images, Map<Long, String> uploaderNames) {
        this.context = context;
        this.images = images;
        this.uploaderNames = uploaderNames;
        this.inflater = LayoutInflater.from(context);
    }

    public void updateData(List<LighterImage> images, Map<Long, String> uploaderNames) {
        this.images = images;
        this.uploaderNames = uploaderNames;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return images.size();
    }

    @Override
    public Object getItem(int position) {
        return images.get(position);
    }

    @Override
    public long getItemId(int position) {
        return images.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_gallery_photo, parent, false);
            holder = new ViewHolder();
            holder.photoImage = convertView.findViewById(R.id.photoImage);
            holder.uploaderText = convertView.findViewById(R.id.uploaderText);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        LighterImage image = images.get(position);

        // Load image
        Picasso.get()
                .load(image.getImageUrl())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .fit()
                .centerCrop()
                .into(holder.photoImage);

        // Set uploader name
        String uploaderName = uploaderNames.get(image.getUploadedBy());
        if (uploaderName != null) {
            holder.uploaderText.setText("by " + uploaderName);
        } else {
            holder.uploaderText.setText("by User " + image.getUploadedBy());
        }

        return convertView;
    }

    static class ViewHolder {
        ImageView photoImage;
        TextView uploaderText;
    }
}
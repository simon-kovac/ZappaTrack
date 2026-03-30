package com.example.zappatrack.screens;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.zappatrack.R;
import com.example.zappatrack.databaseitems.LighterImage;
import com.squareup.picasso.Picasso;

/**
 * Full screen dialog to show photo details
 */
public class PhotoDetailDialog extends Dialog {

    public PhotoDetailDialog(Context context, LighterImage image, String uploaderName) {
        super(context);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_photo_detail);

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
            window.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT);
        }

        ImageView photoImageView = findViewById(R.id.fullPhotoImage);
        TextView descriptionText = findViewById(R.id.photoDescriptionText);
        TextView uploaderText = findViewById(R.id.photoUploaderText);
        TextView dateText = findViewById(R.id.photoDateText);
        Button closeButton = findViewById(R.id.closeButton);

        // Load full image
        Picasso.get()
                .load(image.getImageUrl())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(photoImageView);

        // Set description
        if (image.getDescription() != null && !image.getDescription().isEmpty()) {
            descriptionText.setText(image.getDescription());
            descriptionText.setVisibility(View.VISIBLE);
        } else {
            descriptionText.setVisibility(View.GONE);
        }

        // Set uploader
        uploaderText.setText("Uploaded by: " + (uploaderName != null ? uploaderName : "Unknown"));

        // Set date
        if (image.getUploadedAt() != null) {
            String date = formatDate(image.getUploadedAt());
            dateText.setText(date);
        }

        // Close button
        closeButton.setOnClickListener(v -> dismiss());

        // Click on image to close
        photoImageView.setOnClickListener(v -> dismiss());
    }

    private String formatDate(String timestamp) {
        if (timestamp.contains("T")) {
            String[] parts = timestamp.split("T");
            String date = parts[0];
            String time = parts[1].split("\\.")[0];
            return date + " at " + time.substring(0, 5);
        }
        return timestamp;
    }
}
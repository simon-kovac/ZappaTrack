package com.example.zappatrack.screens;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.zappatrack.AuthSession;
import com.example.zappatrack.adapters.GalleryAdapter;
import com.example.zappatrack.R;
import com.example.zappatrack.SupabaseAuthManager;
import com.example.zappatrack.SupabaseClient;
import com.example.zappatrack.databaseitems.LighterImage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gallery view for all photos of a specific lighter
 */
public class LighterGalleryActivity extends AppCompatActivity {
    private static final String TAG = "LighterGalleryActivity";

    // UI Elements
    private Button backButton;
    private TextView titleText;
    private TextView photoCountText;
    private GridView photoGridView;
    private ProgressBar progressBar;

    // Data
    private SupabaseClient supabaseClient;
    private AuthSession currentSession;
    private long lighterId;
    private String lighterName;
    private List<LighterImage> lighterImages = new ArrayList<>();
    private GalleryAdapter galleryAdapter;
    private Map<Long, String> uploaderNames = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lighter_gallery);

        // Get data from intent
        lighterId = getIntent().getLongExtra("lighter_id", -1);
        lighterName = getIntent().getStringExtra("lighter_name");

        if (lighterId == -1) {
            Toast.makeText(this, "Invalid lighter", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize
        SupabaseAuthManager authManager = SupabaseAuthManager.getInstance();
        supabaseClient = new SupabaseClient();
        currentSession = authManager.getCurrentSession();

        if (currentSession != null) {
            supabaseClient.setAuthToken(currentSession.getAccessToken());
        }

        initializeViews();
        loadLighterImages();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        titleText = findViewById(R.id.titleText);
        photoCountText = findViewById(R.id.photoCountText);
        photoGridView = findViewById(R.id.photoGridView);
        progressBar = findViewById(R.id.progressBar);

        // Set title
        titleText.setText(lighterName != null ? lighterName + " Gallery" : "Photo Gallery");

        // Back button
        backButton.setOnClickListener(v -> finish());

        // Set up gallery adapter
        galleryAdapter = new GalleryAdapter(this, lighterImages, uploaderNames);
        photoGridView.setAdapter(galleryAdapter);

        // Item click listener for full screen view
        photoGridView.setOnItemClickListener((parent, view, position, id) -> {
            LighterImage image = lighterImages.get(position);
            showPhotoDetail(image);
        });
    }

    private void loadLighterImages() {
        showLoading(true);

        supabaseClient.getLighterImagesWithUploaders(lighterId, new SupabaseClient.DataCallback() {
            @Override
            public void onSuccess(Object data) {
                if (data instanceof SupabaseClient.ImagesWithUploaders) {
                    SupabaseClient.ImagesWithUploaders result = (SupabaseClient.ImagesWithUploaders) data;
                    lighterImages = result.images;
                    uploaderNames = result.uploaderNames;

                    runOnUiThread(() -> {
                        galleryAdapter.updateData(lighterImages, uploaderNames);
                        photoCountText.setText(lighterImages.size() + " Photos");
                        showLoading(false);

                        if (lighterImages.isEmpty()) {
                            Toast.makeText(LighterGalleryActivity.this,
                                    "No photos yet", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(LighterGalleryActivity.this,
                                "Failed to load images", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to load images: " + error);
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(LighterGalleryActivity.this,
                            "Failed to load gallery", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showPhotoDetail(LighterImage image) {
        // Create dialog or new activity to show full image with details
        PhotoDetailDialog dialog = new PhotoDetailDialog(this, image,
                uploaderNames.get(image.getUploadedBy()));
        dialog.show();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        photoGridView.setVisibility(show ? View.GONE : View.VISIBLE);
    }
}
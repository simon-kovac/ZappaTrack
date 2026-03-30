package com.example.zappatrack.screens;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.zappatrack.AuthSession;
import com.example.zappatrack.R;
import com.example.zappatrack.SupabaseAuthManager;
import com.example.zappatrack.SupabaseClient;
import com.example.zappatrack.databaseitems.Lighter;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Add Lighter screen with optional photo upload - FIXED VERSION
 */
public class AddLighterActivity extends AppCompatActivity {

    private static final String TAG = "AddLighterActivity";
    private static final int PICK_IMAGE_REQUEST = 1;

    // UI elements
    private EditText nameEditText;
    private EditText descriptionEditText;
    private Button saveButton;
    private TextView backButton;
    private Button uploadImageButton;
    private ImageView selectedImageView;
    private ProgressBar progressBar;

    // Data
    private SupabaseClient supabaseClient;
    private SupabaseAuthManager authManager;
    private String encodedImage = null;
    private boolean hasSelectedImage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_lighter);

        // Initialize
        supabaseClient = new SupabaseClient();
        authManager = SupabaseAuthManager.getInstance();

        // Get current session
        AuthSession session = authManager.getCurrentSession();
        if (session != null) {
            supabaseClient.setAuthToken(session.getAccessToken());
        }

        // Find views
        nameEditText = findViewById(R.id.lighterNameEditText);
        descriptionEditText = findViewById(R.id.lighterDescriptionEditText);
        saveButton = findViewById(R.id.saveLighterButton);
        backButton = findViewById(R.id.backButton);
        uploadImageButton = findViewById(R.id.uploadImageButton);
        selectedImageView = findViewById(R.id.selectedImageView);
        progressBar = findViewById(R.id.progressBar);

        // Set up button listeners
        backButton.setOnClickListener(v -> finish());
        saveButton.setOnClickListener(v -> saveLighter());

        // Set up image upload button
        uploadImageButton.setOnClickListener(v -> selectImage());
        uploadImageButton.setEnabled(true);
        uploadImageButton.setText("📷 Add Photo (Optional)");
        uploadImageButton.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_orange_light));
    }

    private void selectImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Photo"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK &&
                data != null && data.getData() != null) {

            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

                // Resize if needed
                Bitmap resizedBitmap = resizeBitmap(bitmap, 1000);

                // Convert to base64
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                byte[] imageBytes = baos.toByteArray();
                encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
                hasSelectedImage = true;

                // Show selected image preview
                selectedImageView.setVisibility(View.VISIBLE);
                Picasso.get()
                        .load(imageUri)
                        .placeholder(android.R.drawable.ic_menu_camera)
                        .fit()
                        .centerCrop()
                        .into(selectedImageView);

                // Update button text
                uploadImageButton.setText("✓ Photo Selected (Tap to change)");

                Toast.makeText(this, "Photo selected!", Toast.LENGTH_SHORT).show();

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    private void saveLighter() {
        String name = nameEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();

        // Basic validation
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get user profile ID
        AuthSession session = authManager.getCurrentSession();
        if (session == null || !session.hasProfileId()) {
            Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show();
            return;
        }

        long ownerId = session.getProfileId();

        // Show progress
        showLoading(true);

        // Create the lighter and get its ID back
        supabaseClient.createLighterWithResponse(name, description, null, ownerId,
                new SupabaseClient.SingleItemCallback() {
                    @Override
                    public void onSuccess(Object item) {
                        Lighter createdLighter = (Lighter) item;
                        long lighterId = createdLighter.getId();

                        Log.d(TAG, "Lighter created with ID: " + lighterId);

                        // If image was selected, upload it to this lighter
                        if (hasSelectedImage && encodedImage != null) {
                            uploadImageToLighter(lighterId, encodedImage);
                        } else {
                            // No image, just finish
                            runOnUiThread(() -> {
                                showLoading(false);
                                Toast.makeText(AddLighterActivity.this,
                                        "Lighter added successfully!", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            });
                        }
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(AddLighterActivity.this,
                                    "Failed to add lighter: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void uploadImageToLighter(long lighterId, String encodedImage) {
        // Get session for user ID
        AuthSession session = authManager.getCurrentSession();
        if (session == null) {
            runOnUiThread(() -> {
                showLoading(false);
                Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        // Upload the photo to the created lighter
        supabaseClient.uploadLighterPhoto(lighterId, encodedImage, "Initial photo",
                session.getProfileId(), new SupabaseClient.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(AddLighterActivity.this,
                                    "Lighter added with photo!", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        // Photo upload failed, but lighter was created
                        runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(AddLighterActivity.this,
                                    "Lighter added, but photo upload failed: " + error,
                                    Toast.LENGTH_LONG).show();
                            setResult(RESULT_OK); // Still return OK since lighter was created
                            finish();
                        });
                    }
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        saveButton.setEnabled(!show);
        saveButton.setText(show ? "Saving..." : "Save Lighter");
        uploadImageButton.setEnabled(!show);
        nameEditText.setEnabled(!show);
        descriptionEditText.setEnabled(!show);
    }
}
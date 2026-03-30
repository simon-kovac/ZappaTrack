package com.example.zappatrack.screens;

import android.app.AlertDialog;
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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.zappatrack.AuthSession;
import com.example.zappatrack.adapters.LighterAdapter;
import com.example.zappatrack.R;
import com.example.zappatrack.SupabaseAuthManager;
import com.example.zappatrack.SupabaseClient;
import com.example.zappatrack.databaseitems.Lighter;
import com.example.zappatrack.databaseitems.UserProfile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.squareup.picasso.Picasso;


public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int LIGHTER_DETAIL_REQUEST = 200;
    private static final int MAX_BIO_LENGTH = 200;

    // UI Elements
    private TextView backButton;
    private ImageView profileImageView;
    private TextView usernameTextView;
    private TextView bioTextView;
    private TextView bioCharCountText;
    private Button editBioButton;
    private Button changePhotoButton;
    private RecyclerView lightersRecyclerView;
    private TextView lightersCountText;
    private Button addLighterButton;
    private ProgressBar progressBar;

    private TextView favoriteButton;
    private ScrollView profileContentLayout;  // This is a ScrollView

    // Data
    private SupabaseAuthManager authManager;
    private SupabaseClient supabaseClient;
    private AuthSession currentSession;
    private UserProfile userProfile;
    private List<Lighter> userLighters = new ArrayList<>();
    private LighterAdapter lighterAdapter;

    private boolean isFavorited = false;

    // Viewing mode - own profile or someone else's
    private boolean isOwnProfile = true;
    private long viewingUserId = -1;
    private String viewingUserAuthId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize managers
        authManager = SupabaseAuthManager.getInstance();
        supabaseClient = new SupabaseClient();
        currentSession = authManager.getCurrentSession();

        if (currentSession != null) {
            supabaseClient.setAuthToken(currentSession.getAccessToken());
        }

        // Check if viewing someone else's profile
        Intent intent = getIntent();
        if (intent.hasExtra("user_id")) {
            viewingUserId = intent.getLongExtra("user_id", -1);
            isOwnProfile = (viewingUserId == currentSession.getProfileId());
        } else {
            isOwnProfile = true;
            viewingUserId = currentSession.getProfileId();
        }

        // Initialize UI
        initializeViews();

        // Load profile data
        loadProfileData();
        loadUserLighters();
    }

    private void initializeViews() {
        // Find all views
        backButton = findViewById(R.id.backButton);
        profileImageView = findViewById(R.id.profileImageView);
        usernameTextView = findViewById(R.id.usernameTextView);
        bioTextView = findViewById(R.id.bioTextView);
        bioCharCountText = findViewById(R.id.bioCharCountText);
        editBioButton = findViewById(R.id.editBioButton);
        changePhotoButton = findViewById(R.id.changePhotoButton);
        lightersRecyclerView = findViewById(R.id.lightersRecyclerView);
        lightersCountText = findViewById(R.id.lightersCountText);
        addLighterButton = findViewById(R.id.addLighterButton);
        progressBar = findViewById(R.id.progressBar);
        favoriteButton = findViewById(R.id.favoriteButton);
        profileContentLayout = findViewById(R.id.profileContentLayout);  // This is a ScrollView

        // Set up back button
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Go back to previous screen
            }
        });

        // Set up RecyclerView
        lightersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        lighterAdapter = new LighterAdapter(this, currentSession);
        lightersRecyclerView.setAdapter(lighterAdapter);

        // ADD THESE NEW LISTENERS FOR EDIT AND DELETE
        lighterAdapter.setOnEditClickListener(lighter -> {
            showEditLighterDialog(lighter);
        });

        lighterAdapter.setOnDeleteClickListener(lighter -> {
            showDeleteConfirmation(lighter);
        });

        // Set up visibility based on profile ownership
        if (isOwnProfile) {
            editBioButton.setVisibility(View.VISIBLE);
            changePhotoButton.setVisibility(View.VISIBLE);
            addLighterButton.setVisibility(View.VISIBLE);

            editBioButton.setOnClickListener(v -> showEditBioDialog());
            changePhotoButton.setOnClickListener(v -> selectImage());
            profileImageView.setOnClickListener(v -> selectImage());
            addLighterButton.setOnClickListener(v -> {
                // Navigate to AddLighterActivity
                Intent intent = new Intent(ProfileActivity.this, AddLighterActivity.class);
                startActivityForResult(intent, 100); // Use result to refresh lighters list
            });
        } else {
            editBioButton.setVisibility(View.GONE);
            changePhotoButton.setVisibility(View.GONE);
            addLighterButton.setVisibility(View.GONE);
            favoriteButton.setVisibility(View.VISIBLE);
            favoriteButton.setOnClickListener(v -> toggleFavorite());
            checkIfFavorited();
        }

        // IMPORTANT: Set up lighter click listener to navigate to detail view
        lighterAdapter.setOnLighterClickListener(lighter -> {
            // Navigate to lighter detail screen
            Intent intent = new Intent(ProfileActivity.this, LighterDetailActivity.class);
            intent.putExtra("lighter_id", lighter.getId());
            startActivityForResult(intent, LIGHTER_DETAIL_REQUEST);
        });
    }

    private void loadProfileData() {
        if (isOwnProfile && currentSession != null) {
            // Load own profile from database since AuthSession doesn't have all the data
            supabaseClient.getUserProfile(currentSession.getProfileId(), new SupabaseClient.SingleItemCallback() {
                @Override
                public void onSuccess(Object item) {
                    userProfile = (UserProfile) item;
                    runOnUiThread(() -> {
                        // Use correct variable name: usernameTextView
                        usernameTextView.setText(userProfile.getName());

                        // Set bio
                        if (userProfile.getBio() != null && !userProfile.getBio().isEmpty()) {
                            bioTextView.setText(userProfile.getBio());
                            updateBioCharCount(userProfile.getBio().length());
                        } else {
                            bioTextView.setText("No bio yet");
                            updateBioCharCount(0);
                        }

                        // Load profile photo if URL exists
                        String photoUrl = userProfile.getProfilePhotoUrl();
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Picasso.get()
                                    .load(photoUrl)
                                    .placeholder(android.R.drawable.ic_menu_myplaces)
                                    .error(android.R.drawable.ic_menu_report_image)
                                    .fit()
                                    .centerCrop()
                                    .into(profileImageView);
                        } else {
                            profileImageView.setImageResource(android.R.drawable.ic_menu_myplaces);
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(ProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                    });
                }
            });

        } else if (viewingUserId > 0) {
            // Load other user's profile
            supabaseClient.getUserProfile(viewingUserId, new SupabaseClient.SingleItemCallback() {
                @Override
                public void onSuccess(Object item) {
                    userProfile = (UserProfile) item;
                    runOnUiThread(() -> {
                        // Use correct variable name: usernameTextView
                        usernameTextView.setText(userProfile.getName());

                        // Set bio
                        if (userProfile.getBio() != null && !userProfile.getBio().isEmpty()) {
                            bioTextView.setText(userProfile.getBio());
                            updateBioCharCount(userProfile.getBio().length());
                        } else {
                            bioTextView.setText("No bio yet");
                            updateBioCharCount(0);
                        }

                        // Load profile photo if URL exists
                        String photoUrl = userProfile.getProfilePhotoUrl();
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Picasso.get()
                                    .load(photoUrl)
                                    .placeholder(android.R.drawable.ic_menu_myplaces)
                                    .error(android.R.drawable.ic_menu_report_image)
                                    .fit()
                                    .centerCrop()
                                    .into(profileImageView);
                        } else {
                            profileImageView.setImageResource(android.R.drawable.ic_menu_myplaces);
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(ProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
    }

    private void updateUIWithProfile(UserProfile profile) {
        usernameTextView.setText(profile.getName());

        // Set bio
        if (profile.getBio() != null && !profile.getBio().isEmpty()) {
            bioTextView.setText(profile.getBio());
            updateBioCharCount(profile.getBio().length());
        } else {
            bioTextView.setText("No bio yet");
            updateBioCharCount(0);
        }

        // Load profile photo if exists
        if (profile.getProfilePhotoUrl() != null && !profile.getProfilePhotoUrl().isEmpty()) {
            // TODO: Load image from URL using your preferred image loading library
            // For now, we'll skip this
            Log.d(TAG, "Profile photo URL: " + profile.getProfilePhotoUrl());
        }
    }

    private void loadUserLighters() {
        supabaseClient.getUserLighters(viewingUserId, new SupabaseClient.DataCallback() {
            @Override
            public void onSuccess(Object data) {
                List<Lighter> lighters = (List<Lighter>) data;
                runOnUiThread(() -> {
                    userLighters = lighters;
                    lighterAdapter.setLighters(lighters);
                    lightersCountText.setText(lighters.size() + " Lighters");

                    // Show/hide empty state
                    TextView noLightersText = findViewById(R.id.noLightersText);
                    if (noLightersText != null) {
                        noLightersText.setVisibility(lighters.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    lightersCountText.setText("0 Lighters");
                    Log.e(TAG, "Failed to load lighters: " + error);
                });
            }
        });
    }

    private void showEditBioDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Bio");

        // Create custom layout for dialog
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText input = new EditText(this);
        String currentBio = bioTextView.getText().toString();
        input.setText(currentBio.equals("No bio yet") ? "" : currentBio);
        input.setMaxLines(4);
        input.setHint("Tell us about yourself...");
        layout.addView(input);

        final TextView charCount = new TextView(this);
        charCount.setText(input.getText().length() + "/" + MAX_BIO_LENGTH);
        charCount.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
        layout.addView(charCount);

        // Update char count as user types
        input.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                charCount.setText(s.length() + "/" + MAX_BIO_LENGTH);
                if (s.length() > MAX_BIO_LENGTH) {
                    charCount.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                } else {
                    charCount.setTextColor(getResources().getColor(android.R.color.darker_gray));
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newBio = input.getText().toString().trim();
            if (newBio.length() > MAX_BIO_LENGTH) {
                Toast.makeText(this, "Bio too long! Max " + MAX_BIO_LENGTH + " characters", Toast.LENGTH_SHORT).show();
                return;
            }
            updateBio(newBio);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateBio(String newBio) {
        showLoading(true);

        // Update bio in database
        supabaseClient.updateUserBio(currentSession.getProfileId(), newBio, new SupabaseClient.SimpleCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    bioTextView.setText(newBio.isEmpty() ? "No bio yet" : newBio);
                    updateBioCharCount(newBio.length());
                    showLoading(false);
                    Toast.makeText(ProfileActivity.this, "Bio updated!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(ProfileActivity.this, "Failed to update bio: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateBioCharCount(int length) {
        bioCharCountText.setText(length + "/" + MAX_BIO_LENGTH);
    }

    private void selectImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK &&
                data != null && data.getData() != null) {

            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

                // Show image immediately using Picasso (for better UX)
                Picasso.get()
                        .load(imageUri)
                        .placeholder(android.R.drawable.ic_menu_myplaces)
                        .fit()
                        .centerCrop()
                        .into(profileImageView);

                // Resize and upload
                Bitmap resizedBitmap = resizeBitmap(bitmap, 500);
                uploadProfileImage(resizedBitmap);

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkIfFavorited() {
        if (currentSession == null || viewingUserId == -1) return;

        supabaseClient.isUserFavorited(currentSession.getProfileId(), viewingUserId,
                new SupabaseClient.SingleItemCallback() {
                    @Override
                    public void onSuccess(Object item) {
                        isFavorited = (Boolean) item;
                        runOnUiThread(() -> updateFavoriteButton());
                    }

                    @Override
                    public void onError(String error) {}
                });
    }

    private void updateFavoriteButton() {
        if (isFavorited) {
            favoriteButton.setText("★");
            favoriteButton.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
        } else {
            favoriteButton.setText("☆");
            favoriteButton.setTextColor(getResources().getColor(android.R.color.white));
        }
    }

    private void toggleFavorite() {
        if (currentSession == null || viewingUserId == -1) return;
        favoriteButton.setEnabled(false);

        if (isFavorited) {
            supabaseClient.removeFavoriteUser(currentSession.getProfileId(), viewingUserId,
                    new SupabaseClient.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            isFavorited = false;
                            runOnUiThread(() -> {
                                favoriteButton.setEnabled(true);
                                updateFavoriteButton();
                                Toast.makeText(ProfileActivity.this, "Removed from friends", Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
                                favoriteButton.setEnabled(true);
                                Toast.makeText(ProfileActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
        } else {
            supabaseClient.addFavoriteUser(currentSession.getProfileId(), viewingUserId,
                    new SupabaseClient.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            isFavorited = true;
                            runOnUiThread(() -> {
                                favoriteButton.setEnabled(true);
                                updateFavoriteButton();
                                Toast.makeText(ProfileActivity.this, "Added to friends!", Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
                                favoriteButton.setEnabled(true);
                                Toast.makeText(ProfileActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
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

    private void uploadProfileImage(Bitmap bitmap) {
        showLoading(true);

        // Convert bitmap to base64
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        byte[] imageBytes = baos.toByteArray();
        String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);

        // Upload to Supabase Storage
        supabaseClient.uploadProfilePhoto(currentSession.getProfileId(), encodedImage,
                new SupabaseClient.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(ProfileActivity.this, "Profile photo updated!", Toast.LENGTH_SHORT).show();
                            // Reload profile to get the new photo URL
                            loadProfileData();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(ProfileActivity.this, "Failed to upload photo: " + error,
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    // ==================== NEW METHODS FOR EDIT/DELETE ====================

    private void showEditLighterDialog(Lighter lighter) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Lighter");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        // Name input
        TextView nameLabel = new TextView(this);
        nameLabel.setText("Name:");
        layout.addView(nameLabel);

        EditText nameEdit = new EditText(this);
        nameEdit.setText(lighter.getName());
        layout.addView(nameEdit);

        // Description input
        TextView descLabel = new TextView(this);
        descLabel.setText("Description:");
        descLabel.setPadding(0, 20, 0, 0);
        layout.addView(descLabel);

        EditText descEdit = new EditText(this);
        descEdit.setText(lighter.getDescription());
        descEdit.setMinLines(3);
        layout.addView(descEdit);

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = nameEdit.getText().toString().trim();
            String newDesc = descEdit.getText().toString().trim();

            if (!newName.isEmpty()) {
                updateLighter(lighter.getId(), newName, newDesc);
            } else {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateLighter(long lighterId, String name, String description) {
        showLoading(true);

        supabaseClient.updateLighter(lighterId, name, description, new SupabaseClient.SimpleCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(ProfileActivity.this, "Lighter updated!", Toast.LENGTH_SHORT).show();
                    loadUserLighters(); // Refresh the list
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(ProfileActivity.this, "Update failed: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showDeleteConfirmation(Lighter lighter) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Lighter")
                .setMessage("Are you sure you want to delete \"" + lighter.getName() + "\"?\nThis cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteLighter(lighter))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteLighter(Lighter lighter) {
        showLoading(true);

        supabaseClient.deleteLighter(lighter.getId(), new SupabaseClient.SimpleCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(ProfileActivity.this, "Lighter deleted", Toast.LENGTH_SHORT).show();
                    loadUserLighters(); // Refresh the list
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(ProfileActivity.this, "Delete failed: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // ==================== END NEW METHODS ====================

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (profileContentLayout != null) {
            profileContentLayout.setAlpha(show ? 0.5f : 1.0f);
        }
    }
}
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.zappatrack.AuthSession;
import com.example.zappatrack.R;
import com.example.zappatrack.SupabaseAuthManager;
import com.example.zappatrack.SupabaseClient;
import com.example.zappatrack.adapters.CommentAdapter;
import com.example.zappatrack.databaseitems.Comment;
import com.example.zappatrack.databaseitems.Lighter;
import com.example.zappatrack.databaseitems.LighterImage;

import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class LighterDetailActivity extends AppCompatActivity {
    private static final String TAG = "LighterDetailActivity";
    private static final int PICK_LIGHTER_IMAGE_REQUEST = 2;

    // UI Elements
    private TextView backButton;
    private TextView lighterNameText;
    private TextView lighterDescriptionText;
    private TextView ownerNameText;
    private ImageView mainImageView;
    private RecyclerView imageGalleryRecycler;
    private TextView editButton;
    private Button addPhotoButton;
    private Button transferOwnershipButton;
    private TextView favoriteButton;

    // Comments section
    private RecyclerView commentsRecyclerView;
    private EditText commentEditText;
    private Button postCommentButton;
    private TextView commentsCountText;

    // Progress
    private ProgressBar progressBar;
    private LinearLayout contentLayout;

    // Data
    private SupabaseAuthManager authManager;
    private SupabaseClient supabaseClient;
    private AuthSession currentSession;
    private Lighter currentLighter;
    private long lighterId;
    private boolean isOwner = false;
    private boolean isAdmin = false;
    private boolean isFavorited = false;
    private List<LighterImage> lighterImages = new ArrayList<>();

    // Adapters
    private CommentAdapter commentAdapter;
    private List<Comment> comments = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lighter_detail);

        // Get lighter ID from intent
        Intent intent = getIntent();
        lighterId = intent.getLongExtra("lighter_id", -1);

        if (lighterId == -1) {
            Toast.makeText(this, "Invalid lighter", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize managers
        authManager = SupabaseAuthManager.getInstance();
        supabaseClient = new SupabaseClient();
        currentSession = authManager.getCurrentSession();

        if (currentSession != null) {
            supabaseClient.setAuthToken(currentSession.getAccessToken());
            isAdmin = currentSession.isAdmin();
        }

        // Initialize UI
        initializeViews();

        // Load lighter data
        loadLighterDetails();
        loadComments();
        loadLighterImages(); // Load images too
    }

    private void initializeViews() {
        // Find views
        backButton = findViewById(R.id.backButton);
        lighterNameText = findViewById(R.id.lighterNameText);
        lighterDescriptionText = findViewById(R.id.lighterDescriptionText);
        ownerNameText = findViewById(R.id.ownerNameText);
        mainImageView = findViewById(R.id.mainImageView);
        imageGalleryRecycler = findViewById(R.id.imageGalleryRecycler);
        editButton = findViewById(R.id.editButton);
        addPhotoButton = findViewById(R.id.addPhotoButton);
        transferOwnershipButton = findViewById(R.id.transferOwnershipButton);

        // Comments views
        commentsRecyclerView = findViewById(R.id.commentsRecyclerView);
        commentEditText = findViewById(R.id.commentEditText);
        postCommentButton = findViewById(R.id.postCommentButton);
        commentsCountText = findViewById(R.id.commentsCountText);

        progressBar = findViewById(R.id.progressBar);
        contentLayout = findViewById(R.id.contentLayout);

        favoriteButton = findViewById(R.id.favoriteButton);
        favoriteButton.setOnClickListener(v -> toggleFavorite());
        checkIfFavorited();

        // Set up back button
        backButton.setOnClickListener(v -> finish());

        // Hide image gallery for now (will be visible when photos feature is added)
        imageGalleryRecycler.setVisibility(View.GONE);

        // Set up comments RecyclerView
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CommentAdapter(this, currentSession);
        commentsRecyclerView.setAdapter(commentAdapter);

        // Set up click listeners
        editButton.setOnClickListener(v -> showEditDialog());

        // UPDATED: Add photo button now works!
        addPhotoButton.setOnClickListener(v -> {
            if (currentSession != null) {
                selectLighterImage();
            } else {
                Toast.makeText(this, "Please sign in to add photos", Toast.LENGTH_SHORT).show();
            }
        });

        mainImageView.setOnClickListener(v -> {
            if (!lighterImages.isEmpty()) {
                Intent intent = new Intent(LighterDetailActivity.this, LighterGalleryActivity.class);
                intent.putExtra("lighter_id", lighterId);
                intent.putExtra("lighter_name", currentLighter.getName());
                startActivity(intent);
            } else {
                Toast.makeText(this, "No photos to show", Toast.LENGTH_SHORT).show();
            }
        });

        transferOwnershipButton.setOnClickListener(v -> showTransferDialog());
        postCommentButton.setOnClickListener(v -> postComment());

        // Comment delete listener (for own comments or admin)
        commentAdapter.setOnDeleteClickListener(comment -> {
            if (comment.getAuthor() == currentSession.getProfileId() || isAdmin) {
                deleteComment(comment);
            }
        });
    }

    private void loadLighterDetails() {
        showLoading(true);

        supabaseClient.getLighterById(lighterId, new SupabaseClient.SingleItemCallback() {
            @Override
            public void onSuccess(Object item) {
                currentLighter = (Lighter) item;
                runOnUiThread(() -> {
                    updateUIWithLighter(currentLighter);
                    showLoading(false);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(LighterDetailActivity.this,
                            "Failed to load lighter: " + error,
                            Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }

    private void updateUIWithLighter(Lighter lighter) {
        lighterNameText.setText(lighter.getName());
        lighterDescriptionText.setText(lighter.getDescription());

        // Check if current user is owner
        isOwner = (lighter.getOwner() == currentSession.getProfileId());

        // Show/hide owner controls
        if (isOwner || isAdmin) {
            editButton.setVisibility(View.VISIBLE);
            transferOwnershipButton.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        } else {
            editButton.setVisibility(View.GONE);
            transferOwnershipButton.setVisibility(View.GONE);
        }

        // Everyone can add photos
        addPhotoButton.setVisibility(View.VISIBLE);

        // Load owner name
        loadOwnerName(lighter.getOwner());

        // Load main image if exists, otherwise show placeholder
        if (lighter.getImageUrl() != null && !lighter.getImageUrl().isEmpty()) {
            // TODO: Load image from URL using image loading library like Glide
            mainImageView.setImageResource(android.R.drawable.ic_menu_gallery);
        } else {
            // Show placeholder
            mainImageView.setImageResource(android.R.drawable.ic_menu_camera);
        }
    }

    private void loadOwnerName(long ownerId) {
        supabaseClient.getUserProfile(ownerId, new SupabaseClient.SingleItemCallback() {
            @Override
            public void onSuccess(Object item) {
                runOnUiThread(() -> {
                    String ownerName = ((com.example.zappatrack.databaseitems.UserProfile) item).getName();
                    ownerNameText.setText("Owner: " + ownerName);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> ownerNameText.setText("Owner: Unknown"));
            }
        });
    }

    private void loadComments() {
        supabaseClient.getLighterComments(lighterId, new SupabaseClient.DataCallback() {
            @Override
            public void onSuccess(Object data) {
                if (data instanceof SupabaseClient.CommentsData) {
                    SupabaseClient.CommentsData commentsData = (SupabaseClient.CommentsData) data;
                    comments = commentsData.comments;

                    runOnUiThread(() -> {
                        // Set the username cache in adapter
                        commentAdapter.setUsernameCache(commentsData.usernames);
                        // Then set the comments
                        commentAdapter.setComments(comments);
                        commentsCountText.setText(comments.size() + " Comments");
                    });
                } else if (data instanceof List) {
                    // Fallback for old behavior
                    comments = (List<Comment>) data;
                    runOnUiThread(() -> {
                        commentAdapter.setComments(comments);
                        commentsCountText.setText(comments.size() + " Comments");
                    });
                }
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    commentsCountText.setText("0 Comments");
                });
            }
        });
    }

    // ============ PHOTO FUNCTIONALITY ============

    private void selectLighterImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Photo"), PICK_LIGHTER_IMAGE_REQUEST);
    }

    private void loadLighterImages() {
        supabaseClient.getLighterImages(lighterId, new SupabaseClient.DataCallback() {
            @Override
            public void onSuccess(Object data) {
                lighterImages = (List<LighterImage>) data;
                runOnUiThread(() -> {
                    if (!lighterImages.isEmpty()) {
                        // Show the LAST uploaded image (most recent first)
                        LighterImage latestImage = lighterImages.get(0);

                        // Load the actual image using Picasso
                        Picasso.get()
                                .load(latestImage.getImageUrl())
                                .placeholder(android.R.drawable.ic_menu_camera)
                                .error(android.R.drawable.ic_menu_report_image)
                                .fit()
                                .centerCrop()
                                .into(mainImageView);

                        // Log the URL for debugging
                        Log.d(TAG, "Loading image from URL: " + latestImage.getImageUrl());

                        // If there are multiple images, show a hint to the user
                        if (lighterImages.size() > 1) {
                            // You could add an overlay icon or text to indicate more photos
                            // For now, just log it
                            Log.d(TAG, "Gallery has " + lighterImages.size() + " photos");
                        }
                    } else {
                        // No images, show camera placeholder
                        mainImageView.setImageResource(android.R.drawable.ic_menu_camera);
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to load images: " + error);
                runOnUiThread(() -> {
                    // Show camera placeholder on error
                    mainImageView.setImageResource(android.R.drawable.ic_menu_camera);
                });
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_LIGHTER_IMAGE_REQUEST && resultCode == RESULT_OK &&
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
                String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);

                // Show dialog to add description
                showImageDescriptionDialog(encodedImage);

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

    private void showImageDescriptionDialog(String encodedImage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Photo Description");

        EditText descriptionEdit = new EditText(this);
        descriptionEdit.setHint("Enter description (optional)");
        descriptionEdit.setPadding(50, 40, 50, 40);

        builder.setView(descriptionEdit);

        builder.setPositiveButton("Upload", (dialog, which) -> {
            String description = descriptionEdit.getText().toString().trim();
            uploadLighterImage(encodedImage, description);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void uploadLighterImage(String encodedImage, String description) {
        showLoading(true);

        supabaseClient.uploadLighterPhoto(lighterId, encodedImage, description,
                currentSession.getProfileId(), new SupabaseClient.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(LighterDetailActivity.this,
                                    "Photo uploaded successfully!", Toast.LENGTH_SHORT).show();
                            loadLighterImages(); // Refresh images
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(LighterDetailActivity.this,
                                    "Upload failed: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    // ============ END PHOTO FUNCTIONALITY ============

    private void showEditDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Lighter");

        // Create custom layout for the dialog
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        // Name input
        TextView nameLabel = new TextView(this);
        nameLabel.setText("Name:");
        layout.addView(nameLabel);

        EditText nameEdit = new EditText(this);
        nameEdit.setText(currentLighter.getName());
        layout.addView(nameEdit);

        // Description input
        TextView descLabel = new TextView(this);
        descLabel.setText("Description:");
        descLabel.setPadding(0, 20, 0, 0);
        layout.addView(descLabel);

        EditText descEdit = new EditText(this);
        descEdit.setText(currentLighter.getDescription());
        descEdit.setMinLines(3);
        layout.addView(descEdit);

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = nameEdit.getText().toString().trim();
            String newDesc = descEdit.getText().toString().trim();

            if (!newName.isEmpty()) {
                updateLighter(newName, newDesc);
            }
        });

        builder.setNegativeButton("Cancel", null);

        // Add delete option for owner/admin
        if (isOwner || isAdmin) {
            builder.setNeutralButton("Delete Lighter", (dialog, which) -> {
                showDeleteConfirmation();
            });
        }

        builder.show();
    }

    private void updateLighter(String name, String description) {
        showLoading(true);

        supabaseClient.updateLighter(lighterId, name, description, new SupabaseClient.SimpleCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    lighterNameText.setText(name);
                    lighterDescriptionText.setText(description);
                    currentLighter.setName(name);
                    currentLighter.setDescription(description);
                    showLoading(false);
                    Toast.makeText(LighterDetailActivity.this, "Lighter updated!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(LighterDetailActivity.this, "Update failed: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Lighter")
                .setMessage("Are you sure? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteLighter())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteLighter() {
        showLoading(true);

        supabaseClient.deleteLighter(lighterId, new SupabaseClient.SimpleCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(LighterDetailActivity.this, "Lighter deleted", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK); // Tell previous activity to refresh
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(LighterDetailActivity.this, "Delete failed: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }


    private void postComment() {
        String commentText = commentEditText.getText().toString().trim();

        if (commentText.isEmpty()) {
            return;
        }

        postCommentButton.setEnabled(false);

        supabaseClient.postComment(lighterId, currentSession.getProfileId(), commentText,
                new SupabaseClient.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            commentEditText.setText("");
                            postCommentButton.setEnabled(true);
                            Toast.makeText(LighterDetailActivity.this, "Comment posted!", Toast.LENGTH_SHORT).show();
                            loadComments(); // Refresh comments
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            postCommentButton.setEnabled(true);
                            Toast.makeText(LighterDetailActivity.this, "Failed to post comment", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void deleteComment(Comment comment) {
        supabaseClient.deleteComment(comment.getId(), new SupabaseClient.SimpleCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(LighterDetailActivity.this, "Comment deleted", Toast.LENGTH_SHORT).show();
                    loadComments(); // Refresh
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(LighterDetailActivity.this, "Failed to delete comment", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // ============ TRANSFER OWNERSHIP METHODS ============

    private void showTransferDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Transfer Ownership");

        EditText usernameInput = new EditText(this);
        usernameInput.setHint("Enter username");
        usernameInput.setPadding(50, 40, 50, 40);

        builder.setView(usernameInput);

        builder.setPositiveButton("Transfer", (dialog, which) -> {
            String username = usernameInput.getText().toString().trim();
            if (!username.isEmpty()) {
                confirmTransfer(username);
            } else {
                Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void confirmTransfer(String username) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm")
                .setMessage("Transfer to " + username + "?")
                .setPositiveButton("Yes", (dialog, which) -> performTransfer(username))
                .setNegativeButton("No", null)
                .show();
    }

    private void performTransfer(String username) {
        showLoading(true);

        // Find user by username
        supabaseClient.getUserByUsername(username, new SupabaseClient.SingleItemCallback() {
            @Override
            public void onSuccess(Object item) {
                com.example.zappatrack.databaseitems.UserProfile profile =
                        (com.example.zappatrack.databaseitems.UserProfile) item;

                // Transfer to this user
                supabaseClient.transferLighterOwnership(lighterId, profile.getId(),
                        new SupabaseClient.SimpleCallback() {
                            @Override
                            public void onSuccess() {
                                runOnUiThread(() -> {
                                    showLoading(false);
                                    Toast.makeText(LighterDetailActivity.this,
                                            "Transferred to " + username,
                                            Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_OK);
                                    finish();
                                });
                            }

                            @Override
                            public void onError(String error) {
                                runOnUiThread(() -> {
                                    showLoading(false);
                                    Toast.makeText(LighterDetailActivity.this,
                                            "Transfer failed",
                                            Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(LighterDetailActivity.this,
                            "User not found",
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // ============ END OF TRANSFER OWNERSHIP METHODS ============

    private void checkIfFavorited() {
        if (currentSession == null) return;

        supabaseClient.isLighterFavorited(currentSession.getProfileId(), lighterId,
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
        if (currentSession == null) return;
        favoriteButton.setEnabled(false);

        if (isFavorited) {
            supabaseClient.removeFavoriteLighter(currentSession.getProfileId(), lighterId,
                    new SupabaseClient.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            isFavorited = false;
                            runOnUiThread(() -> {
                                favoriteButton.setEnabled(true);
                                updateFavoriteButton();
                                Toast.makeText(LighterDetailActivity.this, "Removed from favorites", Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
                                favoriteButton.setEnabled(true);
                                Toast.makeText(LighterDetailActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
        } else {
            supabaseClient.addFavoriteLighter(currentSession.getProfileId(), lighterId,
                    new SupabaseClient.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            isFavorited = true;
                            runOnUiThread(() -> {
                                favoriteButton.setEnabled(true);
                                updateFavoriteButton();
                                Toast.makeText(LighterDetailActivity.this, "Added to favorites!", Toast.LENGTH_SHORT).show();
                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
                                favoriteButton.setEnabled(true);
                                Toast.makeText(LighterDetailActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
        }
    }
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        contentLayout.setAlpha(show ? 0.5f : 1.0f);
    }
}
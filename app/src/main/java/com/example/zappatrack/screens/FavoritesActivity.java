package com.example.zappatrack.screens;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
import com.example.zappatrack.adapters.LighterAdapter;
import com.example.zappatrack.adapters.UserAdapter;
import com.example.zappatrack.databaseitems.Lighter;
import com.example.zappatrack.databaseitems.UserProfile;

import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity {
    private static final int PROFILE_REQUEST = 100;
    private static final int LIGHTER_DETAIL_REQUEST = 200;

    private Button friendsTabButton, lightersTabButton;
    private TextView backButton;
    private LinearLayout friendsSection, lightersSection;
    private RecyclerView friendsRecyclerView, lightersRecyclerView;
    private TextView friendsCountText, lightersCountText, noFriendsText, noLightersText;
    private ProgressBar progressBar;
    private LinearLayout contentLayout;

    private SupabaseAuthManager authManager;
    private SupabaseClient supabaseClient;
    private AuthSession currentSession;
    private List<UserProfile> favoriteUsers = new ArrayList<>();
    private List<Lighter> favoriteLighters = new ArrayList<>();

    private UserAdapter userAdapter;
    private LighterAdapter lighterAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        authManager = SupabaseAuthManager.getInstance();
        supabaseClient = new SupabaseClient();
        currentSession = authManager.getCurrentSession();

        if (currentSession != null) {
            supabaseClient.setAuthToken(currentSession.getAccessToken());
        }

        initializeViews();
        loadFavoriteUsers();
        loadFavoriteLighters();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        friendsTabButton = findViewById(R.id.friendsTabButton);
        lightersTabButton = findViewById(R.id.lightersTabButton);
        friendsSection = findViewById(R.id.friendsSection);
        lightersSection = findViewById(R.id.lightersSection);
        friendsRecyclerView = findViewById(R.id.friendsRecyclerView);
        lightersRecyclerView = findViewById(R.id.lightersRecyclerView);
        friendsCountText = findViewById(R.id.friendsCountText);
        lightersCountText = findViewById(R.id.lightersCountText);
        noFriendsText = findViewById(R.id.noFriendsText);
        noLightersText = findViewById(R.id.noLightersText);
        progressBar = findViewById(R.id.progressBar);
        contentLayout = findViewById(R.id.contentLayout);

        backButton.setOnClickListener(v -> finish());
        friendsTabButton.setOnClickListener(v -> showFriendsTab());
        lightersTabButton.setOnClickListener(v -> showLightersTab());

        friendsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        userAdapter = new UserAdapter(this);
        friendsRecyclerView.setAdapter(userAdapter);

        userAdapter.setOnUserClickListener(user -> {
            Intent intent = new Intent(FavoritesActivity.this, ProfileActivity.class);
            intent.putExtra("user_id", user.getId());
            startActivityForResult(intent, PROFILE_REQUEST);
        });

        lightersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        lighterAdapter = new LighterAdapter(this, currentSession);
        lightersRecyclerView.setAdapter(lighterAdapter);

        lighterAdapter.setOnLighterClickListener(lighter -> {
            Intent intent = new Intent(FavoritesActivity.this, LighterDetailActivity.class);
            intent.putExtra("lighter_id", lighter.getId());
            startActivityForResult(intent, LIGHTER_DETAIL_REQUEST);
        });

        showFriendsTab();
    }

    private void showFriendsTab() {
        friendsSection.setVisibility(View.VISIBLE);
        lightersSection.setVisibility(View.GONE);

        // Active tab - gold/yellow
        friendsTabButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFC857));
        friendsTabButton.setTextColor(0xFF1F2041);

        // Inactive tab - dark gray
        lightersTabButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF4A4B6A));
        lightersTabButton.setTextColor(0xFFFFFFFF);
    }

    private void showLightersTab() {
        friendsSection.setVisibility(View.GONE);
        lightersSection.setVisibility(View.VISIBLE);

        // Active tab - gold/yellow
        lightersTabButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFC857));
        lightersTabButton.setTextColor(0xFF1F2041);

        // Inactive tab - dark gray
        friendsTabButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF4A4B6A));
        friendsTabButton.setTextColor(0xFFFFFFFF);
    }

    private void loadFavoriteUsers() {
        if (currentSession == null) return;
        showLoading(true);

        supabaseClient.getFavoriteUsers(currentSession.getProfileId(), new SupabaseClient.DataCallback() {
            @Override
            public void onSuccess(Object data) {
                favoriteUsers = (List<UserProfile>) data;
                runOnUiThread(() -> {
                    showLoading(false);
                    updateFriendsUI();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(FavoritesActivity.this, "Failed to load friends", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void loadFavoriteLighters() {
        if (currentSession == null) return;

        supabaseClient.getFavoriteLighters(currentSession.getProfileId(), new SupabaseClient.DataCallback() {
            @Override
            public void onSuccess(Object data) {
                favoriteLighters = (List<Lighter>) data;
                runOnUiThread(() -> updateLightersUI());
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(FavoritesActivity.this, "Failed to load favorites", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void updateFriendsUI() {
        friendsCountText.setText(favoriteUsers.size() + " Friend" + (favoriteUsers.size() != 1 ? "s" : ""));
        if (favoriteUsers.isEmpty()) {
            friendsRecyclerView.setVisibility(View.GONE);
            noFriendsText.setVisibility(View.VISIBLE);
        } else {
            friendsRecyclerView.setVisibility(View.VISIBLE);
            noFriendsText.setVisibility(View.GONE);
            userAdapter.setUsers(favoriteUsers);
        }
    }

    private void updateLightersUI() {
        lightersCountText.setText(favoriteLighters.size() + " Favorite Lighter" + (favoriteLighters.size() != 1 ? "s" : ""));
        if (favoriteLighters.isEmpty()) {
            lightersRecyclerView.setVisibility(View.GONE);
            noLightersText.setVisibility(View.VISIBLE);
        } else {
            lightersRecyclerView.setVisibility(View.VISIBLE);
            noLightersText.setVisibility(View.GONE);
            lighterAdapter.setLighters(favoriteLighters);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFavoriteUsers();
        loadFavoriteLighters();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        contentLayout.setAlpha(show ? 0.5f : 1.0f);
    }
}
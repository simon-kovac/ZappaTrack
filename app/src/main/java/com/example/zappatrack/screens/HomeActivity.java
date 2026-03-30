package com.example.zappatrack.screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.zappatrack.AuthSession;
import com.example.zappatrack.R;
import com.example.zappatrack.SupabaseAuthManager;

/**
 * Simple Home Menu Screen
 */
public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Get auth manager
        SupabaseAuthManager authManager = SupabaseAuthManager.getInstance();

        // Setup Add Lighter button - MAIN GOAL
        Button addLighterButton = findViewById(R.id.addLighterButton);
        addLighterButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AddLighterActivity.class);
            startActivity(intent);
        });

        // Setup Profile button
        Button profileButton = findViewById(R.id.profileButton);
        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        // Setup Sign Out button with CORRECT AuthCallback
        Button signOutButton = findViewById(R.id.signOutButton);
        signOutButton.setOnClickListener(v -> {
            authManager.signOut(new SupabaseAuthManager.AuthCallback() {
                @Override
                public void onSuccess(AuthSession session) {  // FIXED: Added AuthSession parameter
                    Toast.makeText(HomeActivity.this, "Signed out", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(HomeActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Other buttons with placeholders
        Button searchButton = findViewById(R.id.searchButton);
        searchButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, SearchActivity.class);
            startActivity(intent);
        });

        Button leaderboardsButton = findViewById(R.id.leaderboardsButton);
        leaderboardsButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, LeaderboardsActivity.class);
            startActivity(intent);
        });
        Button friendsButton = findViewById(R.id.friendsButton);
        friendsButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, FavoritesActivity.class);
            startActivity(intent);
        });
    }
}
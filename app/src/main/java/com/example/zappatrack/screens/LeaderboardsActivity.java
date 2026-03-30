package com.example.zappatrack.screens;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.zappatrack.AuthSession;
import com.example.zappatrack.R;
import com.example.zappatrack.SupabaseAuthManager;
import com.example.zappatrack.SupabaseClient;
import com.example.zappatrack.adapters.MostTransferredAdapter;
import com.example.zappatrack.adapters.TopUsersAdapter;
import com.example.zappatrack.LeaderboardEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Leaderboards screen showing most transferred lighters and top collectors
 */
public class LeaderboardsActivity extends AppCompatActivity {
    private static final String TAG = "LeaderboardsActivity";

    // UI Elements
    private TextView backButton;
    private ListView mostTransferredList;
    private ListView topUsersListView;
    private ProgressBar progressBar;
    private TextView transferredEmptyText;
    private TextView usersEmptyText;

    // Data
    private SupabaseClient supabaseClient;
    private AuthSession currentSession;
    private List<LeaderboardEntry.TransferredLighter> mostTransferredLighters = new ArrayList<>();
    private List<LeaderboardEntry.TopUser> topUsers = new ArrayList<>();
    private MostTransferredAdapter transferredAdapter;
    private TopUsersAdapter topUsersAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboards);

        // Initialize
        SupabaseAuthManager authManager = SupabaseAuthManager.getInstance();
        supabaseClient = new SupabaseClient();
        currentSession = authManager.getCurrentSession();

        if (currentSession != null) {
            supabaseClient.setAuthToken(currentSession.getAccessToken());
        }

        initializeViews();
        loadLeaderboards();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        mostTransferredList = findViewById(R.id.mostTransferredList);
        topUsersListView = findViewById(R.id.topUsersListView);
        progressBar = findViewById(R.id.progressBar);
        transferredEmptyText = findViewById(R.id.transferredEmptyText);
        usersEmptyText = findViewById(R.id.usersEmptyText);

        // Back button
        backButton.setOnClickListener(v -> finish());

        // Set up adapters
        transferredAdapter = new MostTransferredAdapter(this, mostTransferredLighters);
        mostTransferredList.setAdapter(transferredAdapter);

        topUsersAdapter = new TopUsersAdapter(this, topUsers);
        topUsersListView.setAdapter(topUsersAdapter);

        // Set click listeners for navigation
        mostTransferredList.setOnItemClickListener((parent, view, position, id) -> {
            LeaderboardEntry.TransferredLighter lighter = mostTransferredLighters.get(position);
            // Navigate to lighter detail
            Intent intent = new Intent(LeaderboardsActivity.this, LighterDetailActivity.class);
            intent.putExtra("lighter_id", lighter.getLighterId());
            startActivity(intent);
        });

        topUsersListView.setOnItemClickListener((parent, view, position, id) -> {
            LeaderboardEntry.TopUser user = topUsers.get(position);
            // Navigate to user profile
            Intent intent = new Intent(LeaderboardsActivity.this, ProfileActivity.class);
            intent.putExtra("user_id", user.getUserId());
            startActivity(intent);
        });
    }

    private void loadLeaderboards() {
        showLoading(true);

        // Load most transferred lighters
        loadMostTransferredLighters();

        // Load top users
        loadTopUsers();
    }

    private void loadMostTransferredLighters() {
        supabaseClient.getMostTransferredLighters(30, new SupabaseClient.DataCallback() {
            @Override
            public void onSuccess(Object data) {
                mostTransferredLighters = (List<LeaderboardEntry.TransferredLighter>) data;
                runOnUiThread(() -> {
                    transferredAdapter.updateData(mostTransferredLighters);
                    transferredEmptyText.setVisibility(mostTransferredLighters.isEmpty() ? View.VISIBLE : View.GONE);
                    checkLoadingComplete();
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to load most transferred: " + error);
                runOnUiThread(() -> {
                    transferredEmptyText.setVisibility(View.VISIBLE);
                    transferredEmptyText.setText("Failed to load data");
                    checkLoadingComplete();
                });
            }
        });
    }

    private void loadTopUsers() {
        supabaseClient.getTopLighterOwners(30, new SupabaseClient.DataCallback() {
            @Override
            public void onSuccess(Object data) {
                topUsers = (List<LeaderboardEntry.TopUser>) data;
                runOnUiThread(() -> {
                    topUsersAdapter.updateData(topUsers);
                    usersEmptyText.setVisibility(topUsers.isEmpty() ? View.VISIBLE : View.GONE);
                    checkLoadingComplete();
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to load top users: " + error);
                runOnUiThread(() -> {
                    usersEmptyText.setVisibility(View.VISIBLE);
                    usersEmptyText.setText("Failed to load data");
                    checkLoadingComplete();
                });
            }
        });
    }

    private int loadingCounter = 2;
    private void checkLoadingComplete() {
        loadingCounter--;
        if (loadingCounter <= 0) {
            showLoading(false);
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
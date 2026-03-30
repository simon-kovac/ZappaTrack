package com.example.zappatrack.screens;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.zappatrack.AuthSession;
import com.example.zappatrack.adapters.LighterAdapter;
import com.example.zappatrack.R;
import com.example.zappatrack.SupabaseAuthManager;
import com.example.zappatrack.SupabaseClient;
import com.example.zappatrack.adapters.UserAdapter;
import com.example.zappatrack.databaseitems.Lighter;
import com.example.zappatrack.databaseitems.UserProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class SearchActivity extends AppCompatActivity {
    private static final String TAG = "SearchActivity";

    // UI Elements
    private TextView backButton;
    private EditText searchEditText;
    private ProgressBar progressBar;

    // Sections
    private LinearLayout lightersSection;
    private LinearLayout usersSection;
    private TextView lightersCountText;
    private TextView usersCountText;
    private TextView noResultsText;

    // RecyclerViews
    private RecyclerView lightersRecyclerView;
    private RecyclerView usersRecyclerView;

    // Adapters
    private LighterAdapter lighterAdapter;
    private UserAdapter userAdapter;

    // Data
    private SupabaseAuthManager authManager;
    private SupabaseClient supabaseClient;
    private AuthSession currentSession;
    private Timer searchTimer = new Timer();
    private TimerTask searchTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Initialize managers
        authManager = SupabaseAuthManager.getInstance();
        supabaseClient = new SupabaseClient();
        currentSession = authManager.getCurrentSession();

        if (currentSession != null) {
            supabaseClient.setAuthToken(currentSession.getAccessToken());
        }

        initializeViews();
        setupSearch();
    }

    private void initializeViews() {
        // Find views
        backButton = findViewById(R.id.backButton);
        searchEditText = findViewById(R.id.searchEditText);
        progressBar = findViewById(R.id.progressBar);

        lightersSection = findViewById(R.id.lightersSection);
        usersSection = findViewById(R.id.usersSection);
        lightersCountText = findViewById(R.id.lightersCountText);
        usersCountText = findViewById(R.id.usersCountText);
        noResultsText = findViewById(R.id.noResultsText);

        lightersRecyclerView = findViewById(R.id.lightersRecyclerView);
        usersRecyclerView = findViewById(R.id.usersRecyclerView);

        // Set up back button
        backButton.setOnClickListener(v -> finish());

        // Set up RecyclerViews
        lightersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        lighterAdapter = new LighterAdapter(this, currentSession);
        lightersRecyclerView.setAdapter(lighterAdapter);

        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        userAdapter = new UserAdapter(this);
        usersRecyclerView.setAdapter(userAdapter);

        // Set up click listener for lighters - navigate to detail view
        lighterAdapter.setOnLighterClickListener(lighter -> {
            Intent intent = new Intent(SearchActivity.this, LighterDetailActivity.class);
            intent.putExtra("lighter_id", lighter.getId());
            startActivity(intent);
        });

        // Set up click listener for users - navigate to ProfileActivity
        // ProfileActivity will automatically hide edit buttons when viewing someone else's profile
        userAdapter.setOnUserClickListener(user -> {
            Intent intent = new Intent(SearchActivity.this, ProfileActivity.class);
            intent.putExtra("user_id", user.getId());
            startActivity(intent);
        });

        // Initially hide sections
        lightersSection.setVisibility(View.GONE);
        usersSection.setVisibility(View.GONE);
        noResultsText.setVisibility(View.GONE);
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Cancel previous search task
                if (searchTask != null) {
                    searchTask.cancel();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();

                if (query.isEmpty()) {
                    // Clear results if search is empty
                    clearResults();
                } else {
                    // Debounce search - wait 500ms after user stops typing
                    searchTask = new TimerTask() {
                        @Override
                        public void run() {
                            performSearch(query);
                        }
                    };
                    searchTimer.schedule(searchTask, 500);
                }
            }
        });
    }

    private void performSearch(String query) {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.VISIBLE);
            noResultsText.setVisibility(View.GONE);
        });

        // Search lighters
        supabaseClient.searchLighters(query, new SupabaseClient.DataCallback() {
            @Override
            public void onSuccess(Object data) {
                List<Lighter> lighters = (List<Lighter>) data;
                runOnUiThread(() -> updateLightersSection(lighters));
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> updateLightersSection(new ArrayList<>()));
            }
        });

        // Search users
        supabaseClient.searchUsers(query, new SupabaseClient.DataCallback() {
            @Override
            public void onSuccess(Object data) {
                List<UserProfile> users = (List<UserProfile>) data;
                runOnUiThread(() -> {
                    updateUsersSection(users);
                    progressBar.setVisibility(View.GONE);
                    checkNoResults();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    updateUsersSection(new ArrayList<>());
                    progressBar.setVisibility(View.GONE);
                    checkNoResults();
                });
            }
        });
    }

    private void updateLightersSection(List<Lighter> lighters) {
        if (lighters.isEmpty()) {
            lightersSection.setVisibility(View.GONE);
        } else {
            lightersSection.setVisibility(View.VISIBLE);
            lighterAdapter.setLighters(lighters);
            lightersCountText.setText(lighters.size() + " Lighters found");
        }
    }

    private void updateUsersSection(List<UserProfile> users) {
        if (users.isEmpty()) {
            usersSection.setVisibility(View.GONE);
        } else {
            usersSection.setVisibility(View.VISIBLE);
            userAdapter.setUsers(users);
            usersCountText.setText(users.size() + " Users found");
        }
    }

    private void checkNoResults() {
        if (lightersSection.getVisibility() == View.GONE &&
                usersSection.getVisibility() == View.GONE &&
                !searchEditText.getText().toString().trim().isEmpty()) {
            noResultsText.setVisibility(View.VISIBLE);
        } else {
            noResultsText.setVisibility(View.GONE);
        }
    }

    private void clearResults() {
        lightersSection.setVisibility(View.GONE);
        usersSection.setVisibility(View.GONE);
        noResultsText.setVisibility(View.GONE);
        lighterAdapter.setLighters(new ArrayList<>());
        userAdapter.setUsers(new ArrayList<>());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchTimer != null) {
            searchTimer.cancel();
        }
    }
}
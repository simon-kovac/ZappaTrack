package com.example.zappatrack.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.zappatrack.R;
import com.example.zappatrack.databaseitems.Comment;
import com.example.zappatrack.screens.ProfileActivity;
import com.example.zappatrack.AuthSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter for displaying comments in RecyclerView
 */
public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {

    private List<Comment> comments = new ArrayList<>();
    private final Context context;
    private final AuthSession currentSession;
    private OnDeleteClickListener deleteListener;

    // Cache for usernames
    private Map<Long, String> usernameCache = new HashMap<>();

    // Callback interface for delete
    public interface OnDeleteClickListener {
        void onDeleteClick(Comment comment);
    }

    public CommentAdapter(Context context, AuthSession session) {
        this.context = context;
        this.currentSession = session;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteListener = listener;
    }

    /**
     * Set usernames cache - should be called when comments are loaded
     */
    public void setUsernameCache(Map<Long, String> cache) {
        this.usernameCache = cache;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Comment comment = comments.get(position);

        // Set comment content
        holder.contentText.setText(comment.getContent());

        // Set author name with proper username
        String authorName = usernameCache.get(comment.getAuthor());
        if (authorName == null) {
            authorName = "User " + comment.getAuthor();
        }
        holder.authorText.setText(authorName);

        // Make author name clickable to navigate to profile
        holder.authorText.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProfileActivity.class);
            intent.putExtra("user_id", comment.getAuthor());
            context.startActivity(intent);
        });

        // Format and set timestamp
        if (comment.getCreatedAt() != null) {
            // Format the timestamp nicely
            String timestamp = formatTimestamp(comment.getCreatedAt());
            holder.timestampText.setText(timestamp);
        } else {
            holder.timestampText.setText("Just now");
        }

        // Show delete button only for comment author or admin
        boolean canDelete = false;
        if (currentSession != null) {
            canDelete = (comment.getAuthor() == currentSession.getProfileId()) ||
                    currentSession.isAdmin();
        }

        holder.deleteButton.setVisibility(canDelete ? View.VISIBLE : View.GONE);

        // Set delete listener
        if (canDelete) {
            holder.deleteButton.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDeleteClick(comment);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }


    public void setComments(List<Comment> comments) {
        this.comments = comments != null ? comments : new ArrayList<>();
        notifyDataSetChanged();
    }


    private String formatTimestamp(String timestamp) {

        if (timestamp.contains("T")) {
            String[] parts = timestamp.split("T");
            String date = parts[0];
            String time = parts[1].split("\\.")[0];

            // Get current date to check if it's today
            String today = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());

            if (date.equals(today)) {
                return "Today at " + time.substring(0, 5); // Show HH:mm
            } else {
                return date + " at " + time.substring(0, 5);
            }
        }
        return timestamp;
    }

    // ViewHolder class
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView authorText;
        TextView contentText;
        TextView timestampText;
        ImageButton deleteButton;

        ViewHolder(View view) {
            super(view);
            authorText = view.findViewById(R.id.commentAuthorText);
            contentText = view.findViewById(R.id.commentContentText);
            timestampText = view.findViewById(R.id.commentTimestampText);
            deleteButton = view.findViewById(R.id.deleteCommentButton);
        }
    }
}
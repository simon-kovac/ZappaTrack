package com.example.zappatrack.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;  // Changed from Button to ImageButton
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.zappatrack.AuthSession;
import com.example.zappatrack.R;
import com.example.zappatrack.databaseitems.Lighter;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying lighters in RecyclerView
 */
public class LighterAdapter extends RecyclerView.Adapter<LighterAdapter.ViewHolder> {

    private List<Lighter> lighters = new ArrayList<>();
    private final Context context;
    private final AuthSession currentSession;
    private OnLighterClickListener clickListener;
    private OnEditClickListener editListener;
    private OnDeleteClickListener deleteListener;

    // Callback interfaces
    public interface OnLighterClickListener {
        void onLighterClick(Lighter lighter);
    }

    public interface OnEditClickListener {
        void onEdit(Lighter lighter);
    }

    public interface OnDeleteClickListener {
        void onDelete(Lighter lighter);
    }

    public LighterAdapter(Context context, AuthSession session) {
        this.context = context;
        this.currentSession = session;
    }

    public void setOnLighterClickListener(OnLighterClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnEditClickListener(OnEditClickListener listener) {
        this.editListener = listener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lighter, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Lighter lighter = lighters.get(position);

        // Set lighter info
        holder.nameText.setText(lighter.getName());
        holder.descriptionText.setText(lighter.getDescription());

        // Show owner info only for admins
        boolean isAdmin = currentSession != null && currentSession.isAdmin();
        if (isAdmin) {
            holder.ownerText.setText("Owner ID: " + lighter.getOwner());
            holder.ownerText.setVisibility(View.VISIBLE);
            holder.adminBadge.setVisibility(View.VISIBLE);
        } else {
            holder.ownerText.setVisibility(View.GONE);
            holder.adminBadge.setVisibility(View.GONE);
        }

        // Show edit/delete buttons only for owner or admin
        boolean canModify = false;
        if (currentSession != null) {
            canModify = (lighter.getOwner() == currentSession.getProfileId()) || isAdmin;
        }

        holder.editButton.setVisibility(canModify ? View.VISIBLE : View.GONE);
        holder.deleteButton.setVisibility(canModify ? View.VISIBLE : View.GONE);

        // Click listeners
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onLighterClick(lighter);
            }
        });

        if (canModify) {
            holder.editButton.setOnClickListener(v -> {
                if (editListener != null) {
                    editListener.onEdit(lighter);
                }
            });

            holder.deleteButton.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDelete(lighter);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return lighters.size();
    }

    /**
     * Update the lighters list
     */
    public void setLighters(List<Lighter> lighters) {
        this.lighters = lighters != null ? lighters : new ArrayList<>();
        notifyDataSetChanged();
    }

    // ViewHolder class
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView adminBadge;
        TextView nameText;
        TextView descriptionText;
        TextView ownerText;
        ImageButton editButton;    // FIXED: Changed from Button to ImageButton
        ImageButton deleteButton;  // FIXED: Changed from Button to ImageButton

        ViewHolder(View view) {
            super(view);
            adminBadge = view.findViewById(R.id.adminBadge);
            nameText = view.findViewById(R.id.lighterNameText);
            descriptionText = view.findViewById(R.id.lighterDescriptionText);
            ownerText = view.findViewById(R.id.lighterOwnerText);
            editButton = view.findViewById(R.id.editButton);       // Cast to ImageButton
            deleteButton = view.findViewById(R.id.deleteButton);   // Cast to ImageButton
        }
    }
}
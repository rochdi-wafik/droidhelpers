package com.iorgana.droidhelpers_project.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.iorgana.droidhelpers_project.R;
import com.iorgana.droidhelpers_project.ui.model.HelperBox;

import java.util.List;

/**
 * HelperBoxAdapter
 * -----------------------------------------------------------------------------
 * Renders the MainActivity index: one card per HelperBox.
 * Tapping "Open" starts the target demo Activity for that box.
 */
public class HelperBoxAdapter extends RecyclerView.Adapter<HelperBoxAdapter.ViewHolder> {

    private final List<HelperBox> items;

    public HelperBoxAdapter(List<HelperBox> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_helper_box, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HelperBox box = items.get(position);
        holder.txtHeader.setText(box.getHeader());
        holder.txtBody.setText(box.getBody());
        holder.btnOpen.setOnClickListener(v -> {
            Context context = v.getContext();
            context.startActivity(new Intent(context, box.getTarget()));
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtHeader;
        TextView txtBody;
        MaterialButton btnOpen;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtHeader = itemView.findViewById(R.id.txtHeader);
            txtBody = itemView.findViewById(R.id.txtBody);
            btnOpen = itemView.findViewById(R.id.btnOpen);
        }
    }
}
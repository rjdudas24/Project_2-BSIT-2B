package com.example.cisync.activities;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cisync.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EnhancedAccountabilityAdapter extends RecyclerView.Adapter<EnhancedAccountabilityAdapter.ViewHolder> {

    private List<ViewAccountabilitiesActivity.EnhancedAccountability> accountabilities;

    public EnhancedAccountabilityAdapter(List<ViewAccountabilitiesActivity.EnhancedAccountability> accountabilities) {
        this.accountabilities = accountabilities;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_enhanced_accountability, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ViewAccountabilitiesActivity.EnhancedAccountability accountability = accountabilities.get(position);

        holder.tvFeeName.setText(accountability.getFeeName());
        holder.tvAmount.setText(accountability.getAmount());

        // Set status icon and text
        if (accountability.isPaid()) {
            holder.ivStatus.setImageResource(R.drawable.ic_check);
            holder.tvStatus.setText("PAID");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
        } else {
            holder.ivStatus.setImageDrawable(null);
            holder.tvStatus.setText("UNPAID");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_red_dark));
        }

        // Set posted by information
        String postedByText = "Posted by: " + accountability.getPostedByName() + " (" + accountability.getPostedByPosition() + ")";
        holder.tvPostedBy.setText(postedByText);

        // Set date information
        String dateText = "Date: " + formatDate(accountability.getCreatedAt());
        holder.tvDate.setText(dateText);
    }

    @Override
    public int getItemCount() {
        return accountabilities.size();
    }

    private String formatDate(long timestamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        } catch (Exception e) {
            return "Unknown date";
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFeeName;
        TextView tvAmount;
        TextView tvStatus;
        TextView tvPostedBy;
        TextView tvDate;
        ImageView ivStatus;

        ViewHolder(View itemView) {
            super(itemView);
            tvFeeName = itemView.findViewById(R.id.tvFeeName);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvPostedBy = itemView.findViewById(R.id.tvPostedBy);
            tvDate = itemView.findViewById(R.id.tvDate);
            ivStatus = itemView.findViewById(R.id.ivStatus);
        }
    }
}
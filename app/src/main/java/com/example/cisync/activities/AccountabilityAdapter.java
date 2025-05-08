package com.example.cisync.activities;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cisync.R;
import com.example.cisync.activities.Accountability;

import java.util.List;

public class AccountabilityAdapter extends RecyclerView.Adapter<AccountabilityAdapter.ViewHolder> {

    private List<Accountability> accountabilities;

    public AccountabilityAdapter(List<Accountability> accountabilities) {
        this.accountabilities = accountabilities;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_accountability, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Accountability accountability = accountabilities.get(position);

        holder.tvFeeName.setText(accountability.getFeeName());
        holder.tvAmount.setText(accountability.getAmount());

        // Set check icon for paid items
        if (accountability.isPaid()) {
            holder.ivStatus.setImageResource(R.drawable.ic_check);
        } else {
            holder.ivStatus.setImageDrawable(null);
        }
    }

    @Override
    public int getItemCount() {
        return accountabilities.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFeeName;
        TextView tvAmount;
        ImageView ivStatus;

        ViewHolder(View itemView) {
            super(itemView);
            tvFeeName = itemView.findViewById(R.id.tvFeeName);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            ivStatus = itemView.findViewById(R.id.ivStatus);
        }
    }
}
package com.example.cisync.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cisync.R;
import com.example.cisync.database.DBHelper;

import java.util.ArrayList;
import java.util.List;

public class ViewAccountabilitiesActivity extends AppCompatActivity {

    private RecyclerView rvAccountabilities;
    private AccountabilityAdapter adapter;
    private List<AccountabilityItem> accountabilityItems;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_accountabilities);

        dbHelper = new DBHelper(this);

        // Initialize UI components
        rvAccountabilities = findViewById(R.id.rvAccountabilities);
        ImageButton btnBack = findViewById(R.id.btnBack);

        // Set up RecyclerView
        rvAccountabilities.setLayoutManager(new LinearLayoutManager(this));
        accountabilityItems = new ArrayList<>();
        adapter = new AccountabilityAdapter(accountabilityItems);
        rvAccountabilities.setAdapter(adapter);

        // Set up back button
        btnBack.setOnClickListener(v -> finish());

        // Load data
        loadAccountabilities();
    }

    private void loadAccountabilities() {
        // For now, we'll use hardcoded data to match your design
        // In a real app, you would fetch this from the database
        accountabilityItems.clear();

        accountabilityItems.add(new AccountabilityItem("College Fee", "₱ 123", true));
        accountabilityItems.add(new AccountabilityItem("Fines", "₱ 123", false));
        accountabilityItems.add(new AccountabilityItem("Siglakas", "₱ 123", true));
        accountabilityItems.add(new AccountabilityItem("Others", "₱ 123", false));

        adapter.notifyDataSetChanged();

        // TODO: Implement actual database loading using:
        // SQLiteDatabase db = dbHelper.getReadableDatabase();
        // Cursor cursor = db.rawQuery("SELECT description, amount, status FROM accountabilities WHERE student_id=?", new String[]{"1"});
    }

    // Data class for accountability items
    public static class AccountabilityItem {
        private String feeName;
        private String amount;
        private boolean isPaid;

        public AccountabilityItem(String feeName, String amount, boolean isPaid) {
            this.feeName = feeName;
            this.amount = amount;
            this.isPaid = isPaid;
        }

        public String getFeeName() {
            return feeName;
        }

        public String getAmount() {
            return amount;
        }

        public boolean isPaid() {
            return isPaid;
        }
    }

    // Adapter for accountability items
    private static class AccountabilityAdapter extends RecyclerView.Adapter<AccountabilityAdapter.ViewHolder> {

        private List<AccountabilityItem> items;

        public AccountabilityAdapter(List<AccountabilityItem> items) {
            this.items = items;
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
            AccountabilityItem item = items.get(position);

            holder.tvFeeName.setText(item.getFeeName());
            holder.tvAmount.setText(item.getAmount());

            // Set status display (check or checkbox)
            if (item.isPaid()) {
                holder.ivStatusPaid.setVisibility(View.VISIBLE);
                holder.checkStatus.setVisibility(View.GONE);
            } else {
                holder.ivStatusPaid.setVisibility(View.GONE);
                holder.checkStatus.setVisibility(View.VISIBLE);
                holder.checkStatus.setChecked(false);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvFeeName;
            TextView tvAmount;
            ImageView ivStatusPaid;
            CheckBox checkStatus;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvFeeName = itemView.findViewById(R.id.tvFeeName);
                tvAmount = itemView.findViewById(R.id.tvAmount);
                ivStatusPaid = itemView.findViewById(R.id.ivStatusPaid);
                checkStatus = itemView.findViewById(R.id.checkStatus);
            }
        }
    }
}
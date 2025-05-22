package com.example.cisync.activities;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.cisync.R;
import com.example.cisync.database.DBHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdminRosterActivity extends AppCompatActivity {

    TabLayout tabLayout;
    ViewPager2 viewPager;
    RosterPagerAdapter pagerAdapter;
    ImageView btnBack;

    // Same organization positions as in RegisterActivity
    public static final List<String> ORG_POSITIONS = Arrays.asList(
            "All Positions", // Added for filtering
            "Chairperson",
            "Vice-Chairperson (Internal)",
            "Vice-Chairperson (External)",
            "Secretary",
            "Associate Secretary",
            "Treasurer",
            "Associate Treasurer",
            "Auditor"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_roster);

        try {
            tabLayout = findViewById(R.id.tabLayout);
            viewPager = findViewById(R.id.viewPager);
            btnBack = findViewById(R.id.btnBackHistory);

            // Back button handler
            btnBack.setOnClickListener(v -> finish());

            // Setup ViewPager with fragments
            pagerAdapter = new RosterPagerAdapter(this);
            viewPager.setAdapter(pagerAdapter);

            // Connect TabLayout with ViewPager2
            new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
                if (position == 0) {
                    tab.setText("Faculty Roster");
                } else {
                    tab.setText("Student Roster");
                }
            }).attach();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error initializing roster view: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish(); // Go back if there's an error
        }
    }

    //Override ViewPager adapter to handle tab selection more explicitly
    private static class RosterPagerAdapter extends FragmentStateAdapter {
        public RosterPagerAdapter(AppCompatActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            // Create appropriate fragment based on position
            if (position == 0) {
                return new FacultyRosterFragment();
            } else {
                return new StudentRosterFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 2; // Two tabs: Faculty and Student
        }
    }

    // Base fragment class for roster functionality
    public static abstract class BaseRosterFragment extends Fragment {
        protected ListView lvRoster;
        protected DBHelper dbHelper;
        protected ArrayList<UserData> userList = new ArrayList<>();
        protected UserAdapter adapter;
        protected FloatingActionButton fabRefresh;

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = createView(inflater, container);

            try {
                lvRoster = view.findViewById(R.id.lvRoster);
                fabRefresh = view.findViewById(R.id.fabRefresh);
                dbHelper = new DBHelper(requireContext());

                fabRefresh.setOnClickListener(v -> loadUsers());

                setupListView();
                loadUsers();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(requireContext(), "Error loading roster: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            return view;
        }

        protected abstract View createView(LayoutInflater inflater, ViewGroup container);
        protected abstract void loadUsers();

        private void setupListView() {
            adapter = new UserAdapter(requireContext(), userList);
            lvRoster.setAdapter(adapter);

            lvRoster.setOnItemClickListener((parent, view, position, id) -> {
                UserData user = userList.get(position);
                showUserOptionsDialog(user);
            });
        }

        private void showUserOptionsDialog(UserData user) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("User Options");

            String[] options = {"Edit User Details", user.isEnabled() ? "Disable Account" : "Enable Account"};

            builder.setItems(options, (dialog, which) -> {
                if (which == 0) {
                    showEditUserDialog(user);
                } else if (which == 1) {
                    toggleUserStatus(user);
                }
            });

            builder.setNegativeButton("Cancel", null);
            builder.show();
        }

        private void showEditUserDialog(UserData user) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_user, null);
            builder.setView(dialogView);

            EditText etName = dialogView.findViewById(R.id.etEditName);
            EditText etEmail = dialogView.findViewById(R.id.etEditEmail);
            Spinner spRole = dialogView.findViewById(R.id.spEditRole);

            // Set current values
            etName.setText(user.getName());
            etEmail.setText(user.getEmail());

            // Setup role spinner
            ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(
                    requireContext(),
                    R.layout.custom_role_spinner,
                    new String[]{"Student", "Faculty", "Admin"}
            );
            roleAdapter.setDropDownViewResource(R.layout.custom_role_spinner);
            spRole.setAdapter(roleAdapter);

            // Set current role
            int rolePosition = 0;
            if (user.getRole().equals("Faculty")) rolePosition = 1;
            else if (user.getRole().equals("Admin")) rolePosition = 2;
            spRole.setSelection(rolePosition);

            builder.setPositiveButton("Save", (dialog, which) -> {
                String newName = etName.getText().toString().trim();
                String newEmail = etEmail.getText().toString().trim();
                String newRole = spRole.getSelectedItem().toString();

                updateUser(user, newName, newEmail, newRole);
            });

            builder.setNegativeButton("Cancel", null);
            builder.show();
        }

        private void updateUser(UserData user, String newName, String newEmail, String newRole) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            try {
                ContentValues values = new ContentValues();
                values.put("name", newName);
                values.put("email", newEmail);
                values.put("role", newRole);

                db.update("users", values, "id=?", new String[]{String.valueOf(user.getId())});

                // Log transaction
                ContentValues transValues = new ContentValues();
                transValues.put("user_id", user.getId());
                transValues.put("action_type", "User Update");
                transValues.put("description", "Updated user details for: " + newName);
                transValues.put("timestamp", System.currentTimeMillis());
                db.insert("transactions", null, transValues);

                Toast.makeText(requireContext(), "User updated successfully", Toast.LENGTH_SHORT).show();
                loadUsers();

            } catch (Exception e) {
                Toast.makeText(requireContext(), "Error updating user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        private void toggleUserStatus(UserData user) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            try {
                ContentValues values = new ContentValues();
                int newStatus = user.isEnabled() ? 0 : 1;
                values.put("verified", newStatus);

                db.update("users", values, "id=?", new String[]{String.valueOf(user.getId())});

                // Log transaction
                ContentValues transValues = new ContentValues();
                transValues.put("user_id", user.getId());
                transValues.put("action_type", "Account Status Change");
                transValues.put("description", (newStatus == 1 ? "Enabled" : "Disabled") + " account for: " + user.getName());
                transValues.put("timestamp", System.currentTimeMillis());
                db.insert("transactions", null, transValues);

                Toast.makeText(requireContext(),
                        "Account " + (newStatus == 1 ? "enabled" : "disabled") + " successfully",
                        Toast.LENGTH_SHORT).show();
                loadUsers();

            } catch (Exception e) {
                Toast.makeText(requireContext(), "Error updating account status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Fragment for displaying Faculty roster
    public static class FacultyRosterFragment extends BaseRosterFragment {

        @Override
        protected View createView(LayoutInflater inflater, ViewGroup container) {
            return inflater.inflate(R.layout.fragment_faculty_roster, container, false);
        }

        @Override
        protected void loadUsers() {
            userList.clear();
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            try {
                // Modified query to include all users, not just verified ones
                Cursor cursor = db.rawQuery(
                        "SELECT id, name, email, verified FROM users WHERE role='Faculty'",
                        null
                );

                if (cursor.moveToFirst()) {
                    do {
                        int id = cursor.getInt(0);
                        String name = cursor.getString(1);
                        String email = cursor.getString(2);
                        int verified = cursor.getInt(3);

                        // Add all users to the list, not just verified ones
                        UserData userData = new UserData(id, name, email, "Faculty", verified == 1);
                        userList.add(userData);
                    } while (cursor.moveToNext());
                }

                cursor.close();
                adapter.notifyDataSetChanged();

                // Update empty view if needed
                View view = getView();
                if (view != null) {
                    View emptyView = view.findViewById(R.id.tvEmptyRoster);
                    if (emptyView != null) {
                        emptyView.setVisibility(userList.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(requireContext(), "Error loading faculty users: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Fragment for displaying Student roster with organization filtering
    public static class StudentRosterFragment extends BaseRosterFragment {
        private Spinner spStudentPositionFilter;
        private Button btnStudentApplyFilter, btnStudentClearFilter;

        @Override
        protected View createView(LayoutInflater inflater, ViewGroup container) {
            View view = inflater.inflate(R.layout.fragment_student_roster, container, false);

            try {
                spStudentPositionFilter = view.findViewById(R.id.spStudentPositionFilter);
                btnStudentApplyFilter = view.findViewById(R.id.btnStudentApplyFilter);
                btnStudentClearFilter = view.findViewById(R.id.btnStudentClearFilter);

                setupOrgPositionFilter();

                btnStudentApplyFilter.setOnClickListener(v -> {
                    String selectedPosition = spStudentPositionFilter.getSelectedItem().toString();
                    if (selectedPosition.equals("All Positions")) {
                        loadUsers();
                    } else {
                        loadStudentsByPosition(selectedPosition);
                    }
                });

                btnStudentClearFilter.setOnClickListener(v -> {
                    spStudentPositionFilter.setSelection(0);
                    loadUsers();
                });
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(requireContext(), "Error setting up student roster filters: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            return view;
        }

        private void setupOrgPositionFilter() {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    requireContext(),
                    R.layout.custom_role_spinner,
                    ORG_POSITIONS
            );
            adapter.setDropDownViewResource(R.layout.custom_role_spinner);
            spStudentPositionFilter.setAdapter(adapter);
        }

        @Override
        protected void loadUsers() {
            userList.clear();
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            try {
                // Modified query to include all users, not just verified ones
                Cursor cursor = db.rawQuery(
                        "SELECT id, name, email, has_org, org_role, verified FROM users WHERE role='Student'",
                        null
                );

                if (cursor.moveToFirst()) {
                    do {
                        int id = cursor.getInt(0);
                        String name = cursor.getString(1);
                        String email = cursor.getString(2);
                        int hasOrg = cursor.getInt(3);
                        String orgRole = cursor.getString(4);
                        int verified = cursor.getInt(5);

                        // Add all users to the list, not just verified ones
                        UserData userData = new UserData(id, name, email, "Student", verified == 1);
                        userData.setHasOrg(hasOrg == 1);
                        userData.setOrgRole(orgRole);
                        userList.add(userData);
                    } while (cursor.moveToNext());
                }

                cursor.close();
                adapter.notifyDataSetChanged();

                // Update empty view if needed
                View view = getView();
                if (view != null) {
                    View emptyView = view.findViewById(R.id.tvEmptyRoster);
                    if (emptyView != null) {
                        emptyView.setVisibility(userList.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(requireContext(), "Error loading student users: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        private void loadStudentsByPosition(String position) {
            userList.clear();
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            try {
                // Modified query to include all users with the specified position, not just verified ones
                Cursor cursor = db.rawQuery(
                        "SELECT id, name, email, has_org, org_role, verified FROM users " +
                                "WHERE role='Student' AND has_org=1 AND org_role=?",
                        new String[]{position}
                );

                if (cursor.moveToFirst()) {
                    do {
                        int id = cursor.getInt(0);
                        String name = cursor.getString(1);
                        String email = cursor.getString(2);
                        int hasOrg = cursor.getInt(3);
                        String orgRole = cursor.getString(4);
                        int verified = cursor.getInt(5);

                        // Add all users to the list, not just verified ones
                        UserData userData = new UserData(id, name, email, "Student", verified == 1);
                        userData.setHasOrg(hasOrg == 1);
                        userData.setOrgRole(orgRole);
                        userList.add(userData);
                    } while (cursor.moveToNext());
                }

                cursor.close();
                adapter.notifyDataSetChanged();

                // Update empty view if needed
                View view = getView();
                if (view != null) {
                    View emptyView = view.findViewById(R.id.tvEmptyRoster);
                    if (emptyView != null) {
                        emptyView.setVisibility(userList.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(requireContext(), "Error filtering students: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Model class for user data
    public static class UserData {
        private final int id;
        private final String name;
        private final String email;
        private final String role;
        private boolean hasOrg;
        private String orgRole;
        private final boolean enabled;

        public UserData(int id, String name, String email, String role, boolean enabled) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.role = role;
            this.enabled = enabled;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public boolean isHasOrg() { return hasOrg; }
        public String getOrgRole() { return orgRole; }
        public boolean isEnabled() { return enabled; }

        public void setHasOrg(boolean hasOrg) { this.hasOrg = hasOrg; }
        public void setOrgRole(String orgRole) { this.orgRole = orgRole; }
    }

    public static class UserAdapter extends ArrayAdapter<UserData> {
        private final Context context;
        private final List<UserData> users;

        public UserAdapter(Context context, List<UserData> users) {
            super(context, R.layout.list_item_user, users);
            this.context = context;
            this.users = users;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                convertView = inflater.inflate(R.layout.list_item_user, parent, false);

                holder = new ViewHolder();
                holder.tvName = convertView.findViewById(R.id.tvUserName);
                holder.tvEmail = convertView.findViewById(R.id.tvUserEmail);
                holder.tvStatus = convertView.findViewById(R.id.tvUserStatus);
                holder.tvRole = convertView.findViewById(R.id.tvUserRole);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            UserData user = users.get(position);

            // Display user details regardless of verification status
            holder.tvName.setText(user.getName());
            holder.tvEmail.setText(user.getEmail());

            // Set status text and color based on user status
            if (user.isEnabled()) {
                holder.tvStatus.setText("Active");
                holder.tvStatus.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
            } else {
                holder.tvStatus.setText("Disabled");
                holder.tvStatus.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
            }

            // Set role text
            String roleText = user.getRole();
            if (user.getRole().equals("Student") && user.isHasOrg() && user.getOrgRole() != null) {
                roleText += " - " + user.getOrgRole();
            }
            holder.tvRole.setText(roleText);

            return convertView;
        }

        private static class ViewHolder {
            TextView tvName;
            TextView tvEmail;
            TextView tvStatus;
            TextView tvRole;
        }
    }
}
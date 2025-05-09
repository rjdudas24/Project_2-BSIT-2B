    package com.example.cisync.activities;
    
    import android.app.Activity;
    import android.database.Cursor;
    import android.database.sqlite.SQLiteDatabase;
    import android.os.Bundle;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.*;

    import androidx.annotation.NonNull;
    import androidx.fragment.app.Fragment;
    import androidx.fragment.app.FragmentActivity;
    import androidx.viewpager2.adapter.FragmentStateAdapter;
    import androidx.viewpager2.widget.ViewPager2;
    
    import com.example.cisync.R;
    import com.example.cisync.database.DBHelper;
    import com.google.android.material.tabs.TabLayout;
    import com.google.android.material.tabs.TabLayoutMediator;
    
    import java.util.ArrayList;
    import java.util.Arrays;
    import java.util.List;
    
    public class AdminRosterActivity extends FragmentActivity {
    
        TabLayout tabLayout;
        ViewPager2 viewPager;
        RosterPagerAdapter pagerAdapter;
    
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
    
            tabLayout = findViewById(R.id.tabLayout);
            viewPager = findViewById(R.id.viewPager);
    
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
        }
    
        // ViewPager adapter to manage the roster fragments
        private static class RosterPagerAdapter extends FragmentStateAdapter {
            public RosterPagerAdapter(FragmentActivity fragmentActivity) {
                super(fragmentActivity);
            }
    
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                if (position == 0) {
                    return new AdminRosterFragment();
                } else {
                    return new StudentRosterFragment();
                }
            }
    
            @Override
            public int getItemCount() {
                return 2; // Two tabs: Admin and Student
            }
        }
    
        // Fragment for displaying Admin roster
        public static class AdminRosterFragment extends Fragment {
            ListView lvAdminRoster;
            DBHelper dbHelper;
            ArrayList<String> rosterList = new ArrayList<>();
            ArrayAdapter<String> adapter;
    
            @Override
            public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
                View view = inflater.inflate(R.layout.fragment_faculty_roster, container, false);
    
                lvAdminRoster = view.findViewById(R.id.lvFacultyRoster);
                dbHelper = new DBHelper(requireContext());
    
                loadFacultyRoster();
    
                return view;
            }
    
            private void loadFacultyRoster() {
                rosterList.clear();
                SQLiteDatabase db = dbHelper.getReadableDatabase();
    
                Cursor cursor = db.rawQuery(
                        "SELECT name, email FROM users WHERE role='Faculty'",
                        null
                );
    
                if (cursor.moveToFirst()) {
                    do {
                        rosterList.add(cursor.getString(0) + " - " + cursor.getString(1));
                    } while (cursor.moveToNext());
                }
    
                cursor.close();
    
                adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, rosterList);
                lvAdminRoster.setAdapter(adapter);
            }
        }
    
        // Fragment for displaying Student roster with organization filtering
        public static class StudentRosterFragment extends Fragment {
            ListView lvStudentRoster;
            Spinner spStudentPositionFilter;
            Button btnStudentApplyFilter, btnStudentClearFilter;
            DBHelper dbHelper;
            ArrayList<String> rosterList = new ArrayList<>();
            ArrayAdapter<String> adapter;
    
            @Override
            public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
                View view = inflater.inflate(R.layout.fragment_student_roster, container, false);
    
                lvStudentRoster = view.findViewById(R.id.lvStudentRoster);
                spStudentPositionFilter = view.findViewById(R.id.spStudentPositionFilter);
                btnStudentApplyFilter = view.findViewById(R.id.btnStudentApplyFilter);
                btnStudentClearFilter = view.findViewById(R.id.btnStudentClearFilter);
    
                dbHelper = new DBHelper(requireContext());
    
                setupOrgPositionFilter();
    
                btnStudentApplyFilter.setOnClickListener(v -> {
                    String selectedPosition = spStudentPositionFilter.getSelectedItem().toString();
                    if (selectedPosition.equals("Select Filter")) {
                        loadStudentRoster(); // Load all
                    } else {
                        loadStudentRosterByPosition(selectedPosition); // Load filtered
                    }
                });
    
                btnStudentClearFilter.setOnClickListener(v -> {
                    spStudentPositionFilter.setSelection(0); // Select "All Positions"
                    loadStudentRoster();
                });
    
                loadStudentRoster();
    
                return view;
            }
    
            private void setupOrgPositionFilter() {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        ORG_POSITIONS
                );
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spStudentPositionFilter.setAdapter(adapter);
            }

            private void loadStudentRoster() {
                rosterList.clear();
                SQLiteDatabase db = dbHelper.getReadableDatabase();

                Cursor cursor = db.rawQuery(
                        "SELECT name, email, has_org, org_role FROM users WHERE role='Student'",
                        null
                );


                if (cursor.moveToFirst()) {
                    do {
                        String name = cursor.getString(0);
                        String email = cursor.getString(1);
                        int hasOrg = cursor.getInt(2);
                        String orgRole = cursor.getString(3);

                        if (hasOrg == 1 && orgRole != null) {
                            rosterList.add(name + " - " + email + " [" + orgRole + "]");
                        } else {
                            rosterList.add(name + " - " + email + " [Student]");
                        }
                    } while (cursor.moveToNext());
                }

                cursor.close();

                adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, rosterList);
                lvStudentRoster.setAdapter(adapter);
            }
    
            private void loadStudentRosterByPosition(String position) {
                rosterList.clear();
                SQLiteDatabase db = dbHelper.getReadableDatabase();
    
                Cursor cursor = db.rawQuery(
                        "SELECT name, email, org_role FROM users WHERE role='Student' AND has_org=1 AND org_role=?",
                        new String[]{position}
                );
    
                if (cursor.moveToFirst()) {
                    do {
                        String orgRole = cursor.getString(2);
                        rosterList.add(cursor.getString(0) + " - " + cursor.getString(1) + " [" + orgRole + "]");
                    } while (cursor.moveToNext());
                }
    
                cursor.close();
    
                adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, rosterList);
                lvStudentRoster.setAdapter(adapter);
            }
        }
    }
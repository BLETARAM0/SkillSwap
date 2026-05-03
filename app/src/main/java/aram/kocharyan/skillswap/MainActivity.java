package aram.kocharyan.skillswap;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private ListenerRegistration callListener;
    private FirebaseFirestore db;
    private String currentUserId;
    private boolean incomingCallShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, Register.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        bottomNav = findViewById(R.id.bottomNavigationView);

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_schedule) {
                selectedFragment = new ScheduleFragment();
            } else if (id == R.id.nav_chat) {
                selectedFragment = new ChatFragment();
            } else if (id == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (id == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
            } else if (id == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.container, selectedFragment)
                        .commit();
            }
            return true;
        });

        boolean showModeSelect = getIntent().getBooleanExtra("show_mode_select", false);
        Fragment firstFragment = showModeSelect ? new ModeSelectFragment() : new HomeFragment();

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, firstFragment)
                .commit();

        bottomNav.setVisibility(showModeSelect ? View.GONE : View.VISIBLE);
        if (!showModeSelect) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }

        listenForIncomingCalls();
    }

    // ── Incoming Call Listener ──────────────────────────────────────────────

    private void listenForIncomingCalls() {
        callListener = db.collection("Calls")
                .document(currentUserId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) {
                        incomingCallShown = false;
                        return;
                    }

                    String status     = snapshot.getString("status");
                    String callId     = snapshot.getString("callId");
                    String callerId   = snapshot.getString("callerId");
                    String callerName = snapshot.getString("callerName");

                    // Показываем экран только один раз пока статус "calling"
                    if ("calling".equals(status) && callId != null && !incomingCallShown) {
                        incomingCallShown = true;
                        Intent intent = new Intent(this, IncomingCallActivity.class);
                        intent.putExtra("callId",     callId);
                        intent.putExtra("callerId",   callerId);
                        intent.putExtra("callerName", callerName);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }

                    if (!"calling".equals(status)) {
                        incomingCallShown = false;
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        incomingCallShown = false; // сбрасываем флаг когда возвращаемся
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (callListener != null) callListener.remove();
    }

    public void showBottomNavigation() {
        if (bottomNav != null) {
            bottomNav.setVisibility(View.VISIBLE);
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    public void updateNavColor() {
        SharedPreferences prefs = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        String hexColor = prefs.getString("selected_color", "#2196F3"); // синий по дефолту
        int color = Color.parseColor(hexColor);

        int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked }, // нажата
                new int[] { -android.R.attr.state_checked } // не нажата
        };

        int[] colors = new int[] { color, Color.GRAY };
        ColorStateList csl = new ColorStateList(states, colors);

        bottomNav.setItemIconTintList(csl);
        bottomNav.setItemTextColor(csl);
    }
}
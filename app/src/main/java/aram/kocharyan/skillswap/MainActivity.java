package aram.kocharyan.skillswap;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, Register.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottomNavigationView);

        // Настройка кликов по нижней панели
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_schedule) selectedFragment = new ScheduleFragment();
            else if (id == R.id.nav_chat) selectedFragment = new ChatFragment();
            else if (id == R.id.nav_home) selectedFragment = new HomeFragment();
            else if (id == R.id.nav_settings) selectedFragment = new SettingsFragment();
            else if (id == R.id.nav_profile) selectedFragment = new ProfileFragment();

            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.container, selectedFragment)
                        .commit();
            }
            return true;
        });

        // Проверяем, нужно ли показать выбор режима
        boolean showModeSelect = getIntent().getBooleanExtra("show_mode_select", false);

        if (showModeSelect) {
            // Первый вход после регистрации — показываем выбор режима и скрываем нижнюю панель
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, new ModeSelectFragment())
                    .commit();

            bottomNav.setVisibility(View.GONE);   // ← Скрываем
        } else {
            // Обычный запуск приложения
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, new HomeFragment())
                    .commit();

            bottomNav.setSelectedItemId(R.id.nav_home);
            bottomNav.setVisibility(View.VISIBLE);
        }
    }

    // Метод, который будем вызывать из ModeSelectFragment
    public void showBottomNavigation() {
        if (bottomNav != null) {
            bottomNav.setVisibility(View.VISIBLE);
            bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }
}
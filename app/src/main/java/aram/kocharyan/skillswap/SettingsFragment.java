package aram.kocharyan.skillswap;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Вешаем клики на вьюхи цветов
        view.findViewById(R.id.color1).setOnClickListener(v -> saveColor("#2196F3"));
        view.findViewById(R.id.color2).setOnClickListener(v -> saveColor("#F44336"));
        view.findViewById(R.id.color3).setOnClickListener(v -> saveColor("#4CAF50"));
        view.findViewById(R.id.color4).setOnClickListener(v -> saveColor("#FF9800"));
        view.findViewById(R.id.color5).setOnClickListener(v -> saveColor("#9C27B0"));
        view.findViewById(R.id.color6).setOnClickListener(v -> saveColor("#009688"));
        view.findViewById(R.id.color7).setOnClickListener(v -> saveColor("#000000"));

        view.findViewById(R.id.btnCustomColor).setOnClickListener(v -> {
            // Используем простейший системный способ или заглушку для палитры
            // Если хочешь "прямоугольник", то нужно добавить либу в gradle,
            // но на сегодня можем сделать так:
            int randomColor = android.graphics.Color.rgb(
                    (int)(Math.random()*256),
                    (int)(Math.random()*256),
                    (int)(Math.random()*256)
            );
            saveColor(String.format("#%06X", (0xFFFFFF & randomColor)));
        });
    }

    private void saveColor(String hex) {
        SharedPreferences prefs = getActivity().getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE);
        prefs.edit().putString("selected_color", hex).apply();

        // Сразу применяем в MainActivity
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateNavColor();
        }
    }
}
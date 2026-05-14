package aram.kocharyan.skillswap;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {

    private View colorPreview;
    private TextView tvColorHex;
    private String currentColor = "#2196F3";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        colorPreview = view.findViewById(R.id.colorPreview);
        tvColorHex   = view.findViewById(R.id.tvColorHex);

        // Загружаем сохранённый цвет
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE);
        currentColor = prefs.getString("selected_color", "#2196F3");
        updatePreview(currentColor);

        // 7 предустановленных цветов
        view.findViewById(R.id.color1).setOnClickListener(v -> applyColor("#2196F3"));
        view.findViewById(R.id.color2).setOnClickListener(v -> applyColor("#F44336"));
        view.findViewById(R.id.color3).setOnClickListener(v -> applyColor("#4CAF50"));
        view.findViewById(R.id.color4).setOnClickListener(v -> applyColor("#FF9800"));
        view.findViewById(R.id.color5).setOnClickListener(v -> applyColor("#9C27B0"));
        view.findViewById(R.id.color6).setOnClickListener(v -> applyColor("#009688"));
        view.findViewById(R.id.color7).setOnClickListener(v -> applyColor("#111111"));

        // 8-й квадрат — открывает colorpicker
        view.findViewById(R.id.btnCustomColor).setOnClickListener(v -> showColorPickerDialog());
    }

    // ── Применить цвет ──────────────────────────────────────────────────────

    private void applyColor(String hex) {
        currentColor = hex;
        updatePreview(hex);
        saveColor(hex);
    }

    private void updatePreview(String hex) {
        try {
            colorPreview.setBackgroundColor(Color.parseColor(hex));
            tvColorHex.setText(hex.toUpperCase());
        } catch (Exception ignored) {}
    }

    private void saveColor(String hex) {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE);
        prefs.edit().putString("selected_color", hex).apply();

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateNavColor();
        }
    }

    // ── Colorpicker диалог ──────────────────────────────────────────────────

    private void showColorPickerDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_color_picker);
        dialog.getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        View previewBox     = dialog.findViewById(R.id.dialogColorPreview);
        TextView tvHex      = dialog.findViewById(R.id.dialogTvHex);
        SeekBar sbRed       = dialog.findViewById(R.id.sbRed);
        SeekBar sbGreen     = dialog.findViewById(R.id.sbGreen);
        SeekBar sbBlue      = dialog.findViewById(R.id.sbBlue);

        // Стартовые значения из текущего цвета
        try {
            int c = Color.parseColor(currentColor);
            sbRed.setProgress(Color.red(c));
            sbGreen.setProgress(Color.green(c));
            sbBlue.setProgress(Color.blue(c));
            previewBox.setBackgroundColor(c);
            tvHex.setText(currentColor.toUpperCase());
        } catch (Exception ignored) {}

        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int r = sbRed.getProgress();
                int g = sbGreen.getProgress();
                int b = sbBlue.getProgress();
                int color = Color.rgb(r, g, b);
                previewBox.setBackgroundColor(color);
                String hex = String.format("#%02X%02X%02X", r, g, b);
                tvHex.setText(hex);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        };

        sbRed.setOnSeekBarChangeListener(listener);
        sbGreen.setOnSeekBarChangeListener(listener);
        sbBlue.setOnSeekBarChangeListener(listener);

        dialog.findViewById(R.id.btnPickerApply).setOnClickListener(v -> {
            int r = sbRed.getProgress();
            int g = sbGreen.getProgress();
            int b = sbBlue.getProgress();
            String hex = String.format("#%02X%02X%02X", r, g, b);
            applyColor(hex);
            dialog.dismiss();
        });

        dialog.findViewById(R.id.btnPickerCancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
package aram.kocharyan.skillswap;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class SkillsSelectFragment extends Fragment {

    // Оригинальные Spinner — как было в старом коде
    private Spinner spTeach, spStudy;
    private Button btnLangRu, btnLangEn, btnLangHy, btnNext;
    private String selectedLanguage = null;

    private final String[] skills = {
            "Mathematics Grade 1", "Mathematics Grade 2", "Mathematics Grade 3",
            "Mathematics Grade 4", "Mathematics Grade 5", "Mathematics Grade 6",
            "Mathematics Grade 7", "Mathematics Grade 8", "Mathematics Grade 9",
            "Mathematics Grade 10", "Mathematics Grade 11", "Mathematics Grade 12",
            "English Grade 1", "English Grade 2", "English Grade 3",
            "English Grade 4", "English Grade 5", "English Grade 6",
            "English Grade 7", "English Grade 8", "English Grade 9",
            "English Grade 10", "English Grade 11", "English Grade 12",
            "Russian Grade 1", "Russian Grade 2", "Russian Grade 3",
            "Russian Grade 4", "Russian Grade 5", "Russian Grade 6",
            "Russian Grade 7", "Russian Grade 8", "Russian Grade 9",
            "Russian Grade 10", "Russian Grade 11", "Russian Grade 12",
            "Physics Grade 7", "Physics Grade 8", "Physics Grade 9",
            "Physics Grade 10", "Physics Grade 11", "Physics Grade 12",
            "Chemistry Grade 7", "Chemistry Grade 8", "Chemistry Grade 9",
            "Chemistry Grade 10", "Chemistry Grade 11", "Chemistry Grade 12",
            "Biology Grade 7", "Biology Grade 8", "Biology Grade 9",
            "Biology Grade 10", "Biology Grade 11", "Biology Grade 12",
            "History Grade 5", "History Grade 6", "History Grade 7",
            "History Grade 8", "History Grade 9", "History Grade 10",
            "History Grade 11", "History Grade 12",
            "Geography Grade 6", "Geography Grade 7", "Geography Grade 8",
            "Geography Grade 9", "Geography Grade 10", "Geography Grade 11",
            "Geography Grade 12",
            "Economics Grade 10", "Economics Grade 11", "Economics Grade 12"
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_skills_select, container, false);

        // Те же ID что были в оригинале
        spTeach   = view.findViewById(R.id.spTeach);
        spStudy   = view.findViewById(R.id.spStudy);
        btnLangRu = view.findViewById(R.id.btnLangRu);
        btnLangEn = view.findViewById(R.id.btnLangEn);
        btnLangHy = view.findViewById(R.id.btnLangHy);
        btnNext   = view.findViewById(R.id.btnNext);

        // Адаптер для спиннеров — как в оригинале
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, skills);
        spTeach.setAdapter(adapter);
        spStudy.setAdapter(adapter);

        // Кнопки языка
        btnLangRu.setOnClickListener(v -> selectLanguage("Russian"));
        btnLangEn.setOnClickListener(v -> selectLanguage("English"));
        btnLangHy.setOnClickListener(v -> selectLanguage("Armenian"));

        // Кнопка Next — логика как в оригинале + язык
        btnNext.setOnClickListener(v -> {
            String teach = spTeach.getSelectedItem().toString();
            String study = spStudy.getSelectedItem().toString();

            if (teach.equals(study)) {
                Toast.makeText(requireContext(),
                        "You cannot select the same skill for both.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedLanguage == null) {
                Toast.makeText(requireContext(),
                        "Please select language of instruction", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("Users").document(userId)
                    .update("skillTeach", teach,
                            "skillStudy",  study,
                            "language",    selectedLanguage)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(requireContext(), "Skills saved ✅", Toast.LENGTH_SHORT).show();
                        requireActivity().findViewById(R.id.bottomNavigationView)
                                .setVisibility(View.VISIBLE);
                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.container, new HomeFragment())
                                .commit();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(),
                                    "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        return view;
    }

    private void selectLanguage(String language) {
        selectedLanguage = language;
        resetLangBtn(btnLangRu);
        resetLangBtn(btnLangEn);
        resetLangBtn(btnLangHy);

        Button selected = null;
        switch (language) {
            case "Russian":  selected = btnLangRu; break;
            case "English":  selected = btnLangEn; break;
            case "Armenian": selected = btnLangHy; break;
        }
        if (selected != null) {
            selected.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#2563EB")));
            selected.setTextColor(android.graphics.Color.WHITE);
        }
    }

    private void resetLangBtn(Button btn) {
        if (btn == null) return;
        btn.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#F3F4F6")));
        btn.setTextColor(android.graphics.Color.parseColor("#374151"));
    }
}
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SettingsFragment extends Fragment {

    private Spinner spTeach, spStudy;
    private Button btnSave;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Надуваем layout фрагмента
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Привязываем элементы
        spTeach = view.findViewById(R.id.spTeach);
        spStudy = view.findViewById(R.id.spStudy);
        btnSave = view.findViewById(R.id.btnSave);

        // Список навыков (потом можно будет брать из Firebase или добавить больше)
        String[] skills = {"Programming", "Design", "English", "Math", "Music", "Guitar", "Cooking", "Photo"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                skills
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spTeach.setAdapter(adapter);
        spStudy.setAdapter(adapter);

        // Кнопка сохранения
        btnSave.setOnClickListener(v -> {
            String teach = spTeach.getSelectedItem().toString();
            String study = spStudy.getSelectedItem().toString();

            // Проверка авторизации
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                Toast.makeText(requireContext(), "Сначала войдите в аккаунт", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            DatabaseReference database = FirebaseDatabase.getInstance().getReference("Users");

            database.child(userId).child("skillTeach").setValue(teach);
            database.child(userId).child("skillStudy").setValue(study)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(requireContext(), "Навыки обновлены ✅", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(), "Ошибка сохранения", Toast.LENGTH_SHORT).show());
        });

        return view;
    }
}
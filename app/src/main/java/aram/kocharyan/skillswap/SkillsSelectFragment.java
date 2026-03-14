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

    private Spinner spTeach, spStudy;
    private Button btnNext;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_skills_select, container, false);

        spTeach = view.findViewById(R.id.spTeach);
        spStudy = view.findViewById(R.id.spStudy);
        btnNext = view.findViewById(R.id.btnNext);

        String[] skills = {"Programming", "Design", "English", "Math", "Music", "Guitar", "Cooking", "Physics"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, skills);

        spTeach.setAdapter(adapter);
        spStudy.setAdapter(adapter);

        btnNext.setOnClickListener(v -> {
            String teach = spTeach.getSelectedItem().toString();
            String study = spStudy.getSelectedItem().toString();

            if (teach.equals(study)) {
                Toast.makeText(requireContext(), "You cannot select the same skill for both.", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("Users").document(userId)
                    .update("skillTeach", teach, "skillStudy", study)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(requireContext(), "Skills saved ✅", Toast.LENGTH_SHORT).show();
                        requireActivity().findViewById(R.id.bottomNavigationView).setVisibility(View.VISIBLE);
                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.container, new HomeFragment())
                                .commit();
                    })
                    .addOnFailureListener(e -> Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        return view;
    }
}
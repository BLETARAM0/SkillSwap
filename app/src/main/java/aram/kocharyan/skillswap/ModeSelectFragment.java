package aram.kocharyan.skillswap;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ModeSelectFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_mode_select, container, false);

        Button btnOnline = view.findViewById(R.id.btnOnline);
        Button btnOffline = view.findViewById(R.id.btnOffline);

        btnOnline.setOnClickListener(v -> {
            // Save mode = online
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference db = FirebaseDatabase.getInstance().getReference("Users").child(userId);
            db.child("mode").setValue("online")
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            db.child("country").setValue("");
                            db.child("city").setValue("").addOnCompleteListener(task2 -> {
                                if (task2.isSuccessful()) {
                                    Toast.makeText(requireContext(), "Online mode saved", Toast.LENGTH_SHORT).show();
                                    // Go to SkillsSelectFragment
                                    requireActivity().getSupportFragmentManager()
                                            .beginTransaction()
                                            .replace(R.id.container, new SkillsSelectFragment())
                                            .commit();
                                } else {
                                    Toast.makeText(requireContext(), "Error clearing location: " + task2.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                        } else {
                            Toast.makeText(requireContext(), "Error saving online mode: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        btnOffline.setOnClickListener(v -> {
            // Save mode = offline
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference db = FirebaseDatabase.getInstance().getReference("Users").child(userId);
            db.child("mode").setValue("offline")
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(requireContext(), "Offline mode selected", Toast.LENGTH_SHORT).show();
                            // Go to OfflineFragment
                            requireActivity().getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.container, new OfflineFragment())
                                    .commit();
                        } else {
                            Toast.makeText(requireContext(), "Error saving offline mode: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        return view;
    }
}
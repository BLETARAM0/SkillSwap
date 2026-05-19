package aram.kocharyan.skillswap;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ModeSelectFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_mode_select, container, false);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // В новом XML btnOnline и btnOffline — это CardView, не Button
        // setOnClickListener работает на любом View
        View btnOnline  = view.findViewById(R.id.btnOnline);
        View btnOffline = view.findViewById(R.id.btnOffline);

        btnOnline.setOnClickListener(v -> {
            db.collection("Users").document(userId)
                    .update("mode", "online", "country", "", "city", "")
                    .addOnSuccessListener(aVoid -> {
                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.container, new SkillsSelectFragment())
                                .commit();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(),
                                    "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });

        btnOffline.setOnClickListener(v -> {
            db.collection("Users").document(userId)
                    .update("mode", "offline")
                    .addOnSuccessListener(aVoid -> {
                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.container, new OfflineFragment())
                                .commit();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(),
                                    "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });

        return view;
    }
}
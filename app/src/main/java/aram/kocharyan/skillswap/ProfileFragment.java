package aram.kocharyan.skillswap;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    TextView tvName, tvEmail, tvTeach, tvStudy;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        tvName = view.findViewById(R.id.tvName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvTeach = view.findViewById(R.id.tvTeach);
        tvStudy = view.findViewById(R.id.tvStudy);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String surname = documentSnapshot.getString("surname");
                        String email = documentSnapshot.getString("email");
                        String teach = documentSnapshot.getString("skillTeach");
                        String study = documentSnapshot.getString("skillStudy");

                        tvName.setText(name + " " + surname);
                        tvEmail.setText(email);
                        tvTeach.setText("Teaches: " + teach);
                        tvStudy.setText("Wants to learn: " + study);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Error loading profile", Toast.LENGTH_SHORT).show());

        return view;
    }
}
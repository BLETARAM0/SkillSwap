package aram.kocharyan.skillswap;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ProfileFragment extends Fragment {

    TextView tvName, tvEmail, tvTeach, tvStudy;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Bind views
        tvName = view.findViewById(R.id.tvName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvTeach = view.findViewById(R.id.tvTeach);
        tvStudy = view.findViewById(R.id.tvStudy);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        DatabaseReference database = FirebaseDatabase.getInstance()
                .getReference("Users");

        if (auth.getCurrentUser() != null) {

            String userId = auth.getCurrentUser().getUid();

            database.child(userId).get().addOnSuccessListener(snapshot -> {

                String name = snapshot.child("name").getValue(String.class);
                String surname = snapshot.child("surname").getValue(String.class);
                String email = snapshot.child("email").getValue(String.class);
                String teach = snapshot.child("skillTeach").getValue(String.class);
                String study = snapshot.child("skillStudy").getValue(String.class);

                tvName.setText(name + " " + surname);
                tvEmail.setText(email);
                tvTeach.setText("Teaches: " + teach);
                tvStudy.setText("Wants to learn: " + study);
            });
        }

        return view;
    }
}
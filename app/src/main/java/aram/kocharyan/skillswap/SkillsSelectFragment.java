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

public class SkillsSelectFragment extends Fragment {

    private Spinner spinnerTeach, spinnerLearn;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_skills_select, container, false);

        spinnerTeach = view.findViewById(R.id.spinnerTeach);
        spinnerLearn = view.findViewById(R.id.spinnerLearn);
        Button btnNext = view.findViewById(R.id.btnNext);

        // Sample skills
        String[] skills = {"Math", "English", "Physics", "Programming"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                skills
        );

        spinnerTeach.setAdapter(adapter);
        spinnerLearn.setAdapter(adapter);

        btnNext.setOnClickListener(v -> {

            // show bottom nav
            requireActivity()
                    .findViewById(R.id.bottomNavigationView)
                    .setVisibility(View.VISIBLE);

            // open Home
            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, new HomeFragment())
                    .commit();
        });

        return view;
    }
}

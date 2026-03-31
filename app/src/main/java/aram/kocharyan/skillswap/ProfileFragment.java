package aram.kocharyan.skillswap;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvEmail;
    private Spinner spTeach, spStudy, spinnerCountry, spinnerCity;
    private RadioGroup radioGroupMode;
    private RadioButton radioOnline, radioOffline;
    private LinearLayout layoutLocation;
    private Button btnSave;
    private JSONObject countriesObject;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        tvName = view.findViewById(R.id.tvName);
        tvEmail = view.findViewById(R.id.tvEmail);
        spTeach = view.findViewById(R.id.spTeach);
        spStudy = view.findViewById(R.id.spStudy);
        spinnerCountry = view.findViewById(R.id.spinnerCountry);
        spinnerCity = view.findViewById(R.id.spinnerCity);
        radioGroupMode = view.findViewById(R.id.radioGroupMode);
        radioOnline = view.findViewById(R.id.radioOnline);
        radioOffline = view.findViewById(R.id.radioOffline);
        layoutLocation = view.findViewById(R.id.layoutLocation);
        btnSave = view.findViewById(R.id.btnSave);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Skills
        String[] skills = {"Mathematics Grade 1", "Mathematics Grade 2", "Mathematics Grade 3", "Mathematics Grade 4", "Mathematics Grade 5", "Mathematics Grade 6", "Mathematics Grade 7", "Mathematics Grade 8", "Mathematics Grade 9", "Mathematics Grade 10", "Mathematics Grade 11", "Mathematics Grade 12", "English Grade 1", "English Grade 2", "English Grade 3", "English Grade 4", "English Grade 5", "English Grade 6", "English Grade 7", "English Grade 8", "English Grade 9", "English Grade 10", "English Grade 11", "English Grade 12", "Russian Grade 1", "Russian Grade 2", "Russian Grade 3", "Russian Grade 4", "Russian Grade 5", "Russian Grade 6", "Russian Grade 7", "Russian Grade 8", "Russian Grade 9", "Russian Grade 10", "Russian Grade 11", "Russian Grade 12", "Physics Grade 7", "Physics Grade 8", "Physics Grade 9", "Physics Grade 10", "Physics Grade 11", "Physics Grade 12", "Chemistry Grade 7", "Chemistry Grade 8", "Chemistry Grade 9", "Chemistry Grade 10", "Chemistry Grade 11", "Chemistry Grade 12", "Biology Grade 7", "Biology Grade 8", "Biology Grade 9", "Biology Grade 10", "Biology Grade 11", "Biology Grade 12", "History Grade 5", "History Grade 6", "History Grade 7", "History Grade 8", "History Grade 9", "History Grade 10", "History Grade 11", "History Grade 12", "Geography Grade 6", "Geography Grade 7", "Geography Grade 8", "Geography Grade 9", "Geography Grade 10", "Geography Grade 11", "Geography Grade 12", "Economics Grade 10", "Economics Grade 11", "Economics Grade 12"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, skills);
        spTeach.setAdapter(adapter);
        spStudy.setAdapter(adapter);

        loadCountries();

        spinnerCountry.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                loadCities(parent.getItemAtPosition(pos).toString());
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        radioGroupMode.setOnCheckedChangeListener((group, checkedId) ->
                layoutLocation.setVisibility(checkedId == R.id.radioOffline ? View.VISIBLE : View.GONE));

        // Load user
        db.collection("Users").document(userId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {

                        String name = document.getString("name");
                        String surname = document.getString("surname");
                        String email = document.getString("email");
                        String teach = document.getString("skillTeach");
                        String study = document.getString("skillStudy");
                        String mode = document.getString("mode");
                        String country = document.getString("country");
                        String city = document.getString("city");

                        tvName.setText(name + " " + surname);
                        tvEmail.setText(email);

                        if (teach != null) spTeach.setSelection(adapter.getPosition(teach));
                        if (study != null) spStudy.setSelection(adapter.getPosition(study));

                        if ("offline".equals(mode)) {
                            radioOffline.setChecked(true);
                            layoutLocation.setVisibility(View.VISIBLE);
                        } else {
                            radioOnline.setChecked(true);
                        }
                    }
                });

        btnSave.setOnClickListener(v -> {

            String teach = spTeach.getSelectedItem().toString();
            String study = spStudy.getSelectedItem().toString();
            String mode = radioOnline.isChecked() ? "online" : "offline";
            String country = mode.equals("offline") ? spinnerCountry.getSelectedItem().toString() : "";
            String city = mode.equals("offline") ? spinnerCity.getSelectedItem().toString() : "";

            if (teach.equals(study)) {
                Toast.makeText(requireContext(), "Skills should not match", Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("Users").document(userId)
                    .update("skillTeach", teach,
                            "skillStudy", study,
                            "mode", mode,
                            "country", country,
                            "city", city)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(requireContext(), "Profile updated ✅", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });

        return view;
    }

    private void loadCountries() {
        try {
            InputStream is = requireContext().getAssets().open("countries.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            countriesObject = new JSONObject(new String(buffer, "UTF-8"));

            Iterator<String> keys = countriesObject.keys();
            ArrayList<String> countryList = new ArrayList<>();

            while (keys.hasNext()) countryList.add(keys.next());

            spinnerCountry.setAdapter(new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_dropdown_item, countryList));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadCities(String country) {
        try {
            JSONArray citiesArray = countriesObject.getJSONArray(country);
            ArrayList<String> cityList = new ArrayList<>();

            for (int i = 0; i < citiesArray.length(); i++)
                cityList.add(citiesArray.getString(i));

            spinnerCity.setAdapter(new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_dropdown_item, cityList));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
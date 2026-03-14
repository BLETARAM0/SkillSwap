package aram.kocharyan.skillswap;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

public class SettingsFragment extends Fragment {

    private Spinner spTeach, spStudy, spinnerCountry, spinnerCity;
    private RadioGroup radioGroupMode;
    private RadioButton radioOnline, radioOffline;
    private LinearLayout layoutLocation;
    private Button btnSave;
    private JSONObject countriesObject;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        spTeach = view.findViewById(R.id.spTeach);
        spStudy = view.findViewById(R.id.spStudy);
        radioGroupMode = view.findViewById(R.id.radioGroupMode);
        radioOnline = view.findViewById(R.id.radioOnline);
        radioOffline = view.findViewById(R.id.radioOffline);
        layoutLocation = view.findViewById(R.id.layoutLocation);
        spinnerCountry = view.findViewById(R.id.spinnerCountry);
        spinnerCity = view.findViewById(R.id.spinnerCity);
        btnSave = view.findViewById(R.id.btnSave);

        // Skills
        String[] skills = {"Programming", "Design", "English", "Math", "Music", "Guitar", "Cooking", "Photo"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, skills);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTeach.setAdapter(adapter);
        spStudy.setAdapter(adapter);

        loadCountries();

        spinnerCountry.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View v, int pos, long id) {
                loadCities(parent.getItemAtPosition(pos).toString());
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        radioGroupMode.setOnCheckedChangeListener((group, checkedId) ->
                layoutLocation.setVisibility(checkedId == R.id.radioOffline ? View.VISIBLE : View.GONE));

        // Load current user data
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String mode = document.getString("mode");
                        String teach = document.getString("skillTeach");
                        String study = document.getString("skillStudy");
                        String country = document.getString("country");
                        String city = document.getString("city");

                        if (teach != null) spTeach.setSelection(adapter.getPosition(teach));
                        if (study != null) spStudy.setSelection(adapter.getPosition(study));

                        if ("offline".equals(mode)) {
                            radioOffline.setChecked(true);
                            layoutLocation.setVisibility(View.VISIBLE);
                            if (country != null) {
                                int pos = ((ArrayAdapter<String>) spinnerCountry.getAdapter()).getPosition(country);
                                if (pos >= 0) spinnerCountry.setSelection(pos);
                                loadCities(country);
                                if (city != null) {
                                    int cityPos = ((ArrayAdapter<String>) spinnerCity.getAdapter()).getPosition(city);
                                    if (cityPos >= 0) spinnerCity.setSelection(cityPos);
                                }
                            }
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
            if (mode.equals("offline") && (country.isEmpty() || city.isEmpty())) {
                Toast.makeText(requireContext(), "Please select country and city", Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("Users").document(userId)
                    .update("skillTeach", teach,
                            "skillStudy", study,
                            "mode", mode,
                            "country", country,
                            "city", city)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(requireContext(), "Settings updated ✅", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });

        return view;
    }

    // ================== JSON methods (точно такие же) ==================
    private void loadCountries() {
        try {
            String jsonString = loadJSONFromAsset();
            countriesObject = new JSONObject(jsonString);

            Iterator<String> keys = countriesObject.keys();
            ArrayList<String> countryList = new ArrayList<>();

            while (keys.hasNext()) {
                countryList.add(keys.next());
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    countryList
            );

            spinnerCountry.setAdapter(adapter);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Error loading countries", Toast.LENGTH_LONG).show();
        }
    }

    private void loadCities(String country) {
        try {
            JSONArray citiesArray = countriesObject.getJSONArray(country);
            ArrayList<String> cityList = new ArrayList<>();

            for (int i = 0; i < citiesArray.length(); i++) {
                cityList.add(citiesArray.getString(i));
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    cityList
            );

            spinnerCity.setAdapter(adapter);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Error loading cities", Toast.LENGTH_LONG).show();
        }
    }

    private String loadJSONFromAsset() {
        try {
            InputStream is = requireContext().getAssets().open("countries.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            return new String(buffer, "UTF-8");
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
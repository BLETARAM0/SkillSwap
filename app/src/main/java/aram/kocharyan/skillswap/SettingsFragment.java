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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

public class SettingsFragment extends Fragment {

    private Spinner spTeach, spStudy, spinnerCountry, spinnerCity;
    private RadioGroup radioGroupMode;
    private RadioButton radioOnline, radioOffline;
    private LinearLayout layoutLocation; // For country/city visibility
    private Button btnSave;
    private JSONObject countriesObject;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Bind elements
        spTeach = view.findViewById(R.id.spTeach);
        spStudy = view.findViewById(R.id.spStudy);
        radioGroupMode = view.findViewById(R.id.radioGroupMode);
        radioOnline = view.findViewById(R.id.radioOnline);
        radioOffline = view.findViewById(R.id.radioOffline);
        layoutLocation = view.findViewById(R.id.layoutLocation);
        spinnerCountry = view.findViewById(R.id.spinnerCountry);
        spinnerCity = view.findViewById(R.id.spinnerCity);
        btnSave = view.findViewById(R.id.btnSave);

        // Skills spinner
        String[] skills = {"Programming", "Design", "English", "Math", "Music", "Guitar", "Cooking", "Photo"};

        ArrayAdapter<String> skillsAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                skills
        );
        skillsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spTeach.setAdapter(skillsAdapter);
        spStudy.setAdapter(skillsAdapter);

        // Load countries for location
        loadCountries();

        // Country → city dependent
        spinnerCountry.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view1, int position, long id) {
                String selectedCountry = parent.getItemAtPosition(position).toString();
                loadCities(selectedCountry);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Mode change listener to show/hide location
        radioGroupMode.setOnCheckedChangeListener((group, checkedId) -> {
            layoutLocation.setVisibility(checkedId == R.id.radioOffline ? View.VISIBLE : View.GONE);
        });

        // Load current data from Firebase
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference db = FirebaseDatabase.getInstance().getReference("Users").child(userId);

        db.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                AppUser user = snapshot.getValue(AppUser.class);
                if (user != null) {
                    // Set skills
                    int teachPos = skillsAdapter.getPosition(user.skillTeach);
                    int studyPos = skillsAdapter.getPosition(user.skillStudy);
                    if (teachPos >= 0) spTeach.setSelection(teachPos);
                    if (studyPos >= 0) spStudy.setSelection(studyPos);

                    // Set mode
                    if ("offline".equals(user.mode)) {
                        radioOffline.setChecked(true);
                        layoutLocation.setVisibility(View.VISIBLE);
                        // Set country and city
                        int countryPos = ((ArrayAdapter<String>) spinnerCountry.getAdapter()).getPosition(user.country);
                        if (countryPos >= 0) {
                            spinnerCountry.setSelection(countryPos);
                            // Load cities and set city
                            loadCities(user.country);
                            int cityPos = ((ArrayAdapter<String>) spinnerCity.getAdapter()).getPosition(user.city);
                            if (cityPos >= 0) spinnerCity.setSelection(cityPos);
                        }
                    } else {
                        radioOnline.setChecked(true);
                        layoutLocation.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(requireContext(), "Error loading settings", Toast.LENGTH_SHORT).show();
            }
        });

        // Save button
        btnSave.setOnClickListener(v -> {
            String teach = spTeach.getSelectedItem().toString();
            String study = spStudy.getSelectedItem().toString();
            String mode = radioOnline.isChecked() ? "online" : "offline";
            String country = mode.equals("offline") ? (spinnerCountry.getSelectedItem() != null ? spinnerCountry.getSelectedItem().toString() : "") : "";
            String city = mode.equals("offline") ? (spinnerCity.getSelectedItem() != null ? spinnerCity.getSelectedItem().toString() : "") : "";

            if (teach.equals(study)) {
                Toast.makeText(requireContext(), "Skills should not match", Toast.LENGTH_SHORT).show();
                return;
            }
            if (mode.equals("offline") && (country.isEmpty() || city.isEmpty())) {
                Toast.makeText(requireContext(), "Please select country and city for offline mode", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check authorization
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                Toast.makeText(requireContext(), "Log in first", Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseReference database = FirebaseDatabase.getInstance().getReference("Users").child(userId);

            database.child("skillTeach").setValue(teach);
            database.child("skillStudy").setValue(study);
            database.child("mode").setValue(mode);
            database.child("country").setValue(country);
            database.child("city").setValue(city)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(requireContext(), "Settings updated ✅", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(), "Error saving settings", Toast.LENGTH_SHORT).show());
        });

        return view;
    }

    // -------- Load countries -------- (copied from OfflineFragment)

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

    // -------- Load cities --------

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

    // -------- Read JSON from assets --------

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
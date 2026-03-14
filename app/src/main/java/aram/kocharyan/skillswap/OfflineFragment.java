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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

public class OfflineFragment extends Fragment {

    private Spinner spinnerCountry, spinnerCity;
    private JSONObject countriesObject;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_offline, container, false);

        spinnerCountry = view.findViewById(R.id.spinnerCountry);
        spinnerCity = view.findViewById(R.id.spinnerCity);
        Button btnNext = view.findViewById(R.id.btnNext);

        loadCountries();

        spinnerCountry.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view1, int position, long id) {
                loadCities(parent.getItemAtPosition(position).toString());
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        btnNext.setOnClickListener(v -> {
            String selectedCountry = spinnerCountry.getSelectedItem() != null ?
                    spinnerCountry.getSelectedItem().toString() : "";
            String selectedCity = spinnerCity.getSelectedItem() != null ?
                    spinnerCity.getSelectedItem().toString() : "";

            if (selectedCountry.isEmpty() || selectedCity.isEmpty()) {
                Toast.makeText(requireContext(), "Please select country and city", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("Users").document(userId)
                    .update("country", selectedCountry, "city", selectedCity)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(requireContext(), "Location saved ✅", Toast.LENGTH_SHORT).show();
                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.container, new SkillsSelectFragment())
                                .commit();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });

        return view;
    }

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
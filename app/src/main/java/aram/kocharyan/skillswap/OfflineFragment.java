package aram.kocharyan.skillswap;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

public class OfflineFragment extends Fragment {

    Spinner spinnerCountry, spinnerCity;

    JSONObject countriesObject; // JSON պահում ենք այստեղ

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_offline, container, false);

        spinnerCountry = view.findViewById(R.id.spinnerCountry);
        spinnerCity = view.findViewById(R.id.spinnerCity);

        loadCountries();

        spinnerCountry.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedCountry = parent.getItemAtPosition(position).toString();
                loadCities(selectedCountry);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        return view;
    }


    // =============================
    // LOAD COUNTRIES FROM JSON
    // =============================
    private void loadCountries() {

        try {
            String jsonString = loadJSONFromAsset();

            if (jsonString == null) {
                Toast.makeText(getContext(), "JSON NULL", Toast.LENGTH_LONG).show();
                return;
            }

            countriesObject = new JSONObject(jsonString);

            Iterator<String> keys = countriesObject.keys();

            ArrayList<String> countryList = new ArrayList<>();

            while (keys.hasNext()) {
                countryList.add(keys.next());
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    getContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    countryList
            );

            spinnerCountry.setAdapter(adapter);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "COUNTRY ERROR", Toast.LENGTH_LONG).show();
        }
    }


    // =============================
    // LOAD CITIES DEPENDING COUNTRY
    // =============================
    private void loadCities(String country) {

        try {
            JSONArray citiesArray = countriesObject.getJSONArray(country);

            ArrayList<String> cityList = new ArrayList<>();

            for (int i = 0; i < citiesArray.length(); i++) {
                cityList.add(citiesArray.getString(i));
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    getContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    cityList
            );

            spinnerCity.setAdapter(adapter);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "CITY ERROR", Toast.LENGTH_LONG).show();
        }
    }


    // =============================
    // READ JSON FROM ASSETS
    // =============================
    private String loadJSONFromAsset() {

        String json = null;

        try {
            InputStream is = requireContext().getAssets().open("countries.json");

            int size = is.available();

            byte[] buffer = new byte[size];

            is.read(buffer);

            is.close();

            json = new String(buffer, "UTF-8");

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return json;
    }
}
package aram.kocharyan.skillswap;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.HashMap;
import java.util.Map;

public class OfflineFragment extends Fragment {

    private Spinner spinnerCountry, spinnerCity;
    private Map<String, String[]> countryCityMap = new HashMap<>();
    private Button btnNext;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_offline, container, false);

        spinnerCountry = view.findViewById(R.id.spinnerCountry);
        spinnerCity = view.findViewById(R.id.spinnerCity);
        btnNext = view.findViewById(R.id.btnNext);

        // Countries & cities
        countryCityMap.put("Armenia", new String[]{"Yerevan", "Gyumri", "Vanadzor"});
        countryCityMap.put("Germany", new String[]{"Berlin", "Munich", "Hamburg"});
        countryCityMap.put("France", new String[]{"Paris", "Lyon", "Marseille"});
        countryCityMap.put("USA", new String[]{"New York", "Los Angeles", "Chicago"});

        String[] countries = countryCityMap.keySet().toArray(new String[0]);
        ArrayAdapter<String> countryAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, countries);
        spinnerCountry.setAdapter(countryAdapter);

        // Set initial cities
        String firstCountry = countries[0];
        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                countryCityMap.get(firstCountry));
        spinnerCity.setAdapter(cityAdapter);

        spinnerCountry.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedCountry = spinnerCountry.getSelectedItem().toString();
                ArrayAdapter<String> newCityAdapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        countryCityMap.get(selectedCountry));
                spinnerCity.setAdapter(newCityAdapter);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Next button click → go to HomeFragment & show bottom nav
        btnNext.setOnClickListener(v -> {
            // Show bottom nav
            BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottomNavigationView);
            bottomNav.setVisibility(View.VISIBLE);

            // Replace fragment with HomeFragment
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, new HomeFragment())
                    .commit();
        });

        return view;
    }
}

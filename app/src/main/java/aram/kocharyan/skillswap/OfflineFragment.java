package aram.kocharyan.skillswap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import androidx.fragment.app.Fragment;


public class OfflineFragment extends Fragment {

    Spinner spinnerCountry, spinnerCity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_offline, container, false);

        spinnerCountry = view.findViewById(R.id.spinnerCountry);
        spinnerCity = view.findViewById(R.id.spinnerCity);

        String[] countries = {"Armenia", "Germany", "France"};
        String[] citiesArmenia = {"Yerevan", "Gyumri", "Vanadzor"};

        ArrayAdapter<String> countryAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                countries
        );

        spinnerCountry.setAdapter(countryAdapter);

        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                citiesArmenia
        );

        spinnerCity.setAdapter(cityAdapter);

        return view;
    }
}
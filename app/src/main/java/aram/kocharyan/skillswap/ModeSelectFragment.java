package aram.kocharyan.skillswap;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

public class ModeSelectFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_mode_select, container, false);

        Button btnOnline = view.findViewById(R.id.btnOnline);
        Button btnOffline = view.findViewById(R.id.btnOffline);

        btnOnline.setOnClickListener(v -> {
            // Online ընտրեց → գնալ HomeFragment
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, new HomeFragment())
                    .commit();
        });

        btnOffline.setOnClickListener(v -> {
            // Offline ընտրեց → գնալ OfflineFragment
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, new OfflineFragment())
                    .commit();
        });

        return view;
    }
}

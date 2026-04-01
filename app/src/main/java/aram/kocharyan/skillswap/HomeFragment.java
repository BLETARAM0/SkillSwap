package aram.kocharyan.skillswap;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements HomeMatchAdapter.OnMatchClickListener {

    private RecyclerView recyclerView;
    private HomeMatchAdapter adapter;
    private List<AppUser> matchList = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        adapter = new HomeMatchAdapter(matchList, this);
        recyclerView.setAdapter(adapter);

        loadMatches();

        return view;
    }

    private void loadMatches() {
        db.collection("Users").document(currentUserId).get()
                .addOnSuccessListener(currentDoc -> {
                    if (!currentDoc.exists()) return;

                    AppUser currentUser = currentDoc.toObject(AppUser.class);
                    if (currentUser == null) return;


                    db.collection("Users")
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                matchList.clear();

                                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                    AppUser user = doc.toObject(AppUser.class);
                                    if (user == null || doc.getId().equals(currentUserId)) continue;

                                    if (isMatch(currentUser, user)) {
                                        matchList.add(user);
                                    }
                                }

                                adapter.notifyDataSetChanged();

                                if (matchList.isEmpty()) {
                                    Toast.makeText(requireContext(), "No matches found yet", Toast.LENGTH_SHORT).show();
                                }
                            });
                });
    }

    private boolean isMatch(AppUser current, AppUser other) {
        if (current.mode == null || other.mode == null) return false;


        boolean skillsMatch = current.skillTeach.equals(other.skillStudy) &&
                current.skillStudy.equals(other.skillTeach);

        if (!skillsMatch) return false;


        if ("online".equals(current.mode) && "online".equals(other.mode)) {
            return true;
        }

        if ("offline".equals(current.mode) && "offline".equals(other.mode)) {
            return current.country != null && current.country.equals(other.country) &&
                    current.city != null && current.city.equals(other.city);
        }

        return false;
    }

    @Override
    public void onMatchClick(AppUser user) {
        Toast.makeText(requireContext(), "Clicked on: " + user.name, Toast.LENGTH_SHORT).show();
    }
}
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                            .addOnSuccessListener(query -> {
                                matchList.clear();

                                for (QueryDocumentSnapshot doc : query) {
                                    if (doc.getId().equals(currentUserId)) continue;

                                    AppUser user = doc.toObject(AppUser.class);
                                    if (user == null) continue;

                                    if (isMatch(currentUser, user)) {
                                        matchList.add(user);
                                    }
                                }
                                adapter.notifyDataSetChanged();
                            });
                });
    }

    private boolean isMatch(AppUser current, AppUser other) {
        if (current.mode == null || other.mode == null) return false;

        boolean skillsMatch = current.skillTeach.equals(other.skillStudy) &&
                current.skillStudy.equals(other.skillTeach);

        if (!skillsMatch) return false;

        if ("online".equals(current.mode) && "online".equals(other.mode)) return true;

        if ("offline".equals(current.mode) && "offline".equals(other.mode)) {
            return current.country != null && current.country.equals(other.country) &&
                    current.city != null && current.city.equals(other.city);
        }

        return false;
    }

    @Override
    public void onMatchClick(AppUser user) {
        sendChatRequest(user);
    }

    private void sendChatRequest(AppUser targetUser) {
        // requestId
        String requestId = currentUserId + "_" + targetUser.userId;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Users").document(currentUserId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) return;
                    AppUser currentUser = document.toObject(AppUser.class);
                    if (currentUser == null) return;

                    Map<String, Object> request = new HashMap<>();
                    request.put("fromUserId", currentUserId);
                    request.put("fromName", currentUser.name);       // Имя
                    request.put("fromSurname", currentUser.surname); // Фамилия
                    request.put("fromEmail", currentUser.email);     // Gmail/Email
                    request.put("toUserId", targetUser.userId);      // Кому отправляем

                    db.collection("ChatRequests")
                            .document(requestId)
                            .set(request)
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(requireContext(), "Request sent to " + targetUser.name, Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(requireContext(), "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                });
    }
}
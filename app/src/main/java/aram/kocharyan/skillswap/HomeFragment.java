package aram.kocharyan.skillswap;

import android.os.Bundle;
import android.util.Log;
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

    private static final String TAG = "SKILLSWAP_DEBUG";

    private RecyclerView recyclerView;
    private HomeMatchAdapter adapter;
    private final List<AppUser> matchList = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentUserId;
    private AppUser currentUser;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        db            = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        adapter = new HomeMatchAdapter(matchList, this);
        recyclerView.setAdapter(adapter);

        loadMatches();

        return view;
    }

    private void loadMatches() {
        db.collection("Users").document(currentUserId).get()
                .addOnSuccessListener(currentDoc -> {
                    if (!currentDoc.exists()) {
                        Log.d(TAG, "Current user not found");
                        return;
                    }

                    currentUser = currentDoc.toObject(AppUser.class);
                    if (currentUser == null) return;
                    currentUser.userId = currentDoc.getId();

                    // Если mode не установлен — считаем online
                    if (currentUser.mode == null) currentUser.mode = "online";

                    Log.d(TAG, "Me: " + currentUser.name
                            + " teach=" + currentUser.skillTeach
                            + " study=" + currentUser.skillStudy
                            + " lang=" + currentUser.language
                            + " mode=" + currentUser.mode
                            + " country=" + currentUser.country
                            + " city=" + currentUser.city);

                    db.collection("Users").get()
                            .addOnSuccessListener(snapshot -> {
                                matchList.clear();
                                for (QueryDocumentSnapshot doc : snapshot) {
                                    if (doc.getId().equals(currentUserId)) continue;
                                    AppUser other = doc.toObject(AppUser.class);
                                    if (other == null) continue;
                                    other.userId = doc.getId();

                                    // Если mode не установлен — считаем online
                                    if (other.mode == null) other.mode = "online";

                                    boolean match = isMatch(currentUser, other);
                                    Log.d(TAG, "vs " + other.name
                                            + " teach=" + other.skillTeach
                                            + " study=" + other.skillStudy
                                            + " lang=" + other.language
                                            + " mode=" + other.mode
                                            + " country=" + other.country
                                            + " city=" + other.city
                                            + " → " + match);

                                    if (match) matchList.add(other);
                                }
                                adapter.notifyDataSetChanged();
                                Log.d(TAG, "Matches: " + matchList.size());

                                if (matchList.isEmpty() && isAdded()) {
                                    Toast.makeText(requireContext(),
                                            "No matches found yet", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "Error loading users", e));
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading current user", e));
    }

    private boolean isMatch(AppUser current, AppUser other) {
        // Скиллы обязательны
        if (current.skillTeach == null || current.skillStudy == null) return false;
        if (other.skillTeach   == null || other.skillStudy   == null) return false;

        // Взаимное совпадение скиллов
        boolean skillsMatch = current.skillTeach.equals(other.skillStudy)
                && current.skillStudy.equals(other.skillTeach);
        if (!skillsMatch) return false;

        // Язык: если у обоих указан — должен совпадать
        if (current.language != null && !current.language.isEmpty()
                && other.language != null && !other.language.isEmpty()) {
            if (!current.language.equals(other.language)) return false;
        }

        // Режим online-online
        if ("online".equals(current.mode) && "online".equals(other.mode)) {
            return true;
        }

        // Режим offline-offline + страна + город
        if ("offline".equals(current.mode) && "offline".equals(other.mode)) {
            if (current.country == null || current.city == null) return false;
            return current.country.equals(other.country)
                    && current.city.equals(other.city);
        }

        // online + offline не матчатся
        return false;
    }

    @Override
    public void onMatchClick(AppUser targetUser) {
        if (targetUser.userId == null) {
            Toast.makeText(requireContext(), "Error: User ID is null", Toast.LENGTH_SHORT).show();
            return;
        }
        sendChatRequest(targetUser);
    }

    private void sendChatRequest(AppUser targetUser) {
        String requestId = currentUserId.compareTo(targetUser.userId) < 0
                ? currentUserId + "_" + targetUser.userId
                : targetUser.userId + "_" + currentUserId;

        Map<String, Object> requestData = new HashMap<>();
        requestData.put("requestId",  requestId);
        requestData.put("fromUserId", currentUserId);
        requestData.put("fromName",   currentUser != null && currentUser.name != null
                ? currentUser.name : "Someone");
        requestData.put("toUserId",   targetUser.userId);
        requestData.put("status",     "pending");
        requestData.put("timestamp",  System.currentTimeMillis());

        db.collection("ChatRequests").document(requestId)
                .set(requestData)
                .addOnSuccessListener(v -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Request sent to " + targetUser.name, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Failed to send request", Toast.LENGTH_SHORT).show();
                });
    }
}
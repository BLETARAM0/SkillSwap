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
import com.google.firebase.firestore.Filter;
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
    private AppUser currentUser;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        adapter = new HomeMatchAdapter(matchList, this);
        recyclerView.setAdapter(adapter);

        checkChatStatusAndLoad();

        return view;
    }

    /*private void checkChatStatusAndLoad() {
        db.collection("Chats")
                .where(Filter.or(
                        Filter.equalTo("user1", currentUserId),
                        Filter.equalTo("user2", currentUserId)
                ))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        matchList.clear();
                        adapter.notifyDataSetChanged();
                    } else {
                        loadMatches();
                    }
                });
    }*/

    private void checkChatStatusAndLoad() {
        // ВРЕМЕННО отключаем сложную фильтрацию, чтобы проверить, появятся ли матчи
        Log.d("SKILLSWAP_DEBUG", "Starting loadMatches without chat filter...");
        loadMatches();
    }

    private void loadMatches() {
        db.collection("Users").document(currentUserId).get()
                .addOnSuccessListener(currentDoc -> {
                    if (!currentDoc.exists()) {
                        Log.d("SKILLSWAP_DEBUG", "Current user document not found in Firestore!");
                        return;
                    }

                    currentUser = currentDoc.toObject(AppUser.class);
                    if (currentUser == null) return;
                    currentUser.userId = currentDoc.getId();

                    Log.d("SKILLSWAP_DEBUG", "Current User: " + currentUser.name + " | Teach: " + currentUser.skillTeach + " | Study: " + currentUser.skillStudy);

                    db.collection("Users").get().addOnSuccessListener(queryDocumentSnapshots -> {
                        matchList.clear();
                        Log.d("SKILLSWAP_DEBUG", "Total users in collection: " + queryDocumentSnapshots.size());

                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            AppUser otherUser = doc.toObject(AppUser.class);
                            if (otherUser == null || doc.getId().equals(currentUserId)) continue;

                            otherUser.userId = doc.getId();

                            boolean match = isMatch(currentUser, otherUser);
                            Log.d("SKILLSWAP_DEBUG", "Comparing with: " + otherUser.name + " (Teach: " + otherUser.skillTeach + ", Study: " + otherUser.skillStudy + ") | Match: " + match);

                            if (match) {
                                matchList.add(otherUser);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        Log.d("SKILLSWAP_DEBUG", "Final match list size: " + matchList.size());
                    }).addOnFailureListener(e -> Log.e("SKILLSWAP_DEBUG", "Error getting users", e));
                }).addOnFailureListener(e -> Log.e("SKILLSWAP_DEBUG", "Error getting current user", e));
    }

    private boolean isMatch(AppUser current, AppUser other) {
        if (current.mode == null || other.mode == null) return false;
        if (current.skillTeach == null || current.skillStudy == null) return false;
        if (other.skillTeach == null || other.skillStudy == null) return false;

        // ТОЛЬКО СТРОГОЕ СРАВНЕНИЕ (как в твоём первом коде)
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
    public void onMatchClick(AppUser targetUser) {
        Log.d("SKILLSWAP_DEBUG", "Click on user: " + targetUser.name + " ID: " + targetUser.userId);

        if (targetUser.userId == null) {
            Toast.makeText(requireContext(), "Error: User ID is null", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Упрощаем проверку. Просто пытаемся отправить запрос.
        // Если юзер в чате, мы это обработаем на этапе принятия запроса в ChatFragment.
        // Это уберет потенциальный затык с Filter.or
        sendChatRequest(targetUser);
    }

    private void sendChatRequest(AppUser targetUser) {
        // Генерируем ID запроса
        String requestId = currentUserId.compareTo(targetUser.userId) < 0
                ? currentUserId + "_" + targetUser.userId
                : targetUser.userId + "_" + currentUserId;

        Log.d("SKILLSWAP_DEBUG", "Generated requestId: " + requestId);

        // Подготавливаем данные
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("requestId", requestId);
        requestData.put("fromUserId", currentUserId);
        requestData.put("fromName", (currentUser != null && currentUser.name != null) ? currentUser.name : "Someone");
        requestData.put("toUserId", targetUser.userId);
        requestData.put("status", "pending");
        requestData.put("timestamp", System.currentTimeMillis());

        // Сохраняем в Firestore
        db.collection("ChatRequests").document(requestId)
                .set(requestData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("SKILLSWAP_DEBUG", "Firestore success: Request saved!");
                    Toast.makeText(requireContext(), "Request sent to " + targetUser.name, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("SKILLSWAP_DEBUG", "Firestore error: " + e.getMessage());
                    Toast.makeText(requireContext(), "Failed to send request", Toast.LENGTH_SHORT).show();
                });
    }
}
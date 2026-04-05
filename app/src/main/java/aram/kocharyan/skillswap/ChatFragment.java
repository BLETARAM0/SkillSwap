package aram.kocharyan.skillswap;

import android.content.Intent;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Filter;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatFragment extends Fragment implements RequestAdapter.OnRequestActionListener {

    private RecyclerView recyclerRequests, recyclerChats;
    private RequestAdapter requestAdapter;
    private ChatAdapter chatAdapter;
    private List<ChatRequest> requestList = new ArrayList<>();
    private List<Chat> chatList = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        recyclerRequests = view.findViewById(R.id.recyclerRequests);
        recyclerChats = view.findViewById(R.id.recyclerChats);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        recyclerRequests.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerChats.setLayoutManager(new LinearLayoutManager(requireContext()));

        requestAdapter = new RequestAdapter(requestList, this);
        chatAdapter = new ChatAdapter(chatList, this::openChatActivity);

        recyclerRequests.setAdapter(requestAdapter);
        recyclerChats.setAdapter(chatAdapter);

        loadChats();
        loadChatRequests();

        return view;
    }

    private void loadChats() {
        Log.d("SKILLSWAP_DEBUG", "Starting loadChats. My ID: " + currentUserId);

        db.collection("Chats")
                .whereEqualTo("user1", currentUserId)
                .addSnapshotListener((v1, e1) -> {
                    if (e1 != null) {
                        // ВАЖНО: это покажет реальную причину (например, ссылку на создание индекса)
                        Log.e("SKILLSWAP_DEBUG", "Error in query1: " + e1.getMessage());
                    }

                    db.collection("Chats")
                            .whereEqualTo("user2", currentUserId)
                            .addSnapshotListener((v2, e2) -> {
                                if (e2 != null) {
                                    Log.e("SKILLSWAP_DEBUG", "Error in query2: " + e2.getMessage());
                                }

                                if (e1 != null || e2 != null) return;

                                chatList.clear();
                                if (v1 != null) {
                                    for (DocumentSnapshot doc : v1.getDocuments()) {
                                        Chat chat = doc.toObject(Chat.class);
                                        if (chat != null) { chat.chatId = doc.getId(); chatList.add(chat); }
                                    }
                                }
                                if (v2 != null) {
                                    for (DocumentSnapshot doc : v2.getDocuments()) {
                                        Chat chat = doc.toObject(Chat.class);
                                        if (chat != null) { chat.chatId = doc.getId(); chatList.add(chat); }
                                    }
                                }

                                Log.d("SKILLSWAP_DEBUG", "Update UI. Chats found: " + chatList.size());
                                chatAdapter.notifyDataSetChanged();
                            });
                });
    }

    private void loadChatRequests() {
        db.collection("ChatRequests")
                .whereEqualTo("toUserId", currentUserId)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    requestList.clear();
                    // Если есть чат, игнорируем входящие запросы в UI
                    if (!chatList.isEmpty()) {
                        requestAdapter.notifyDataSetChanged();
                        return;
                    }
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        ChatRequest req = doc.toObject(ChatRequest.class);
                        if (req != null) {
                            req.requestId = doc.getId();
                            requestList.add(req);
                        }
                    }
                    requestAdapter.notifyDataSetChanged();
                });
    }

    @Override
    public void onAction(ChatRequest request, boolean accept) {
        if (!accept) {
            db.collection("ChatRequests").document(request.requestId).delete()
                    .addOnSuccessListener(aVoid -> Toast.makeText(requireContext(), "Declined", Toast.LENGTH_SHORT).show());
            return;
        }

        // ВРЕМЕННО: Убираем проверку на существующие чаты, чтобы исключить зависание Filter.or
        Log.d("SKILLSWAP_DEBUG", "Button Accept clicked for request: " + request.requestId);
        createChat(request);
    }

    private void createChat(ChatRequest request) {
        // Проверка на null
        if (request.fromUserId == null || currentUserId == null) {
            Log.e("SKILLSWAP_DEBUG", "Error: One of the User IDs is NULL");
            return;
        }

        String chatId = generateChatId(request.fromUserId, currentUserId);
        Log.d("SKILLSWAP_DEBUG", "Generated Chat ID: " + chatId);

        Map<String, Object> chatData = new HashMap<>();
        chatData.put("chatId", chatId);
        chatData.put("user1", request.fromUserId);
        chatData.put("user2", currentUserId);
        chatData.put("lastMessage", "Chat started");
        chatData.put("timestamp", System.currentTimeMillis());

        // Прямая запись
        db.collection("Chats").document(chatId)
                .set(chatData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("SKILLSWAP_DEBUG", "SUCCESS: Chat created in Firestore!");

                    // Удаляем текущий запрос
                    db.collection("ChatRequests").document(request.requestId).delete()
                            .addOnSuccessListener(v -> Log.d("SKILLSWAP_DEBUG", "Request deleted."));

                    Toast.makeText(requireContext(), "Chat created!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("SKILLSWAP_DEBUG", "FIRESTORE ERROR: " + e.getMessage());
                    Toast.makeText(requireContext(), "Firebase Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
    private void deleteAllUserRequests(String uid) {
        db.collection("ChatRequests").whereEqualTo("toUserId", uid).get()
                .addOnSuccessListener(query -> { for (DocumentSnapshot doc : query) doc.getReference().delete(); });
        db.collection("ChatRequests").whereEqualTo("fromUserId", uid).get()
                .addOnSuccessListener(query -> { for (DocumentSnapshot doc : query) doc.getReference().delete(); });
    }

    private String generateChatId(String uid1, String uid2) {
        return uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }

    private void openChatActivity(Chat chat) {
        Intent intent = new Intent(requireContext(), ChatActivity.class);
        intent.putExtra("chatId", chat.chatId);
        String otherUserId = chat.user1.equals(currentUserId) ? chat.user2 : chat.user1;
        intent.putExtra("otherUserId", otherUserId);
        startActivity(intent);
    }
}
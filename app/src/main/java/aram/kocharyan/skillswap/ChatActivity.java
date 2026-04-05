package aram.kocharyan.skillswap;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText etMessage;
    private Button btnSend;

    private MessageAdapter adapter;
    private List<Message> messageList = new ArrayList<>();

    private FirebaseFirestore db;
    private String currentUserId;
    private String chatId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat); // Нужно создать этот XML

        chatId = getIntent().getStringExtra("chatId");
        if (chatId == null) {
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        recyclerView = findViewById(R.id.recyclerMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Сообщения появляются снизу
        recyclerView.setLayoutManager(layoutManager);

        adapter = new MessageAdapter(messageList, currentUserId);
        recyclerView.setAdapter(adapter);

        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString();
            if (!text.trim().isEmpty()) {
                sendMessage(text);
            }
        });

        loadMessages();
    }

    private void sendMessage(String text) {
        if (text.trim().isEmpty()) return;

        // Путь: Chats -> {chatId} -> Messages -> {autoID}
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("text", text);
        messageData.put("senderId", currentUserId);
        messageData.put("timestamp", System.currentTimeMillis());

        db.collection("Chats").document(chatId)
                .collection("Messages") // Подколлекция сообщений
                .add(messageData)
                .addOnSuccessListener(documentReference -> {
                    etMessage.setText(""); // Очистить поле после отправки
                    Log.d("SKILLSWAP_DEBUG", "Message sent successfully!");
                })
                .addOnFailureListener(e -> {
                    Log.e("SKILLSWAP_DEBUG", "Error sending message", e);
                    Toast.makeText(this, "Send failed", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadMessages() {
        db.collection("Chats").document(chatId).collection("Messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    messageList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Message msg = doc.toObject(Message.class);
                        if (msg != null) messageList.add(msg);
                    }
                    adapter.notifyDataSetChanged();
                    if (!messageList.isEmpty()) {
                        recyclerView.scrollToPosition(messageList.size() - 1);
                    }
                });
    }
}
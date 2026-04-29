package aram.kocharyan.skillswap;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity implements MessageAdapter.MessageActionListener {

    private RecyclerView recyclerView;
    private EditText etMessage;
    private ImageButton btnSend;
    private TextView tvReplyPreview;
    private View layoutReply;

    private MessageAdapter adapter;
    private List<Message> messageList = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentUserId, chatId;
    private String editingId = null;
    private Message replyMessage = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatId = getIntent().getStringExtra("chatId");
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        recyclerView = findViewById(R.id.recyclerMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        layoutReply = findViewById(R.id.layoutReplyPreview); // Создай в XML
        tvReplyPreview = findViewById(R.id.tvReplyPreviewText);

        adapter = new MessageAdapter(messageList, currentUserId, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Кнопка: микрофон при старте
        btnSend.setImageResource(android.R.drawable.ic_btn_speak_now);

        etMessage.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int b, int c) {
                if (s.toString().trim().length() > 0) btnSend.setImageResource(android.R.drawable.ic_menu_send);
                else btnSend.setImageResource(android.R.drawable.ic_btn_speak_now);
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });

        btnSend.setOnClickListener(v -> {
            String txt = etMessage.getText().toString().trim();
            if (txt.isEmpty()) {
                Toast.makeText(this, "Hold for Voice", Toast.LENGTH_SHORT).show();
            } else {
                if (editingId != null) commitEdit(txt);
                else sendMessage(txt);
            }
        });

        setupSwipeReply();
        loadMessages();

        // В onCreate добавь:
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish()); // Закрывает чат и возвращает назад

        String otherUserId = getIntent().getStringExtra("otherUserId");
        if (otherUserId != null) {
            // Загружаем имя собеседника из коллекции Users
            db.collection("Users").document(otherUserId).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String name = doc.getString("name");
                    String surname = doc.getString("surname");
                    TextView tvUserName = findViewById(R.id.tvUserName);
                    tvUserName.setText(name + " " + surname);
                }
            });
        }
    }

    private void sendMessage(String text) {
        DocumentReference ref = db.collection("Chats").document(chatId).collection("Messages").document();
        String myMsgId = ref.getId(); // Генерируем ID заранее

        Message msg = new Message(myMsgId, currentUserId, text, System.currentTimeMillis(), 0, "text", false);

        if (replyMessage != null) {
            msg.replyToText = replyMessage.text;
        }

        ref.set(msg).addOnSuccessListener(aVoid -> {
            etMessage.setText("");
            cancelReply();
            // После успешной записи в облако — ставим статус 1
            ref.update("status", 1);
        });
    }

    private void loadMessages() {
        db.collection("Chats").document(chatId).collection("Messages").orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, e) -> {
                    if (value == null) return;
                    messageList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Message msg = doc.toObject(Message.class);
                        if (msg != null) {
                            messageList.add(msg);

                            // Если сообщение прислал НЕ Я и оно еще не прочитано (status < 2)
                            if (!msg.senderId.equals(currentUserId) && msg.status < 2) {
                                // Обновляем статус на "прочитано" в базе
                                db.collection("Chats").document(chatId)
                                        .collection("Messages").document(msg.messageId)
                                        .update("status", 2);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                    recyclerView.scrollToPosition(messageList.size() - 1);
                });
    }

    private void setupSwipeReply() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder t) { return false; }
            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {
                onReply(messageList.get(vh.getAdapterPosition()));
                adapter.notifyItemChanged(vh.getAdapterPosition());
            }
        }).attachToRecyclerView(recyclerView);
    }

    @Override public void onReply(Message m) {
        replyMessage = m;
        layoutReply.setVisibility(View.VISIBLE);
        tvReplyPreview.setText(m.text);
    }

    public void cancelReply(View v) { cancelReply(); }
    private void cancelReply() {
        replyMessage = null;
        layoutReply.setVisibility(View.GONE);
    }

    @Override public void onEdit(Message m) {
        editingId = m.messageId;
        etMessage.setText(m.text);
        etMessage.requestFocus();
    }

    private void commitEdit(String t) {
        db.collection("Chats").document(chatId).collection("Messages").document(editingId)
                .update("text", t, "edited", true).addOnSuccessListener(v -> {
                    editingId = null;
                    etMessage.setText("");
                });
    }

    @Override public void onDelete(Message m) {
        db.collection("Chats").document(chatId).collection("Messages").document(m.messageId)
                .update("type", "deleted", "text", "Message deleted");
    }
}
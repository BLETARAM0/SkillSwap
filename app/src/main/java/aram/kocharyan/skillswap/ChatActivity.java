package aram.kocharyan.skillswap;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import com.google.firebase.firestore.ListenerRegistration;
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
    private ImageButton btnAttach;

    // Переменная для управления слушателем базы данных
    private ListenerRegistration chatListener;

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
        layoutReply = findViewById(R.id.layoutReplyPreview);
        tvReplyPreview = findViewById(R.id.tvReplyPreviewText);

        adapter = new MessageAdapter(messageList, currentUserId, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

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

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        String otherUserId = getIntent().getStringExtra("otherUserId");
        if (otherUserId != null) {
            db.collection("Users").document(otherUserId).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String name = doc.getString("name");
                    String surname = doc.getString("surname");
                    TextView tvUserName = findViewById(R.id.tvUserName);
                    if (tvUserName != null) tvUserName.setText(name + " " + surname);
                }
            });
        }

        btnAttach = findViewById(R.id.btnAttach);
        btnAttach.setOnClickListener(v -> showAttachmentMenu());
    }

    private void showAttachmentMenu() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_attachment_picker, null);

        view.findViewById(R.id.itemGallery).setOnClickListener(v -> {
            // Открыть галерею
            openGallery();
            dialog.dismiss();
        });

        // Добавь обработку для камеры, документов и т.д.

        dialog.setContentView(view);
        dialog.show();
    }

    private void openGallery() {
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 101); // 101 - код для галереи
    }

    private void sendMessage(String text) {
        DocumentReference ref = db.collection("Chats").document(chatId).collection("Messages").document();
        String myMsgId = ref.getId();

        Message msg = new Message(myMsgId, currentUserId, text, System.currentTimeMillis(), 0, "text", false);

        if (replyMessage != null) {
            msg.replyToText = replyMessage.text;
        }

        ref.set(msg).addOnSuccessListener(aVoid -> {
            etMessage.setText("");
            cancelReply();
            ref.update("status", 1);
        });
    }

    private void loadMessages() {
        // Инициализируем слушатель и сохраняем его в chatListener
        chatListener = db.collection("Chats").document(chatId)
                .collection("Messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, e) -> {
                    if (e != null) {
                        Log.e("ChatActivity", "Listen failed", e);
                        return;
                    }

                    if (value != null) {
                        messageList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Message msg = doc.toObject(Message.class);
                            if (msg != null) {
                                messageList.add(msg);

                                // Логика "Птичек": если сообщение чужое и я сейчас в этой Activity
                                if (!msg.senderId.equals(currentUserId) && msg.status < 2) {
                                    doc.getReference().update("status", 2);
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                        if (messageList.size() > 0) {
                            recyclerView.scrollToPosition(messageList.size() - 1);
                        }
                    }
                });
    }

    // Этот метод вызывается, когда пользователь выходит из чата
    @Override
    protected void onStop() {
        super.onStop();
        // Удаляем слушателя, чтобы статусы не обновлялись в фоне
        if (chatListener != null) {
            chatListener.remove();
            chatListener = null;
        }
    }

    private void setupSwipeReply() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder t) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {
                int position = vh.getAdapterPosition();
                Message m = messageList.get(position);

                // Если сообщение удалено, сбрасываем свайп и ничего не делаем
                if ("deleted".equals(m.type)) {
                    adapter.notifyItemChanged(position);
                } else {
                    // Если живое — вызываем реплай
                    onReply(m);
                    adapter.notifyItemChanged(position);
                }
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

    @Override
    public void onDelete(Message m) {
        db.collection("Chats").document(chatId).collection("Messages").document(m.messageId)
                .update(
                        "type", "deleted",
                        "text", "Message deleted",
                        "replyToText", null // Убираем реплай, чтобы он не висел над удаленным сообщением
                );
    }
}
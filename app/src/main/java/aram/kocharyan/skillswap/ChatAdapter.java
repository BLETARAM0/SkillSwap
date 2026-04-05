package aram.kocharyan.skillswap;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final List<Chat> chatList;
    private final String currentUserId;
    private final OnChatClickListener listener; // Добавляем интерфейс

    // Интерфейс для обработки клика
    public interface OnChatClickListener {
        void onChatClick(Chat chat);
    }

    // Обновленный конструктор с двумя параметрами
    public ChatAdapter(List<Chat> chatList, OnChatClickListener listener) {
        this.chatList = chatList;
        this.listener = listener;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = chatList.get(position);

        // Определяем ID собеседника
        String otherUserId = chat.user1.equals(currentUserId) ? chat.user2 : chat.user1;

        // Загружаем имя собеседника
        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(otherUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String surname = doc.getString("surname");
                        if (name != null && surname != null) {
                            holder.tvChatName.setText(name + " " + surname);
                        }
                    }
                });

        // Устанавливаем клик на весь элемент списка
        holder.itemView.setOnClickListener(v -> listener.onChatClick(chat));
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvChatName;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChatName = itemView.findViewById(R.id.tvChatName);
        }
    }
}
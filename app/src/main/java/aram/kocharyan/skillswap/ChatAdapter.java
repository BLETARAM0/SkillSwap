package aram.kocharyan.skillswap;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final List<Chat> chatList;
    private final String currentUserId;
    private final OnChatClickListener listener;

    public interface OnChatClickListener {
        void onChatClick(Chat chat);
    }

    public ChatAdapter(List<Chat> chatList, OnChatClickListener listener) {
        this.chatList      = chatList;
        this.listener      = listener;
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
        String otherUserId = chat.user1.equals(currentUserId) ? chat.user2 : chat.user1;

        // Last message
        if (chat.lastMessage != null && !chat.lastMessage.isEmpty()) {
            holder.tvLastMessage.setText(chat.lastMessage);
        } else {
            holder.tvLastMessage.setText("Chat started");
        }

        // Time
        if (chat.timestamp > 0) {
            holder.tvTime.setText(formatTime(chat.timestamp));
        }

        // Загружаем имя и аватарку собеседника
        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(otherUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name      = doc.getString("name");
                        String surname   = doc.getString("surname");
                        String avatarUrl = doc.getString("avatarUrl");

                        if (name != null)
                            holder.tvChatName.setText(name + " " + (surname != null ? surname : ""));

                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            Glide.with(holder.itemView.getContext())
                                    .load(avatarUrl)
                                    .transform(new CircleCrop())
                                    .placeholder(android.R.drawable.ic_menu_myplaces)
                                    .into(holder.ivAvatar);
                        } else {
                            holder.ivAvatar.setImageResource(android.R.drawable.ic_menu_myplaces);
                        }
                    }
                });

        // Непрочитанные — считаем из Firestore
        FirebaseFirestore.getInstance()
                .collection("Chats").document(chat.chatId)
                .collection("Messages")
                .whereEqualTo("status", 1)
                .whereNotEqualTo("senderId", currentUserId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    int unread = snapshot.size();
                    if (unread > 0) {
                        holder.tvUnreadCount.setVisibility(View.VISIBLE);
                        holder.tvUnreadCount.setText(unread > 99 ? "99+" : String.valueOf(unread));
                    } else {
                        holder.tvUnreadCount.setVisibility(View.GONE);
                    }
                });

        holder.itemView.setOnClickListener(v -> listener.onChatClick(chat));
    }

    private String formatTime(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        if (diff < 60_000) return "now";
        if (diff < 3_600_000) return (diff / 60_000) + "m";
        if (diff < 86_400_000) return new SimpleDateFormat("HH:mm", Locale.US).format(new Date(timestamp));
        return new SimpleDateFormat("MMM d", Locale.US).format(new Date(timestamp));
    }

    @Override
    public int getItemCount() { return chatList.size(); }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView  tvChatName, tvLastMessage, tvTime, tvUnreadCount;
        ImageView ivAvatar;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChatName    = itemView.findViewById(R.id.tvChatName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime        = itemView.findViewById(R.id.tvTime);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
            ivAvatar      = itemView.findViewById(R.id.ivAvatar);
        }
    }
}
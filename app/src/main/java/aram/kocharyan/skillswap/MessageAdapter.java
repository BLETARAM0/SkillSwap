package aram.kocharyan.skillswap;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private final List<Message> messageList;
    private final String currentUserId;
    private final MessageActionListener actionListener;

    public interface MessageActionListener {
        void onDelete(Message message);
        void onEdit(Message message);
        void onReply(Message message);
    }

    public MessageAdapter(List<Message> messageList, String currentUserId, MessageActionListener actionListener) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
        this.actionListener = actionListener;
    }

    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).senderId.equals(currentUserId) ? 1 : 0;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int res = (viewType == 1) ? R.layout.item_msg_right : R.layout.item_msg_left;
        return new MessageViewHolder(LayoutInflater.from(parent.getContext()).inflate(res, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);
        Context context = holder.itemView.getContext();

        // Дата по центру
        long curTime = message.timestamp;
        long prevTime = (position > 0) ? messageList.get(position - 1).timestamp : 0;
        if (position == 0 || !isSameDay(curTime, prevTime)) {
            holder.tvDateHeader.setVisibility(View.VISIBLE);
            holder.tvDateHeader.setText(formatDate(curTime));
        } else {
            holder.tvDateHeader.setVisibility(View.GONE);
        }

        // Логика ответа (Reply)
        if (message.replyToText != null) {
            holder.tvReplyText.setVisibility(View.VISIBLE);
            holder.tvReplyText.setText(message.replyToText);
        } else {
            holder.tvReplyText.setVisibility(View.GONE);
        }

        // Удаление/Текст
        if ("deleted".equals(message.type)) {
            holder.tvMessage.setText("Message deleted");
            holder.tvMessage.setTypeface(null, Typeface.ITALIC);
            holder.tvTime.setVisibility(View.GONE);
        } else {
            holder.tvMessage.setText(message.text);
            holder.tvMessage.setTypeface(null, Typeface.NORMAL);
            holder.tvTime.setVisibility(View.VISIBLE);
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            holder.tvTime.setText(sdf.format(message.timestamp));
        }

        holder.tvEdited.setVisibility(message.edited ? View.VISIBLE : View.GONE);

        // Статусы (Птички)
// Внутри MessageAdapter, для сообщений справа (sender)
        if (holder.tvStatus != null) {
// Замени message.getStatus() на message.status
            int status = message.status;

            holder.tvStatus.setText("✓");

            if (status == 2) {
                holder.tvStatus.setTextColor(Color.parseColor("#03A9F4")); // Синий
            } else {
                holder.tvStatus.setTextColor(Color.GRAY); // Серый
            }
        }

        // Лонг клик для меню
        holder.itemView.setOnLongClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.tvMessage);
            popup.getMenu().add("Reply");
            popup.getMenu().add("Copy");
            if (message.senderId.equals(currentUserId) && !"deleted".equals(message.type)) {
                popup.getMenu().add("Edit");
                popup.getMenu().add("Delete");
            }
            popup.setOnMenuItemClickListener(item -> {
                switch (item.getTitle().toString()) {
                    case "Reply": actionListener.onReply(message); break;
                    case "Copy":
                        ClipboardManager cb = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                        cb.setPrimaryClip(ClipData.newPlainText("msg", message.text));
                        break;
                    case "Edit": actionListener.onEdit(message); break;
                    case "Delete": actionListener.onDelete(message); break;
                }
                return true;
            });
            popup.show();
            return true;
        });
    }

    private boolean isSameDay(long t1, long t2) {
        Calendar c1 = Calendar.getInstance(); c1.setTimeInMillis(t1);
        Calendar c2 = Calendar.getInstance(); c2.setTimeInMillis(t2);
        return c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR) && c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR);
    }

    private String formatDate(long t) {
        return new SimpleDateFormat("d MMMM", Locale.getDefault()).format(t);
    }

    @Override public int getItemCount() { return messageList.size(); }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvDateHeader, tvMessage, tvTime, tvStatus, tvEdited, tvReplyText;
        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateHeader = itemView.findViewById(R.id.tvDateHeader);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvEdited = itemView.findViewById(R.id.tvEdited);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvReplyText = itemView.findViewById(R.id.tvReplyText); // ДОБАВЬ В XML
        }
    }
}
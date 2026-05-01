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

        // Время
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.US);
        String timeStr = timeFormat.format(message.timestamp);

        // ЛОГИКА УДАЛЕНИЯ И ТЕКСТА
        if ("deleted".equals(message.type)) {
            holder.tvMessage.setText("Message deleted");
            holder.tvMessage.setTextColor(Color.GRAY);
            holder.tvMessage.setTypeface(null, Typeface.ITALIC);
            holder.tvReplyText.setVisibility(View.GONE);
            holder.tvEdited.setVisibility(View.GONE); // У удаленного не пишем edited
            holder.tvTime.setText(timeStr);
        } else {
            holder.tvMessage.setText(message.text);
            holder.tvMessage.setTextColor(Color.BLACK);
            holder.tvMessage.setTypeface(null, Typeface.NORMAL);

            // Реплай только у живых сообщений
            if (message.replyToText != null) {
                holder.tvReplyText.setVisibility(View.VISIBLE);
                holder.tvReplyText.setText(message.replyToText);
            } else {
                holder.tvReplyText.setVisibility(View.GONE);
            }

            // Исправленный EDITED с пробелом
            if (message.edited) {
                holder.tvEdited.setVisibility(View.VISIBLE);
                holder.tvEdited.setText("edited " + timeStr);
                holder.tvTime.setVisibility(View.GONE); // Скрываем обычное время, если есть edited
            } else {
                holder.tvEdited.setVisibility(View.GONE);
                holder.tvTime.setVisibility(View.VISIBLE);
                holder.tvTime.setText(timeStr);
            }
        }

        // Птички
        if (holder.tvStatus != null) {
            holder.tvStatus.setText("✓");
            holder.tvStatus.setTextColor(message.status == 2 ? Color.parseColor("#03A9F4") : Color.GRAY);
        }

        // Лонг клик
        holder.itemView.setOnLongClickListener(v -> {
            if ("deleted".equals(message.type)) return false; // Удаленные нельзя трогать

            PopupMenu popup = new PopupMenu(context, holder.tvMessage);
            popup.getMenu().add("Reply");
            popup.getMenu().add("Copy");
            if (message.senderId.equals(currentUserId)) {
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
        Calendar now = Calendar.getInstance();
        Calendar msg = Calendar.getInstance();
        msg.setTimeInMillis(t);

        if (isSameDay(now.getTimeInMillis(), t)) return "Today";

        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        if (isSameDay(yesterday.getTimeInMillis(), t)) return "Yesterday";

        SimpleDateFormat formatter = (now.get(Calendar.YEAR) == msg.get(Calendar.YEAR))
                ? new SimpleDateFormat("MMM d", Locale.US)
                : new SimpleDateFormat("MMM d, yyyy", Locale.US);
        return formatter.format(msg.getTime());
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
            tvReplyText = itemView.findViewById(R.id.tvReplyText);
        }
    }
}
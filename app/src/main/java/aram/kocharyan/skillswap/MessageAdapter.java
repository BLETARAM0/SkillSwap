package aram.kocharyan.skillswap;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
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

    public MessageAdapter(List<Message> messageList, String currentUserId,
                          MessageActionListener actionListener) {
        this.messageList    = messageList;
        this.currentUserId  = currentUserId;
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
        return new MessageViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(res, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);
        Context context = holder.itemView.getContext();

        // ── Дата-разделитель ────────────────────────────────────────────────
        long curTime  = message.timestamp;
        long prevTime = (position > 0) ? messageList.get(position - 1).timestamp : 0;
        if (position == 0 || !isSameDay(curTime, prevTime)) {
            holder.tvDateHeader.setVisibility(View.VISIBLE);
            holder.tvDateHeader.setText(formatDate(curTime));
        } else {
            holder.tvDateHeader.setVisibility(View.GONE);
        }

        String timeStr = new SimpleDateFormat("HH:mm", Locale.US).format(message.timestamp);

        // ── Сброс видимости ─────────────────────────────────────────────────
        holder.ivPhoto.setVisibility(View.GONE);
        holder.layoutDocument.setVisibility(View.GONE);
        holder.tvMessage.setVisibility(View.GONE);

        // ── Удалённое сообщение ─────────────────────────────────────────────
        if ("deleted".equals(message.type)) {
            holder.tvMessage.setVisibility(View.VISIBLE);
            holder.tvMessage.setText("Message deleted");
            holder.tvMessage.setTextColor(Color.GRAY);
            holder.tvMessage.setTypeface(null, Typeface.ITALIC);
            holder.tvReplyText.setVisibility(View.GONE);
            holder.tvEdited.setVisibility(View.GONE);
            holder.tvTime.setText(timeStr);
            holder.tvTime.setVisibility(View.VISIBLE);
            return;
        }

        // ── Фото ────────────────────────────────────────────────────────────
        if ("image".equals(message.type)) {
            holder.ivPhoto.setVisibility(View.VISIBLE);

            Glide.with(context)
                    .asBitmap()
                    .load(message.text) // text хранит URL
                    .transform(new RoundedCorners(32))
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap bitmap,
                                                    Transition<? super Bitmap> transition) {
                            holder.ivPhoto.setImageBitmap(bitmap);

                            // Автосохранение в галерею только для входящих сообщений
                            if (!message.senderId.equals(currentUserId)) {
                                saveImageToGallery(context, bitmap, message.messageId);
                            }
                        }
                    });

            // Клик — открыть полный экран
            holder.ivPhoto.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(message.text), "image/*");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try { context.startActivity(intent); }
                catch (Exception e) {
                    Toast.makeText(context, "No app to open image", Toast.LENGTH_SHORT).show();
                }
            });

            // ── Документ ────────────────────────────────────────────────────────
        } else if ("document".equals(message.type)) {
            holder.layoutDocument.setVisibility(View.VISIBLE);
            String displayName = (message.fileName != null && !message.fileName.isEmpty())
                    ? message.fileName : "Document";
            holder.tvFileName.setText(displayName);

            holder.layoutDocument.setOnClickListener(v -> openDocument(context, message.text));

            // ── Текст ────────────────────────────────────────────────────────────
        } else {
            holder.tvMessage.setVisibility(View.VISIBLE);
            holder.tvMessage.setText(message.text);
            holder.tvMessage.setTextColor(Color.BLACK);
            holder.tvMessage.setTypeface(null, Typeface.NORMAL);
        }

        // ── Reply preview ───────────────────────────────────────────────────
        if (message.replyToText != null && !message.replyToText.isEmpty()) {
            holder.tvReplyText.setVisibility(View.VISIBLE);
            holder.tvReplyText.setText(message.replyToText);
        } else {
            holder.tvReplyText.setVisibility(View.GONE);
        }

        // ── Edited / Time ───────────────────────────────────────────────────
        if (message.edited) {
            holder.tvEdited.setVisibility(View.VISIBLE);
            holder.tvEdited.setText("edited " + timeStr);
            holder.tvTime.setVisibility(View.GONE);
        } else {
            holder.tvEdited.setVisibility(View.GONE);
            holder.tvTime.setVisibility(View.VISIBLE);
            holder.tvTime.setText(timeStr);
        }

        // ── Статус (птички) ─────────────────────────────────────────────────
        if (holder.tvStatus != null) {
            holder.tvStatus.setText(message.status >= 2 ? "✓✓" : "✓");
            holder.tvStatus.setTextColor(
                    message.status == 2 ? Color.parseColor("#03A9F4") : Color.GRAY);
        }

        // ── Long click (popup menu) ─────────────────────────────────────────
        holder.itemView.setOnLongClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.itemView);
            popup.getMenu().add("Reply");
            if ("text".equals(message.type)) popup.getMenu().add("Copy");
            if (message.senderId.equals(currentUserId)) {
                if ("text".equals(message.type)) popup.getMenu().add("Edit");
                popup.getMenu().add("Delete");
            }
            popup.setOnMenuItemClickListener(item -> {
                switch (item.getTitle().toString()) {
                    case "Reply": actionListener.onReply(message); break;
                    case "Copy":
                        ClipboardManager cb = (ClipboardManager)
                                context.getSystemService(Context.CLIPBOARD_SERVICE);
                        cb.setPrimaryClip(ClipData.newPlainText("msg", message.text));
                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show();
                        break;
                    case "Edit":   actionListener.onEdit(message);   break;
                    case "Delete": actionListener.onDelete(message); break;
                }
                return true;
            });
            popup.show();
            return true;
        });
    }

    // ── Сохранение фото в галерею ───────────────────────────────────────────

    private void saveImageToGallery(Context context, Bitmap bitmap, String messageId) {
        // Проверяем не сохраняли ли уже (по тегу в SharedPreferences)
        android.content.SharedPreferences prefs =
                context.getSharedPreferences("saved_images", Context.MODE_PRIVATE);
        if (prefs.getBoolean(messageId, false)) return; // уже сохранено

        try {
            String fileName = "SkillSwap_" + System.currentTimeMillis() + ".jpg";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/SkillSwap");

                Uri uri = context.getContentResolver()
                        .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (OutputStream out = context.getContentResolver().openOutputStream(uri)) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out);
                    }
                }
            } else {
                // Android 9 и ниже
                File dir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES), "SkillSwap");
                if (!dir.exists()) dir.mkdirs();
                File file = new File(dir, fileName);
                try (FileOutputStream out = new FileOutputStream(file)) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out);
                }
                // Обновить MediaStore чтобы фото появилось в галерее
                context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        Uri.fromFile(file)));
            }

            // Запоминаем что сохранили
            prefs.edit().putBoolean(messageId, true).apply();

        } catch (Exception e) {
            // Тихо — не беспокоим пользователя если не удалось
        }
    }

    // ── Открыть документ ───────────────────────────────────────────────────

    private void openDocument(Context context, String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) return;
        String ext = MimeTypeMap.getFileExtensionFromUrl(fileUrl);
        String mime = (ext != null)
                ? MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase())
                : "*/*";
        if (mime == null) mime = "*/*";

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(fileUrl), mime);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(Intent.createChooser(intent, "Open with"));
        } catch (Exception e) {
            Toast.makeText(context, "No app to open this file", Toast.LENGTH_SHORT).show();
        }
    }

    // ── Date helpers ────────────────────────────────────────────────────────

    private boolean isSameDay(long t1, long t2) {
        Calendar c1 = Calendar.getInstance(); c1.setTimeInMillis(t1);
        Calendar c2 = Calendar.getInstance(); c2.setTimeInMillis(t2);
        return c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
                && c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR);
    }

    private String formatDate(long t) {
        Calendar now = Calendar.getInstance();
        Calendar msg = Calendar.getInstance(); msg.setTimeInMillis(t);
        if (isSameDay(now.getTimeInMillis(), t)) return "Today";
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        if (isSameDay(yesterday.getTimeInMillis(), t)) return "Yesterday";
        SimpleDateFormat fmt = (now.get(Calendar.YEAR) == msg.get(Calendar.YEAR))
                ? new SimpleDateFormat("MMM d", Locale.US)
                : new SimpleDateFormat("MMM d, yyyy", Locale.US);
        return fmt.format(msg.getTime());
    }

    @Override public int getItemCount() { return messageList.size(); }

    // ── ViewHolder ──────────────────────────────────────────────────────────

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView    tvDateHeader, tvMessage, tvTime, tvStatus, tvEdited, tvReplyText, tvFileName;
        ImageView   ivPhoto;
        LinearLayout layoutDocument;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateHeader   = itemView.findViewById(R.id.tvDateHeader);
            tvMessage      = itemView.findViewById(R.id.tvMessage);
            tvTime         = itemView.findViewById(R.id.tvTime);
            tvEdited       = itemView.findViewById(R.id.tvEdited);
            tvStatus       = itemView.findViewById(R.id.tvStatus);
            tvReplyText    = itemView.findViewById(R.id.tvReplyText);
            ivPhoto        = itemView.findViewById(R.id.ivPhoto);
            layoutDocument = itemView.findViewById(R.id.layoutDocument);
            tvFileName     = itemView.findViewById(R.id.tvFileName);
        }
    }
}
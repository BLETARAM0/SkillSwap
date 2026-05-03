package aram.kocharyan.skillswap;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatActivity extends AppCompatActivity implements MessageAdapter.MessageActionListener {

    private static final String TAG = "ChatActivity";

    // ── Cloudinary ──────────────────────────────────────────────────────────
    private static final String CLOUD_NAME    = "dium7pqky";
    private static final String UPLOAD_PRESET = "SkillSwap";
    private static final String UPLOAD_URL    =
            "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/auto/upload";

    // ── UI ──────────────────────────────────────────────────────────────────
    private RecyclerView recyclerView;
    private EditText etMessage;
    private ImageButton btnSend, btnAttach, btnVideoCall;
    private TextView tvReplyPreview;
    private View layoutReply;

    // ── Data ────────────────────────────────────────────────────────────────
    private MessageAdapter adapter;
    private final List<Message> messageList = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentUserId, chatId, otherUserId;
    private String currentUserName = "User";
    private String editingId = null;
    private Message replyMessage = null;
    private ListenerRegistration chatListener;

    // Для камеры
    private Uri cameraImageUri;

    // ── Activity Result Launchers ───────────────────────────────────────────

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) uploadToCloudinary(uri, "image");
                }
            });

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && cameraImageUri != null) {
                    uploadToCloudinary(cameraImageUri, "image");
                }
            });

    private final ActivityResultLauncher<Intent> documentLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) uploadToCloudinary(uri, "document");
                }
            });

    private final ActivityResultLauncher<String> cameraPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) openCamera();
                else Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            });

    private final ActivityResultLauncher<String> storagePermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) openGallery();
                else Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
            });

    private final ActivityResultLauncher<String[]> callPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean cam = Boolean.TRUE.equals(result.get(android.Manifest.permission.CAMERA));
                boolean mic = Boolean.TRUE.equals(result.get(android.Manifest.permission.RECORD_AUDIO));
                if (cam && mic) startVideoCall();
                else Toast.makeText(this, "Camera and microphone permission required", Toast.LENGTH_SHORT).show();
            });

    // ── Lifecycle ───────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatId      = getIntent().getStringExtra("chatId");
        otherUserId = getIntent().getStringExtra("otherUserId");
        db          = FirebaseFirestore.getInstance();

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Not authorized", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Загружаем своё имя для звонка
        db.collection("Users").document(currentUserId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String name    = doc.getString("name");
                String surname = doc.getString("surname");
                if (name != null) currentUserName = name + (surname != null ? " " + surname : "");
            }
        });

        initViews();
        loadOtherUserName();
        setupSendButton();
        setupVideoCallButton();
        setupSwipeReply();
        loadMessages();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (chatListener != null) {
            chatListener.remove();
            chatListener = null;
        }
    }

    // ── Init ────────────────────────────────────────────────────────────────

    private void initViews() {
        recyclerView   = findViewById(R.id.recyclerMessages);
        etMessage      = findViewById(R.id.etMessage);
        btnSend        = findViewById(R.id.btnSend);
        layoutReply    = findViewById(R.id.layoutReplyPreview);
        tvReplyPreview = findViewById(R.id.tvReplyPreviewText);
        btnAttach      = findViewById(R.id.btnAttach);
        btnVideoCall   = findViewById(R.id.btnVideoCall);

        adapter = new MessageAdapter(messageList, currentUserId, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btnSend.setImageResource(android.R.drawable.ic_btn_speak_now);

        etMessage.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int b, int c) {
                btnSend.setImageResource(s.toString().trim().length() > 0
                        ? android.R.drawable.ic_menu_send
                        : android.R.drawable.ic_btn_speak_now);
            }
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
        });

        btnAttach.setOnClickListener(v -> showAttachmentMenu());

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    // ── Video Call ──────────────────────────────────────────────────────────

    private void setupVideoCallButton() {
        if (btnVideoCall == null) return;
        btnVideoCall.setOnClickListener(v -> checkCallPermissionsAndStart());
    }

    private void checkCallPermissionsAndStart() {
        boolean hasCam = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED;
        boolean hasMic = ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED;
        if (hasCam && hasMic) {
            startVideoCall();
        } else {
            callPermLauncher.launch(new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO});
        }
    }

    private void startVideoCall() {
        if (otherUserId == null) return;

        String callId = (currentUserId.compareTo(otherUserId) < 0)
                ? currentUserId + "_" + otherUserId
                : otherUserId + "_" + currentUserId;

        TextView tvUserName = findViewById(R.id.tvUserName);
        String calleeName = tvUserName != null ? tvUserName.getText().toString() : "";

        // Документ для получателя
        Map<String, Object> callDataReceiver = new HashMap<>();
        callDataReceiver.put("callId",     callId);
        callDataReceiver.put("callerId",   currentUserId);
        callDataReceiver.put("callerName", currentUserName);
        callDataReceiver.put("status",     "calling");

        // Документ для себя — слушаем статус accepted/declined
        Map<String, Object> callDataSelf = new HashMap<>();
        callDataSelf.put("callId",      callId);
        callDataSelf.put("otherUserId", otherUserId);
        callDataSelf.put("status",      "calling");

        db.collection("Calls").document(otherUserId).set(callDataReceiver);
        db.collection("Calls").document(currentUserId)
                .set(callDataSelf)
                .addOnSuccessListener(v -> {
                    Intent intent = new Intent(this, OutgoingCallActivity.class);
                    intent.putExtra("callId",      callId);
                    intent.putExtra("currentUid",  currentUserId);
                    intent.putExtra("otherUserId", otherUserId);
                    intent.putExtra("calleeName",  calleeName);
                    startActivity(intent);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not start call", Toast.LENGTH_SHORT).show());
    }

    // ── Other User Name ─────────────────────────────────────────────────────

    private void loadOtherUserName() {
        if (otherUserId == null) return;
        db.collection("Users").document(otherUserId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String name    = doc.getString("name");
                String surname = doc.getString("surname");
                TextView tvUserName = findViewById(R.id.tvUserName);
                if (tvUserName != null) tvUserName.setText(name + " " + surname);
            }
        });
    }

    // ── Send Button ─────────────────────────────────────────────────────────

    private void setupSendButton() {
        btnSend.setOnClickListener(v -> {
            String txt = etMessage.getText().toString().trim();
            if (txt.isEmpty()) {
                Toast.makeText(this, "Hold for Voice", Toast.LENGTH_SHORT).show();
            } else {
                if (editingId != null) commitEdit(txt);
                else sendTextMessage(txt);
            }
        });
    }

    // ── Attachment Menu ─────────────────────────────────────────────────────

    private void showAttachmentMenu() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_attachment_picker, null);

        view.findViewById(R.id.itemGallery).setOnClickListener(v -> {
            dialog.dismiss();
            checkStoragePermAndOpenGallery();
        });
        view.findViewById(R.id.itemCamera).setOnClickListener(v -> {
            dialog.dismiss();
            checkCameraPermAndOpen();
        });
        view.findViewById(R.id.itemDocument).setOnClickListener(v -> {
            dialog.dismiss();
            openDocumentPicker();
        });

        dialog.setContentView(view);
        dialog.show();
    }

    // ── Gallery ─────────────────────────────────────────────────────────────

    private void checkStoragePermAndOpenGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            openGallery();
        } else if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            storagePermLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    // ── Camera ──────────────────────────────────────────────────────────────

    private void checkCameraPermAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            cameraPermLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File photoFile = createImageFile();
            cameraImageUri = FileProvider.getUriForFile(
                    this, getPackageName() + ".provider", photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            cameraLauncher.launch(intent);
        } catch (IOException e) {
            Log.e(TAG, "Camera file error", e);
            Toast.makeText(this, "Could not open camera", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String ts  = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File dir   = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("IMG_" + ts + "_", ".jpg", dir);
    }

    // ── Document Picker ─────────────────────────────────────────────────────

    private void openDocumentPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        documentLauncher.launch(intent);
    }

    // ── Cloudinary Upload ───────────────────────────────────────────────────

    private void uploadToCloudinary(Uri uri, String messageType) {
        Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show();
        String fileName = getFileName(uri);

        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                if (is == null) throw new IOException("Cannot open stream");
                byte[] fileBytes = is.readAllBytes();
                is.close();

                String boundary = "Boundary" + System.currentTimeMillis();
                HttpURLConnection conn = (HttpURLConnection) new URL(UPLOAD_URL).openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                conn.setConnectTimeout(30_000);
                conn.setReadTimeout(60_000);

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n");
                dos.writeBytes(UPLOAD_PRESET + "\r\n");
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\""
                        + fileName + "\"\r\n");
                dos.writeBytes("Content-Type: application/octet-stream\r\n\r\n");
                dos.write(fileBytes);
                dos.writeBytes("\r\n--" + boundary + "--\r\n");
                dos.flush();
                dos.close();

                int code = conn.getResponseCode();
                InputStream resp = (code == 200) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(resp));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                conn.disconnect();

                if (code == 200) {
                    String fileUrl = new JSONObject(sb.toString()).getString("secure_url");
                    runOnUiThread(() -> sendFileMessage(fileUrl, fileName, messageType));
                } else {
                    Log.e(TAG, "Cloudinary error " + code + ": " + sb);
                    runOnUiThread(() ->
                            Toast.makeText(this, "Upload failed (" + code + ")", Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                Log.e(TAG, "Upload error", e);
                runOnUiThread(() ->
                        Toast.makeText(this, "Upload error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor c = getContentResolver().query(
                    uri, null, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    int idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) result = c.getString(idx);
                }
            }
        }
        if (result == null) {
            String path = uri.getPath();
            if (path != null) {
                int cut = path.lastIndexOf('/');
                result = (cut != -1) ? path.substring(cut + 1) : path;
            }
        }
        return result != null ? result : "file_" + System.currentTimeMillis();
    }

    // ── Messaging ───────────────────────────────────────────────────────────

    private void sendTextMessage(String text) {
        DocumentReference ref = db.collection("Chats").document(chatId)
                .collection("Messages").document();
        Message msg = new Message(ref.getId(), currentUserId, text,
                System.currentTimeMillis(), 0, "text", false);
        if (replyMessage != null) msg.replyToText = replyMessage.text;

        ref.set(msg).addOnSuccessListener(v -> {
            etMessage.setText("");
            cancelReply();
            ref.update("status", 1);
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Send failed", Toast.LENGTH_SHORT).show());
    }

    private void sendFileMessage(String fileUrl, String fileName, String type) {
        DocumentReference ref = db.collection("Chats").document(chatId)
                .collection("Messages").document();
        Message msg = new Message(ref.getId(), currentUserId, fileUrl,
                System.currentTimeMillis(), 0, type, false);
        msg.fileName = fileName;
        if (replyMessage != null) msg.replyToText = replyMessage.text;

        ref.set(msg).addOnSuccessListener(v -> {
            cancelReply();
            ref.update("status", 1);
            Toast.makeText(this, "Sent!", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Send failed", Toast.LENGTH_SHORT).show());
    }

    private void loadMessages() {
        chatListener = db.collection("Chats").document(chatId)
                .collection("Messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, e) -> {
                    if (e != null) { Log.e(TAG, "Listen failed", e); return; }
                    if (value != null) {
                        messageList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Message msg = doc.toObject(Message.class);
                            if (msg != null) {
                                messageList.add(msg);
                                if (!msg.senderId.equals(currentUserId) && msg.status < 2)
                                    doc.getReference().update("status", 2);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        if (!messageList.isEmpty())
                            recyclerView.scrollToPosition(messageList.size() - 1);
                    }
                });
    }

    // ── Swipe to Reply ──────────────────────────────────────────────────────

    private void setupSwipeReply() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override public boolean onMove(@NonNull RecyclerView rv,
                                            @NonNull RecyclerView.ViewHolder vh,
                                            @NonNull RecyclerView.ViewHolder t) { return false; }
            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {
                int pos = vh.getAdapterPosition();
                Message m = messageList.get(pos);
                if (!"deleted".equals(m.type)) onReply(m);
                adapter.notifyItemChanged(pos);
            }
        }).attachToRecyclerView(recyclerView);
    }

    // ── MessageActionListener ───────────────────────────────────────────────

    @Override public void onReply(Message m) {
        replyMessage = m;
        layoutReply.setVisibility(View.VISIBLE);
        tvReplyPreview.setText(
                ("image".equals(m.type) || "document".equals(m.type))
                        ? "📎 " + (m.fileName != null ? m.fileName : "Attachment")
                        : m.text);
    }

    public void cancelReply(View v) { cancelReply(); }
    private void cancelReply() {
        replyMessage = null;
        layoutReply.setVisibility(View.GONE);
    }

    @Override public void onEdit(Message m) {
        if (!"text".equals(m.type)) {
            Toast.makeText(this, "Cannot edit attachments", Toast.LENGTH_SHORT).show();
            return;
        }
        editingId = m.messageId;
        etMessage.setText(m.text);
        etMessage.requestFocus();
    }

    private void commitEdit(String t) {
        db.collection("Chats").document(chatId)
                .collection("Messages").document(editingId)
                .update("text", t, "edited", true)
                .addOnSuccessListener(v -> { editingId = null; etMessage.setText(""); });
    }

    @Override public void onDelete(Message m) {
        db.collection("Chats").document(chatId)
                .collection("Messages").document(m.messageId)
                .update("type", "deleted", "text", "Message deleted", "replyToText", null);
    }
}
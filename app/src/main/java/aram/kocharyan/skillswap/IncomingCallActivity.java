package aram.kocharyan.skillswap;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class IncomingCallActivity extends AppCompatActivity {

    private static final int PERMISSION_REQ = 33;
    private static final String[] PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };

    private FirebaseFirestore db;
    private String currentUserId, callerId, callId;
    private ListenerRegistration callListener;
    private Ringtone ringtone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call);

        callId    = getIntent().getStringExtra("callId");
        callerId  = getIntent().getStringExtra("callerId");
        String callerName = getIntent().getStringExtra("callerName");

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        TextView tvCallerName = findViewById(R.id.tvCallerName);
        if (callerName != null) tvCallerName.setText(callerName);

        findViewById(R.id.btnAccept).setOnClickListener(v -> {
            if (checkPermissions()) acceptCall();
            else ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQ);
        });

        findViewById(R.id.btnDecline).setOnClickListener(v -> declineCall());

        startRingtone();
        listenForCallCancellation();
    }

    // ── Ringtone ────────────────────────────────────────────────────────────

    private void startRingtone() {
        try {
            Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            ringtone = RingtoneManager.getRingtone(getApplicationContext(), ringtoneUri);
            if (ringtone != null) {
                ringtone.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
                ringtone.play();
            }
        } catch (Exception e) {
            // Тихо если не удалось запустить рингтон
        }
    }

    private void stopRingtone() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
        ringtone = null;
    }

    // ── Permissions ─────────────────────────────────────────────────────────

    private boolean checkPermissions() {
        for (String perm : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, perm)
                    != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ) {
            boolean allGranted = true;
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) { allGranted = false; break; }
            }
            if (allGranted) acceptCall();
        }
    }

    // ── Accept / Decline ────────────────────────────────────────────────────

    private void acceptCall() {
        stopRingtone();
        if (callerId != null) {
            db.collection("Calls").document(callerId)
                    .update("status", "accepted")
                    .addOnSuccessListener(v -> openVideoCall())
                    .addOnFailureListener(e -> openVideoCall());
        } else {
            openVideoCall();
        }
    }

    private void openVideoCall() {
        if (callListener != null) { callListener.remove(); callListener = null; }
        Intent intent = new Intent(this, VideoCallActivity.class);
        intent.putExtra("callId",      callId);
        intent.putExtra("currentUid",  currentUserId);
        intent.putExtra("otherUserId", callerId);
        startActivity(intent);
        finish();
    }

    private void declineCall() {
        stopRingtone();
        db.collection("Calls").document(currentUserId).delete();
        if (callerId != null)
            db.collection("Calls").document(callerId).update("status", "declined");
        finish();
    }

    private void listenForCallCancellation() {
        callListener = db.collection("Calls").document(currentUserId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) return;
                    if (snapshot == null || !snapshot.exists()) {
                        stopRingtone();
                        finish();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRingtone();
        if (callListener != null) callListener.remove();
    }
}
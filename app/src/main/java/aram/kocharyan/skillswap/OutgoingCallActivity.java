package aram.kocharyan.skillswap;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class OutgoingCallActivity extends AppCompatActivity {

    private static final int PERMISSION_REQ = 44;
    private static final int TIMEOUT_MS     = 24000;
    private static final String[] PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };

    private FirebaseFirestore db;
    private String callId, currentUserId, otherUserId;
    private ListenerRegistration statusListener;
    private final Handler timeoutHandler = new Handler();
    private boolean callEnded = false;

    // Гудки
    private ToneGenerator toneGenerator;
    private final Handler dialToneHandler = new Handler();
    private boolean dialToneRunning = false;

    // Таймаут 24 секунды
    private final Runnable timeoutRunnable = () -> {
        if (!callEnded) {
            callEnded = true;
            cancelCall();
        }
    };

    // Гудок каждые 3 секунды: 1 сек играет, 2 сек пауза
    private final Runnable dialToneRunnable = new Runnable() {
        @Override
        public void run() {
            if (!dialToneRunning) return;
            try {
                if (toneGenerator != null)
                    toneGenerator.startTone(ToneGenerator.TONE_SUP_DIAL, 1000);
            } catch (Exception ignored) {}
            dialToneHandler.postDelayed(this, 3000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_outgoing_call);

        callId        = getIntent().getStringExtra("callId");
        currentUserId = getIntent().getStringExtra("currentUid");
        otherUserId   = getIntent().getStringExtra("otherUserId");
        String calleeName = getIntent().getStringExtra("calleeName");

        db = FirebaseFirestore.getInstance();

        TextView tvCalleeName = findViewById(R.id.tvCalleeName);
        if (calleeName != null) tvCalleeName.setText(calleeName);

        findViewById(R.id.btnCancelCall).setOnClickListener(v -> {
            callEnded = true;
            cancelCall();
        });

        if (!checkPermissions()) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQ);
        }

        startDialTone();
        timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT_MS);
        listenForCallStatus();
    }

    // ── Dial Tone ───────────────────────────────────────────────────────────

    private void startDialTone() {
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80);
            dialToneRunning = true;
            dialToneHandler.post(dialToneRunnable);
        } catch (Exception ignored) {}
    }

    private void stopDialTone() {
        dialToneRunning = false;
        dialToneHandler.removeCallbacks(dialToneRunnable);
        try {
            if (toneGenerator != null) {
                toneGenerator.stopTone();
                toneGenerator.release();
                toneGenerator = null;
            }
        } catch (Exception ignored) {}
    }

    // ── Permissions ─────────────────────────────────────────────────────────

    private boolean checkPermissions() {
        for (String perm : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, perm)
                    != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }

    // ── Status Listener ─────────────────────────────────────────────────────

    private void listenForCallStatus() {
        statusListener = db.collection("Calls").document(currentUserId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || callEnded) return;

                    if (snapshot == null || !snapshot.exists()) {
                        callEnded = true;
                        stopDialTone();
                        timeoutHandler.removeCallbacks(timeoutRunnable);
                        finish();
                        return;
                    }

                    String status = snapshot.getString("status");

                    if ("accepted".equals(status)) {
                        callEnded = true;
                        stopDialTone();
                        timeoutHandler.removeCallbacks(timeoutRunnable);
                        if (statusListener != null) { statusListener.remove(); statusListener = null; }
                        openVideoCall();
                    } else if ("declined".equals(status)) {
                        callEnded = true;
                        stopDialTone();
                        timeoutHandler.removeCallbacks(timeoutRunnable);
                        db.collection("Calls").document(currentUserId).delete();
                        finish();
                    }
                });
    }

    private void openVideoCall() {
        Intent intent = new Intent(this, VideoCallActivity.class);
        intent.putExtra("callId",      callId);
        intent.putExtra("currentUid",  currentUserId);
        intent.putExtra("otherUserId", otherUserId);
        startActivity(intent);
        finish();
    }

    private void cancelCall() {
        stopDialTone();
        timeoutHandler.removeCallbacks(timeoutRunnable);
        if (otherUserId != null)
            db.collection("Calls").document(otherUserId).delete();
        if (currentUserId != null)
            db.collection("Calls").document(currentUserId).delete();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopDialTone();
        timeoutHandler.removeCallbacks(timeoutRunnable);
        if (statusListener != null) statusListener.remove();
    }
}
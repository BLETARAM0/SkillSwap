package aram.kocharyan.skillswap;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class OutgoingCallActivity extends AppCompatActivity {

    private static final int PERMISSION_REQ  = 44;
    private static final int TIMEOUT_MS      = 24000; // 24 секунды
    private static final String[] PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };

    private FirebaseFirestore db;
    private String callId, currentUserId, otherUserId;
    private ListenerRegistration statusListener;
    private final Handler timeoutHandler = new Handler();
    private boolean callEnded = false;

    // Runnable который сработает через 24 секунды
    private final Runnable timeoutRunnable = () -> {
        if (!callEnded) {
            callEnded = true;
            cancelCall();
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

        // Запрашиваем разрешения заранее
        if (!checkPermissions()) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQ);
        }

        // Запускаем таймаут 24 секунды
        timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT_MS);

        listenForCallStatus();
    }

    private boolean checkPermissions() {
        for (String perm : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, perm)
                    != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }

    private void listenForCallStatus() {
        statusListener = db.collection("Calls").document(currentUserId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || callEnded) return;

                    if (snapshot == null || !snapshot.exists()) {
                        callEnded = true;
                        timeoutHandler.removeCallbacks(timeoutRunnable);
                        finish();
                        return;
                    }

                    String status = snapshot.getString("status");

                    if ("accepted".equals(status)) {
                        callEnded = true;
                        timeoutHandler.removeCallbacks(timeoutRunnable);
                        if (statusListener != null) {
                            statusListener.remove();
                            statusListener = null;
                        }
                        openVideoCall();
                    } else if ("declined".equals(status)) {
                        callEnded = true;
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
        timeoutHandler.removeCallbacks(timeoutRunnable);
        if (statusListener != null) statusListener.remove();
    }
}
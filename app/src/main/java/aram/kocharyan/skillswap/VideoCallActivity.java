package aram.kocharyan.skillswap;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;

public class VideoCallActivity extends AppCompatActivity {

    private static final String APP_ID = "aca32e3e2d67495fb9834ab98aa91b94";
    private static final int PERMISSION_REQ = 22;
    private static final String[] PERMISSIONS = {
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO
    };

    // UI
    private FrameLayout flRemoteVideo, flLocalVideo;
    private ImageButton btnMute, btnCamera, btnEndCall;

    // Agora
    private RtcEngine rtcEngine;
    private boolean isMuted    = false;
    private boolean isCameraOn = true;
    private int localUid;

    // Data
    private String channelId, currentUserId, otherUserId;
    private FirebaseFirestore db;
    private ListenerRegistration callEndListener;

    // ── Agora event handler ─────────────────────────────────────────────────

    private final IRtcEngineEventHandler eventHandler = new IRtcEngineEventHandler() {

        @Override
        public void onUserJoined(int uid, int elapsed) {
            android.util.Log.d("AGORA", "onUserJoined uid=" + uid);
            runOnUiThread(() -> {
                playConnectSound();
                setupRemoteVideo(uid);
            });
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            android.util.Log.d("AGORA", "onUserOffline uid=" + uid);
            runOnUiThread(() -> {
                playDisconnectSound();
                flRemoteVideo.removeAllViews();
                flRemoteVideo.setBackgroundColor(Color.BLACK);
                Toast.makeText(VideoCallActivity.this, "Call ended", Toast.LENGTH_SHORT).show();
                // Небольшая задержка чтобы звук успел проиграть
                new android.os.Handler().postDelayed(() -> cleanupAndFinish(), 1000);
            });
        }

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            android.util.Log.d("AGORA", "onJoinChannelSuccess uid=" + uid);
        }

        @Override
        public void onRemoteVideoStateChanged(int uid, int state, int reason, int elapsed) {
            runOnUiThread(() -> {
                if (state == Constants.REMOTE_VIDEO_STATE_DECODING) {
                    setupRemoteVideo(uid);
                } else if (state == Constants.REMOTE_VIDEO_STATE_STOPPED
                        || state == Constants.REMOTE_VIDEO_STATE_FROZEN) {
                    flRemoteVideo.removeAllViews();
                    flRemoteVideo.setBackgroundColor(Color.BLACK);
                }
            });
        }

        @Override
        public void onError(int err) {
            android.util.Log.e("AGORA", "onError err=" + err);
        }
    };

    // ── Sounds ──────────────────────────────────────────────────────────────

    private void playConnectSound() {
        try {
            ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80);
            // Два коротких бипа — сигнал подключения
            tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 400);
            new android.os.Handler().postDelayed(tg::release, 600);
        } catch (Exception ignored) {}
    }

    private void playDisconnectSound() {
        try {
            ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80);
            // Один длинный низкий бип — сигнал отключения
            tg.startTone(ToneGenerator.TONE_SUP_BUSY, 800);
            new android.os.Handler().postDelayed(tg::release, 1000);
        } catch (Exception ignored) {}
    }

    // ── Lifecycle ───────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);

        channelId     = getIntent().getStringExtra("callId");
        currentUserId = getIntent().getStringExtra("currentUid");
        otherUserId   = getIntent().getStringExtra("otherUserId");
        db            = FirebaseFirestore.getInstance();

        localUid = Math.abs(currentUserId.hashCode()) % 100000 + 1;

        flRemoteVideo = findViewById(R.id.flRemoteVideo);
        flLocalVideo  = findViewById(R.id.flLocalVideo);
        btnMute       = findViewById(R.id.btnMute);
        btnCamera     = findViewById(R.id.btnCamera);
        btnEndCall    = findViewById(R.id.btnEndCall);

        flRemoteVideo.setBackgroundColor(Color.BLACK);

        btnEndCall.setOnClickListener(v -> endCall());

        btnMute.setOnClickListener(v -> {
            isMuted = !isMuted;
            rtcEngine.muteLocalAudioStream(isMuted);
            btnMute.setImageResource(isMuted
                    ? android.R.drawable.ic_lock_silent_mode
                    : android.R.drawable.ic_lock_silent_mode_off);
        });

        btnCamera.setOnClickListener(v -> {
            isCameraOn = !isCameraOn;
            rtcEngine.muteLocalVideoStream(!isCameraOn);
            flLocalVideo.setVisibility(isCameraOn ? View.VISIBLE : View.INVISIBLE);
            btnCamera.setImageResource(isCameraOn
                    ? android.R.drawable.ic_menu_camera
                    : android.R.drawable.ic_menu_close_clear_cancel);
        });

        listenForCallEnd();

        if (checkPermissions()) {
            initAgora();
        } else {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQ);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (callEndListener != null) callEndListener.remove();
        if (rtcEngine != null) {
            rtcEngine.leaveChannel();
            RtcEngine.destroy();
            rtcEngine = null;
        }
    }

    // ── Call End Listener ───────────────────────────────────────────────────

    private void listenForCallEnd() {
        if (currentUserId == null) return;
        callEndListener = db.collection("Calls").document(currentUserId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) return;
                    if (snapshot == null || !snapshot.exists()) {
                        runOnUiThread(this::cleanupAndFinish);
                    }
                });
    }

    // ── End Call ────────────────────────────────────────────────────────────

    private void endCall() {
        playDisconnectSound();
        if (currentUserId != null)
            db.collection("Calls").document(currentUserId).delete();
        if (otherUserId != null)
            db.collection("Calls").document(otherUserId).delete();
        new android.os.Handler().postDelayed(this::cleanupAndFinish, 600);
    }

    private void cleanupAndFinish() {
        if (callEndListener != null) {
            callEndListener.remove();
            callEndListener = null;
        }
        if (rtcEngine != null) {
            rtcEngine.leaveChannel();
            RtcEngine.destroy();
            rtcEngine = null;
        }
        finish();
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
            if (allGranted) initAgora();
            else {
                Toast.makeText(this, "Camera and microphone permission required",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    // ── Agora Init ──────────────────────────────────────────────────────────

    private void initAgora() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext      = getApplicationContext();
            config.mAppId        = APP_ID;
            config.mEventHandler = eventHandler;
            rtcEngine = RtcEngine.create(config);
        } catch (Exception e) {
            Toast.makeText(this, "Init failed: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        rtcEngine.enableVideo();
        rtcEngine.enableAudio();
        rtcEngine.setAudioProfile(
                Constants.AUDIO_PROFILE_DEFAULT,
                Constants.AUDIO_SCENARIO_GAME_STREAMING
        );
        rtcEngine.setEnableSpeakerphone(true);
        rtcEngine.muteLocalVideoStream(false);
        rtcEngine.muteLocalAudioStream(false);

        setupLocalVideo();
        joinChannel();
    }

    private void setupLocalVideo() {
        SurfaceView localView = new SurfaceView(this);
        localView.setZOrderMediaOverlay(true);
        flLocalVideo.removeAllViews();
        flLocalVideo.addView(localView);
        rtcEngine.setupLocalVideo(new VideoCanvas(localView,
                VideoCanvas.RENDER_MODE_HIDDEN, localUid));
        rtcEngine.startPreview();
    }

    private void setupRemoteVideo(int uid) {
        if (uid == localUid) return;
        SurfaceView remoteView = new SurfaceView(this);
        flRemoteVideo.removeAllViews();
        flRemoteVideo.setBackgroundColor(Color.TRANSPARENT);
        flRemoteVideo.addView(remoteView);
        rtcEngine.setupRemoteVideo(new VideoCanvas(remoteView,
                VideoCanvas.RENDER_MODE_HIDDEN, uid));
    }

    private void joinChannel() {
        ChannelMediaOptions options = new ChannelMediaOptions();
        options.clientRoleType         = Constants.CLIENT_ROLE_BROADCASTER;
        options.channelProfile         = Constants.CHANNEL_PROFILE_COMMUNICATION;
        options.publishCameraTrack     = true;
        options.publishMicrophoneTrack = true;
        options.autoSubscribeAudio     = true;
        options.autoSubscribeVideo     = true;
        rtcEngine.joinChannel(null, channelId, localUid, options);
    }
}
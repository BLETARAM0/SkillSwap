package aram.kocharyan.skillswap;

import android.animation.ObjectAnimator;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
    private static final int MARGIN_DP = 16;

    // UI
    private FrameLayout flRemoteVideo, flLocalVideo;
    private LinearLayout controlsPanel;
    private ImageButton btnMute, btnCamera, btnEndCall, btnFlipCamera;
    private boolean controlsVisible = true;

    // Drag state
    private float dragStartX, dragStartY;
    private float viewStartX, viewStartY;
    private boolean isDragging = false;

    // Swap state — false = локальное в flLocalVideo (маленькое), remote в flRemoteVideo (большое)
    private boolean isSwapped = false;
    private int remoteUid = -1;

    // Camera state
    private boolean isFrontCamera = true;

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
            runOnUiThread(() -> {
                remoteUid = uid;
                playConnectSound();
                renderRemoteVideo(uid);
            });
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            runOnUiThread(() -> {
                remoteUid = -1;
                playDisconnectSound();
                getRemoteContainer().removeAllViews();
                getRemoteContainer().setBackgroundColor(Color.BLACK);
                Toast.makeText(VideoCallActivity.this, "Call ended", Toast.LENGTH_SHORT).show();
                new Handler().postDelayed(() -> cleanupAndFinish(), 1000);
            });
        }

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            android.util.Log.d("AGORA", "joined uid=" + uid);
        }

        @Override
        public void onRemoteVideoStateChanged(int uid, int state, int reason, int elapsed) {
            runOnUiThread(() -> {
                if (state == Constants.REMOTE_VIDEO_STATE_DECODING) {
                    remoteUid = uid;
                    renderRemoteVideo(uid);
                } else if (state == Constants.REMOTE_VIDEO_STATE_STOPPED
                        || state == Constants.REMOTE_VIDEO_STATE_FROZEN) {
                    getRemoteContainer().removeAllViews();
                    getRemoteContainer().setBackgroundColor(Color.BLACK);
                }
            });
        }

        @Override
        public void onError(int err) {
            android.util.Log.e("AGORA", "err=" + err);
        }
    };

    // ── Helpers — какой контейнер локальный/удалённый ───────────────────────

    /** Контейнер где сейчас показывается локальное видео */
    private FrameLayout getLocalContainer() {
        return isSwapped ? flRemoteVideo : flLocalVideo;
    }

    /** Контейнер где сейчас показывается удалённое видео */
    private FrameLayout getRemoteContainer() {
        return isSwapped ? flLocalVideo : flRemoteVideo;
    }

    // ── Sounds ──────────────────────────────────────────────────────────────

    private void playConnectSound() {
        try {
            ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80);
            tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 400);
            new Handler().postDelayed(tg::release, 600);
        } catch (Exception ignored) {}
    }

    private void playDisconnectSound() {
        try {
            ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80);
            tg.startTone(ToneGenerator.TONE_SUP_BUSY, 800);
            new Handler().postDelayed(tg::release, 1000);
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

        flRemoteVideo  = findViewById(R.id.flRemoteVideo);
        flLocalVideo   = findViewById(R.id.flLocalVideo);
        controlsPanel  = findViewById(R.id.controlsPanel);
        btnMute        = findViewById(R.id.btnMute);
        btnCamera      = findViewById(R.id.btnCamera);
        btnEndCall     = findViewById(R.id.btnEndCall);
        btnFlipCamera  = findViewById(R.id.btnFlipCamera);

        flRemoteVideo.setBackgroundColor(Color.BLACK);

        // Клик на большой экран — скрыть/показать панель
        flRemoteVideo.setOnClickListener(v -> toggleControls());

        // Перетаскивание + клик на маленькое окошко
        setupDraggable();

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
            getLocalContainer().setVisibility(isCameraOn ? View.VISIBLE : View.INVISIBLE);
            btnCamera.setImageResource(isCameraOn
                    ? android.R.drawable.ic_menu_camera
                    : android.R.drawable.ic_menu_close_clear_cancel);
        });

        // Переворот камеры
        btnFlipCamera.setOnClickListener(v -> flipCamera());

        listenForCallEnd();

        if (checkPermissions()) initAgora();
        else ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQ);
    }

    // ── Flip Camera ─────────────────────────────────────────────────────────

    private void flipCamera() {
        if (rtcEngine == null) return;
        isFrontCamera = !isFrontCamera;
        rtcEngine.switchCamera();

        // Анимация вращения иконки
        btnFlipCamera.animate().rotationBy(180f).setDuration(300).start();
    }

    // ── Toggle Controls ─────────────────────────────────────────────────────

    private void toggleControls() {
        controlsVisible = !controlsVisible;
        controlsPanel.animate()
                .alpha(controlsVisible ? 1f : 0f)
                .setDuration(250)
                .withEndAction(() -> {
                    if (!controlsVisible)
                        controlsPanel.setVisibility(View.INVISIBLE);
                })
                .start();
        if (controlsVisible) controlsPanel.setVisibility(View.VISIBLE);
    }

    // ── Swap Videos ─────────────────────────────────────────────────────────

    private void swapVideos() {
        isSwapped = !isSwapped;

        // Локальное видео — всегда в getLocalContainer()
        SurfaceView localView = new SurfaceView(this);
        boolean localIsSmall = !isSwapped; // маленькое когда НЕ свапнуто
        if (localIsSmall) localView.setZOrderMediaOverlay(true);
        FrameLayout localContainer = getLocalContainer();
        localContainer.removeAllViews();
        localContainer.addView(localView);
        rtcEngine.setupLocalVideo(new VideoCanvas(localView,
                VideoCanvas.RENDER_MODE_HIDDEN, localUid));

        // Удалённое видео — всегда в getRemoteContainer()
        if (remoteUid != -1) {
            renderRemoteVideo(remoteUid);
        }

        // Клик на маленькое окошко (всегда flLocalVideo физически)
        // — но теперь swap работает в обе стороны
    }

    // ── Render Videos ───────────────────────────────────────────────────────

    private void renderRemoteVideo(int uid) {
        if (uid == localUid) return;
        FrameLayout container = getRemoteContainer();
        SurfaceView remoteView = new SurfaceView(this);
        container.removeAllViews();
        container.setBackgroundColor(Color.TRANSPARENT);
        container.addView(remoteView);
        rtcEngine.setupRemoteVideo(new VideoCanvas(remoteView,
                VideoCanvas.RENDER_MODE_HIDDEN, uid));
    }

    // ── Drag + Snap ─────────────────────────────────────────────────────────

    private void setupDraggable() {
        flLocalVideo.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    dragStartX = event.getRawX();
                    dragStartY = event.getRawY();
                    viewStartX = v.getX();
                    viewStartY = v.getY();
                    isDragging = false;
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - dragStartX;
                    float dy = event.getRawY() - dragStartY;
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) isDragging = true;
                    if (isDragging) {
                        v.setX(viewStartX + dx);
                        v.setY(viewStartY + dy);
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    if (isDragging) {
                        snapToCorner(v);
                    } else {
                        // Клик — всегда свапаем в обе стороны
                        swapVideos();
                    }
                    isDragging = false;
                    return true;
            }
            return false;
        });
    }

    private void snapToCorner(View v) {
        int screenW = getResources().getDisplayMetrics().widthPixels;
        int screenH = getResources().getDisplayMetrics().heightPixels;
        int margin  = (int) (MARGIN_DP * getResources().getDisplayMetrics().density);

        float centerX = v.getX() + v.getWidth() / 2f;
        float centerY = v.getY() + v.getHeight() / 2f;

        float targetX = (centerX < screenW / 2f) ? margin : screenW - v.getWidth() - margin;
        float targetY = (centerY < screenH / 2f) ? margin : screenH - v.getHeight() - margin;

        ObjectAnimator animX = ObjectAnimator.ofFloat(v, "x", v.getX(), targetX);
        animX.setDuration(250);
        animX.setInterpolator(new DecelerateInterpolator());
        animX.start();

        ObjectAnimator animY = ObjectAnimator.ofFloat(v, "y", v.getY(), targetY);
        animY.setDuration(250);
        animY.setInterpolator(new DecelerateInterpolator());
        animY.start();
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
        new Handler().postDelayed(this::cleanupAndFinish, 600);
    }

    private void cleanupAndFinish() {
        if (callEndListener != null) { callEndListener.remove(); callEndListener = null; }
        if (rtcEngine != null) {
            rtcEngine.leaveChannel();
            RtcEngine.destroy();
            rtcEngine = null;
        }
        finish();
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
            else { Toast.makeText(this, "Permissions required", Toast.LENGTH_SHORT).show(); finish(); }
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
            Toast.makeText(this, "Init failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        rtcEngine.enableVideo();
        rtcEngine.enableAudio();
        rtcEngine.setAudioProfile(Constants.AUDIO_PROFILE_DEFAULT,
                Constants.AUDIO_SCENARIO_GAME_STREAMING);
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
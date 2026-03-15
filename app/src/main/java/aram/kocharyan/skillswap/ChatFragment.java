package aram.kocharyan.skillswap;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.jitsi.meet.sdk.JitsiMeet;
import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;

import java.net.MalformedURLException;
import java.net.URL;

public class ChatFragment extends Fragment {

    private Button btnVideoCall;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        btnVideoCall = view.findViewById(R.id.btnVideoCall);

        // Инициализация Jitsi (без удалённой строки)
        try {
            JitsiMeetConferenceOptions defaultOptions = new JitsiMeetConferenceOptions.Builder()
                    .setServerURL(new URL("https://meet.jit.si"))
                    .build();

            JitsiMeet.setDefaultConferenceOptions(defaultOptions);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Проверяем режим пользователя
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    String mode = document.getString("mode");

                    if ("online".equals(mode)) {
                        btnVideoCall.setVisibility(View.VISIBLE);
                        btnVideoCall.setOnClickListener(v -> startVideoCall());
                    } else {
                        btnVideoCall.setVisibility(View.GONE);
                    }
                });

        return view;
    }

    private void startVideoCall() {
        String roomName = "skillswap_call_" + System.currentTimeMillis();

        JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
                .setRoom(roomName)
                .setAudioMuted(false)
                .setVideoMuted(false)
                .build();

        JitsiMeetActivity.launch(requireContext(), options);
    }
}
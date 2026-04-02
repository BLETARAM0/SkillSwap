package aram.kocharyan.skillswap;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.jitsi.meet.sdk.JitsiMeet;
import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment implements RequestAdapter.OnRequestActionListener {

    private Button btnVideoCall;
    private RecyclerView recyclerRequests;
    private RequestAdapter requestAdapter;
    private List<ChatRequest> requestList = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        btnVideoCall = view.findViewById(R.id.btnVideoCall);
        recyclerRequests = view.findViewById(R.id.recyclerRequests);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        recyclerRequests.setLayoutManager(new LinearLayoutManager(requireContext()));
        requestAdapter = new RequestAdapter(requestList, this);
        recyclerRequests.setAdapter(requestAdapter);

        initJitsi();
        checkUserMode();
        loadChatRequests();

        return view;
    }

    private void initJitsi() {
        try {
            JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
                    .setServerURL(new URL("https://meet.jit.si"))
                    .build();
            JitsiMeet.setDefaultConferenceOptions(options);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private void checkUserMode() {
        db.collection("Users").document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    String mode = doc.getString("mode");
                    btnVideoCall.setVisibility("online".equals(mode) ? View.VISIBLE : View.GONE);
                    if ("online".equals(mode)) {
                        btnVideoCall.setOnClickListener(v -> startVideoCall());
                    }
                });
    }

    private void loadChatRequests() {
        db.collection("ChatRequests")
                .whereEqualTo("toUserId", currentUserId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(query -> {
                    requestList.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        ChatRequest req = doc.toObject(ChatRequest.class);
                        if (req != null) requestList.add(req);
                    }
                    requestAdapter.notifyDataSetChanged();
                });
    }

    private void startVideoCall() {
        String roomName = "skillswap_call_" + System.currentTimeMillis();
        JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
                .setRoom(roomName)
                .build();
        JitsiMeetActivity.launch(requireContext(), options);
    }

    // Обработка принятия / отклонения запроса
    @Override
    public void onAction(ChatRequest request, boolean accept) {
        String status = accept ? "accepted" : "rejected";

        db.collection("ChatRequests").document(request.requestId)
                .update("status", status)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), accept ? "Chat started!" : "Request rejected", Toast.LENGTH_SHORT).show();
                    loadChatRequests();
                });
    }
}
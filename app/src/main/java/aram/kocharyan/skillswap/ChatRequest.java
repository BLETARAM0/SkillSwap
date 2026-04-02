package aram.kocharyan.skillswap;

public class ChatRequest {

    public String requestId;
    public String fromUserId;
    public String fromName;
    public String toUserId;
    public String status;        // "pending", "accepted", "rejected"
    public long timestamp;

    public ChatRequest() {} // нужен для Firestore

    public ChatRequest(String requestId, String fromUserId, String fromName, String toUserId, String status, long timestamp) {
        this.requestId = requestId;
        this.fromUserId = fromUserId;
        this.fromName = fromName;
        this.toUserId = toUserId;
        this.status = status;
        this.timestamp = timestamp;
    }
}
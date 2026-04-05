package aram.kocharyan.skillswap;

public class Message {
    public String messageId;
    public String senderId;
    public String text;
    public long timestamp;

    public Message() {}

    public Message(String messageId, String senderId, String text, long timestamp) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
    }
}
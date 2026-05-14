package aram.kocharyan.skillswap;

public class Message {
    public String messageId;
    public String senderId;
    public String text;
    public long timestamp;
    public int status; // 0-pending, 1-sent, 2-read
    public String type; // "text", "image", "document", "voice", "deleted"
    public boolean edited;
    public String replyToText;
    public String replyToSenderId;
    public String fileName;   // для документов и изображений
    public long duration;     // для голосовых — длительность в секундах

    public Message() {}

    public Message(String messageId, String senderId, String text, long timestamp,
                   int status, String type, boolean edited) {
        this.messageId = messageId;
        this.senderId  = senderId;
        this.text      = text;
        this.timestamp = timestamp;
        this.status    = status;
        this.type      = type;
        this.edited    = edited;
    }
}
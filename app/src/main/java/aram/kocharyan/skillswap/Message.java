package aram.kocharyan.skillswap;

public class Message {
    public String messageId;
    public String senderId;
    public String text;
    public long timestamp;
    public int status; // 0-pending, 1-sent, 2-read
    public String type;
    public boolean edited;
    public String replyToText; // Текст сообщения, на которое отвечаем
    public String replyToSenderId; // Кто автор оригинала

    public Message() {}

    public Message(String messageId, String senderId, String text, long timestamp, int status, String type, boolean edited) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
        this.status = status;
        this.type = type;
        this.edited = edited;
    }
}
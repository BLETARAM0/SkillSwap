package aram.kocharyan.skillswap;

public class Chat {
    public String chatId;
    public String user1;
    public String user2;
    public String lastMessage; // Добавлено
    public long timestamp;     // Переименовано из createdAt для соответствия Firestore

    public Chat() {}

    public Chat(String user1, String user2, long timestamp) {
        this.user1 = user1;
        this.user2 = user2;
        this.timestamp = timestamp;
        this.lastMessage = "Chat started";
    }
}
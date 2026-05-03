package aram.kocharyan.skillswap;

import android.app.Application;

public class SkillSwapApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Agora инициализируется прямо в VideoCallActivity
        // Здесь ничего дополнительного не нужно
    }

    // Эти методы больше не нужны — можно удалить вызовы из Login и Register
    public static void initZegoCall(android.content.Context context,
                                    String userId, String userName) {
        // Zegocloud удалён — метод оставлен чтобы не было ошибок компиляции
        // Можно удалить вместе с вызовами в Login.java и Register.java
    }

    public static void uninitZegoCall() {
        // Zegocloud удалён
    }
}
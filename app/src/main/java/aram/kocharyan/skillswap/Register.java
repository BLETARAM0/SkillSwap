package aram.kocharyan.skillswap;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Register extends AppCompatActivity {

    private EditText etName, etSurname, etEmail, etPassword;
    private Button btnRegister, btnGoLogin;

    private FirebaseAuth auth;
    private DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference("Users");

        // Привязка полей
        etName = findViewById(R.id.etName);
        etSurname = findViewById(R.id.etSurname);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnGoLogin = findViewById(R.id.btnGoLogin);

        // Кнопка регистрации
        btnRegister.setOnClickListener(v -> registerUser());

        // Кнопка "Уже есть аккаунт"
        btnGoLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, Login.class));
            finish();
        });
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String surname = etSurname.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (name.isEmpty() || surname.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Пароль должен быть минимум 6 символов", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {

                        String userId = auth.getCurrentUser().getUid();

                        // Сохраняем пользователя в базу
                        AppUser user = new AppUser(name, surname, email, "", "");
                        database.child(userId).setValue(user);

                        Toast.makeText(this, "Регистрация успешна! ✅", Toast.LENGTH_SHORT).show();

                        // ←←← ВОТ ГЛАВНОЕ ИЗМЕНЕНИЕ ←←←
                        // Переходим сразу в экран выбора режима (ModeSelectFragment)
                        Intent intent = new Intent(Register.this, MainActivity.class);
                        intent.putExtra("show_mode_select", true);
                        startActivity(intent);
                        finish();

                    } else {
                        Toast.makeText(this,
                                "Ошибка: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
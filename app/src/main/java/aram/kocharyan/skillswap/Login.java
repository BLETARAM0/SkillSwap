package aram.kocharyan.skillswap;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class Login extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private FirebaseAuth auth;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {

            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if(email.isEmpty() || password.isEmpty()){
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if(task.isSuccessful()){
                            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(this, MainActivity.class);
                            intent.putExtra("open", "home");
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(this,
                                    task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });
        Button btnGoRegister = findViewById(R.id.btnGoRegister);

        btnGoRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, Register.class));
        });
    }
}
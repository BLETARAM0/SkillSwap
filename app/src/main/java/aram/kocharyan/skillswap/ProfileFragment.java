package aram.kocharyan.skillswap;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private static final String CLOUD_NAME    = "dium7pqky";
    private static final String UPLOAD_PRESET = "SkillSwap";
    private static final String UPLOAD_URL    =
            "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/auto/upload";

    private static final String[] LANGUAGES = {
            "English", "Russian", "Armenian", "Arabic", "Chinese",
            "French", "German", "Spanish", "Turkish", "Persian",
            "Ukrainian", "Georgian", "Azerbaijani", "Kazakh", "Uzbek",
            "Korean", "Japanese", "Italian", "Portuguese", "Hindi"
    };

    private TextView tvName, tvEmail, tvLanguageSelected;
    private ImageView ivAvatar;
    private Spinner spTeach, spStudy, spinnerCountry, spinnerCity;
    private RadioGroup radioGroupMode;
    private RadioButton radioOnline, radioOffline;
    private LinearLayout layoutLocation;
    private Button btnSave;
    private JSONObject countriesObject;

    private FirebaseFirestore db;
    private String userId;
    private Uri cameraImageUri;
    private String selectedLanguage = null;

    // ── Launchers ────────────────────────────────────────────────────────────

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) uploadAvatar(uri);
                }
            });

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && cameraImageUri != null) {
                    uploadAvatar(cameraImageUri);
                }
            });

    private final ActivityResultLauncher<String> cameraPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) openCamera();
                else Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show();
            });

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvName             = view.findViewById(R.id.tvName);
        tvEmail            = view.findViewById(R.id.tvEmail);
        ivAvatar           = view.findViewById(R.id.ivAvatar);
        spTeach            = view.findViewById(R.id.spTeach);
        spStudy            = view.findViewById(R.id.spStudy);
        spinnerCountry     = view.findViewById(R.id.spinnerCountry);
        spinnerCity        = view.findViewById(R.id.spinnerCity);
        radioGroupMode     = view.findViewById(R.id.radioGroupMode);
        radioOnline        = view.findViewById(R.id.radioOnline);
        radioOffline       = view.findViewById(R.id.radioOffline);
        layoutLocation     = view.findViewById(R.id.layoutLocation);
        btnSave            = view.findViewById(R.id.btnSave);
        tvLanguageSelected = view.findViewById(R.id.tvLanguageSelected);

        db     = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Аватарка
        view.findViewById(R.id.avatarContainer).setOnClickListener(v -> showAvatarSourceDialog());

        // Skills
        String[] skills = {"Mathematics Grade 1", "Mathematics Grade 2", "Mathematics Grade 3", "Mathematics Grade 4", "Mathematics Grade 5", "Mathematics Grade 6", "Mathematics Grade 7", "Mathematics Grade 8", "Mathematics Grade 9", "Mathematics Grade 10", "Mathematics Grade 11", "Mathematics Grade 12", "English Grade 1", "English Grade 2", "English Grade 3", "English Grade 4", "English Grade 5", "English Grade 6", "English Grade 7", "English Grade 8", "English Grade 9", "English Grade 10", "English Grade 11", "English Grade 12", "Russian Grade 1", "Russian Grade 2", "Russian Grade 3", "Russian Grade 4", "Russian Grade 5", "Russian Grade 6", "Russian Grade 7", "Russian Grade 8", "Russian Grade 9", "Russian Grade 10", "Russian Grade 11", "Russian Grade 12", "Physics Grade 7", "Physics Grade 8", "Physics Grade 9", "Physics Grade 10", "Physics Grade 11", "Physics Grade 12", "Chemistry Grade 7", "Chemistry Grade 8", "Chemistry Grade 9", "Chemistry Grade 10", "Chemistry Grade 11", "Chemistry Grade 12", "Biology Grade 7", "Biology Grade 8", "Biology Grade 9", "Biology Grade 10", "Biology Grade 11", "Biology Grade 12", "History Grade 5", "History Grade 6", "History Grade 7", "History Grade 8", "History Grade 9", "History Grade 10", "History Grade 11", "History Grade 12", "Geography Grade 6", "Geography Grade 7", "Geography Grade 8", "Geography Grade 9", "Geography Grade 10", "Geography Grade 11", "Geography Grade 12", "Economics Grade 10", "Economics Grade 11", "Economics Grade 12"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, skills);
        spTeach.setAdapter(adapter);
        spStudy.setAdapter(adapter);

        // Выбор языка — кликабельное поле
        tvLanguageSelected.setOnClickListener(v ->
                new AlertDialog.Builder(requireContext())
                        .setTitle("🌐 Language of instruction")
                        .setItems(LANGUAGES, (dialog, which) -> {
                            selectedLanguage = LANGUAGES[which];
                            tvLanguageSelected.setText(selectedLanguage);
                            tvLanguageSelected.setTextColor(0xFF111111);
                        })
                        .show()
        );

        loadCountries();

        spinnerCountry.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                loadCities(parent.getItemAtPosition(pos).toString());
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        radioGroupMode.setOnCheckedChangeListener((group, checkedId) ->
                layoutLocation.setVisibility(checkedId == R.id.radioOffline ? View.VISIBLE : View.GONE));

        // Загружаем данные пользователя
        db.collection("Users").document(userId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name      = document.getString("name");
                        String surname   = document.getString("surname");
                        String email     = document.getString("email");
                        String teach     = document.getString("skillTeach");
                        String study     = document.getString("skillStudy");
                        String mode      = document.getString("mode");
                        String avatarUrl = document.getString("avatarUrl");
                        String language  = document.getString("language");

                        tvName.setText(name + " " + surname);
                        tvEmail.setText(email);

                        if (teach != null) spTeach.setSelection(adapter.getPosition(teach));
                        if (study != null) spStudy.setSelection(adapter.getPosition(study));

                        if ("offline".equals(mode)) {
                            radioOffline.setChecked(true);
                            layoutLocation.setVisibility(View.VISIBLE);
                        } else {
                            radioOnline.setChecked(true);
                        }

                        // Язык
                        if (language != null && !language.isEmpty()) {
                            selectedLanguage = language;
                            tvLanguageSelected.setText(language);
                            tvLanguageSelected.setTextColor(0xFF111111);
                        }

                        loadAvatar(avatarUrl);
                    }
                });

        btnSave.setOnClickListener(v -> saveProfile(adapter));
    }

    // ── Avatar ───────────────────────────────────────────────────────────────

    private void showAvatarSourceDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Change Photo")
                .setItems(new String[]{"Camera", "Gallery"}, (dialog, which) -> {
                    if (which == 0) {
                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                                == PackageManager.PERMISSION_GRANTED) {
                            openCamera();
                        } else {
                            cameraPermLauncher.launch(Manifest.permission.CAMERA);
                        }
                    } else {
                        openGallery();
                    }
                })
                .show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(requireActivity().getPackageManager()) == null) {
            Toast.makeText(requireContext(), "No camera app found", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File dir  = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File file = File.createTempFile("AVA_" + ts + "_", ".jpg", dir);
            cameraImageUri = FileProvider.getUriForFile(
                    requireContext(), requireContext().getPackageName() + ".provider", file);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            cameraLauncher.launch(intent);
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Could not open camera", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadAvatar(String avatarUrl) {
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(avatarUrl)
                    .transform(new CircleCrop())
                    .placeholder(android.R.drawable.ic_menu_camera)
                    .into(ivAvatar);
        }
    }

    private void uploadAvatar(Uri uri) {
        Toast.makeText(requireContext(), "Uploading photo...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                InputStream is = requireContext().getContentResolver().openInputStream(uri);
                if (is == null) throw new IOException("Cannot open stream");
                byte[] fileBytes = is.readAllBytes();
                is.close();

                String boundary = "Boundary" + System.currentTimeMillis();
                HttpURLConnection conn = (HttpURLConnection) new URL(UPLOAD_URL).openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                conn.setConnectTimeout(30_000);
                conn.setReadTimeout(60_000);

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n");
                dos.writeBytes(UPLOAD_PRESET + "\r\n");
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"avatar.jpg\"\r\n");
                dos.writeBytes("Content-Type: image/jpeg\r\n\r\n");
                dos.write(fileBytes);
                dos.writeBytes("\r\n--" + boundary + "--\r\n");
                dos.flush();
                dos.close();

                int code = conn.getResponseCode();
                InputStream resp = (code == 200) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(resp));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                conn.disconnect();

                if (code == 200) {
                    String avatarUrl = new JSONObject(sb.toString()).getString("secure_url");
                    db.collection("Users").document(userId)
                            .update("avatarUrl", avatarUrl)
                            .addOnSuccessListener(v -> {
                                if (getActivity() == null) return;
                                requireActivity().runOnUiThread(() -> {
                                    loadAvatar(avatarUrl);
                                    Toast.makeText(requireContext(), "Photo updated ✅", Toast.LENGTH_SHORT).show();
                                });
                            });
                } else {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Upload failed", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // ── Save Profile ─────────────────────────────────────────────────────────

    private void saveProfile(ArrayAdapter<String> adapter) {
        String teach   = spTeach.getSelectedItem().toString();
        String study   = spStudy.getSelectedItem().toString();
        String mode    = radioOnline.isChecked() ? "online" : "offline";
        String country = "offline".equals(mode) ? spinnerCountry.getSelectedItem().toString() : "";
        String city    = "offline".equals(mode) ? spinnerCity.getSelectedItem().toString() : "";

        if (teach.equals(study)) {
            Toast.makeText(requireContext(), "Skills should not match", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("Users").document(userId)
                .update("skillTeach", teach,
                        "skillStudy",  study,
                        "mode",        mode,
                        "country",     country,
                        "city",        city,
                        "language",    selectedLanguage != null ? selectedLanguage : "")
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(requireContext(), "Profile updated ✅", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    // ── Countries ────────────────────────────────────────────────────────────

    private void loadCountries() {
        try {
            InputStream is = requireContext().getAssets().open("countries.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            countriesObject = new JSONObject(new String(buffer, "UTF-8"));
            Iterator<String> keys = countriesObject.keys();
            ArrayList<String> countryList = new ArrayList<>();
            while (keys.hasNext()) countryList.add(keys.next());
            spinnerCountry.setAdapter(new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_dropdown_item, countryList));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadCities(String country) {
        try {
            JSONArray citiesArray = countriesObject.getJSONArray(country);
            ArrayList<String> cityList = new ArrayList<>();
            for (int i = 0; i < citiesArray.length(); i++)
                cityList.add(citiesArray.getString(i));
            spinnerCity.setAdapter(new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_dropdown_item, cityList));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
package com.almaquinta.analytics.iu.profile;

import android.app.ProgressDialog;
import android.app.DatePickerDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.almaquinta.analytics.R;
import com.almaquinta.analytics.data.model.AppUser;
import com.almaquinta.analytics.iu.common.SystemBarsEdgeToEdge;
import com.almaquinta.analytics.session.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {
    private static final int IMAGE_MAX_BYTES = 70 * 1024, IMAGE_MAX_DIMENSION = 256, KEYBOARD_THRESHOLD_DP = 120;
    private EditText etName, etLastName, etPhone, etProfession, etBirthDate, etInstagram;
    private ImageView ivProfilePreview;
    private ProgressDialog progressDialog;
    private DatabaseReference userRef;
    private String selectedImageBase64 = "";
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<Void> cameraLauncher;
    private ScrollView scrollProfile;
    private View currentFocusedField, currentFocusedTarget;
    private ViewTreeObserver.OnGlobalLayoutListener keyboardLayoutListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        SystemBarsEdgeToEdge.applyWithIme(this, R.id.scrollProfile);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, R.string.profile_error_no_session, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userRef = FirebaseDatabase.getInstance().getReference("Usuarios").child(currentUser.getUid());
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(getString(R.string.profile_loading));
        progressDialog.setCanceledOnTouchOutside(false);

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                handlePickedImage(uri);
            }
        });

        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
            if (bitmap != null) {
                handlePickedBitmap(bitmap);
            }
        });

        bindViews();
        loadProfileData();
    }


    private void bindViews() {
        ImageView btnBack = findViewById(R.id.btnBackProfile);
        Button btnSave = findViewById(R.id.btnSaveProfile);
        Button btnPickPhoto = findViewById(R.id.btnPickPhoto);

        etName = findViewById(R.id.etProfileName);
        etLastName = findViewById(R.id.etProfileLastName);
        etPhone = findViewById(R.id.etPhone);
        etProfession = findViewById(R.id.etProfession);
        etBirthDate = findViewById(R.id.etBirthDate);
        etInstagram = findViewById(R.id.etInstagram);
        ivProfilePreview = findViewById(R.id.ivProfilePreview);
        scrollProfile = findViewById(R.id.scrollProfile);

        btnBack.setOnClickListener(v -> finish());
        btnPickPhoto.setOnClickListener(v -> showImageSourceChooser());
        btnSave.setOnClickListener(v -> saveProfile());
        etBirthDate.setOnClickListener(v -> showBirthDatePicker());
        setupFocusAutoScroll();
    }

    private void setupFocusAutoScroll() {
        if (scrollProfile == null) {
            return;
        }

        View phoneContainer = findViewById(R.id.containerPhoneProfile);
        bindFocusAutoScroll(etName, etName);
        bindFocusAutoScroll(etLastName, etLastName);
        bindFocusAutoScroll(etPhone, phoneContainer != null ? phoneContainer : etPhone);
        bindFocusAutoScroll(etProfession, etProfession);
        bindFocusAutoScroll(etInstagram, etInstagram);
        setupKeyboardVisibilityScroll();
    }

    private void bindFocusAutoScroll(View focusableField, View targetToReveal) {
        if (focusableField == null || targetToReveal == null || scrollProfile == null) {
            return;
        }
        focusableField.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                if (currentFocusedField == focusableField) {
                    currentFocusedField = null;
                    currentFocusedTarget = null;
                }
                return;
            }

            currentFocusedField = focusableField;
            currentFocusedTarget = targetToReveal;
            scrollProfile.post(() -> scrollTargetIntoView(targetToReveal));
            scrollProfile.postDelayed(() -> scrollTargetIntoView(targetToReveal), 180L);
            scrollProfile.postDelayed(() -> ensureTargetVisibleAboveKeyboard(targetToReveal), 240L);
        });
    }

    private void setupKeyboardVisibilityScroll() {
        if (scrollProfile == null || keyboardLayoutListener != null) {
            return;
        }

        keyboardLayoutListener = () -> {
            if (currentFocusedTarget == null || getCurrentFocus() == null) {
                return;
            }
            ensureTargetVisibleAboveKeyboard(currentFocusedTarget);
        };
        scrollProfile.getViewTreeObserver().addOnGlobalLayoutListener(keyboardLayoutListener);
    }

    private void scrollTargetIntoView(View target) {
        if (scrollProfile == null) {
            return;
        }
        View content = scrollProfile.getChildAt(0);
        if (!(content instanceof ViewGroup) || target == null) {
            return;
        }
        ViewGroup contentGroup = (ViewGroup) content;

        Rect rect = new Rect();
        target.getDrawingRect(rect);
        contentGroup.offsetDescendantRectToMyCoords(target, rect);
        rect.bottom += dpToPx(24);
        scrollProfile.requestChildRectangleOnScreen(contentGroup, rect, true);
    }

    private void ensureTargetVisibleAboveKeyboard(View target) {
        if (scrollProfile == null || target == null) {
            return;
        }

        Rect visibleFrame = new Rect();
        scrollProfile.getWindowVisibleDisplayFrame(visibleFrame);
        int keyboardHeight = scrollProfile.getRootView().getHeight() - visibleFrame.bottom;
        if (keyboardHeight < dpToPx(KEYBOARD_THRESHOLD_DP)) {
            return;
        }

        int[] location = new int[2];
        target.getLocationOnScreen(location);
        int targetBottom = location[1] + target.getHeight() + dpToPx(16);
        int keyboardTop = visibleFrame.bottom;
        if (targetBottom > keyboardTop) {
            scrollProfile.smoothScrollBy(0, targetBottom - keyboardTop);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        ));
    }

    private void loadProfileData() {
        progressDialog.setMessage(getString(R.string.profile_loading_data));
        progressDialog.show();

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressDialog.dismiss();

                String name = getSafeString(snapshot.child("nombre").getValue());
                String lastName = getSafeString(snapshot.child("apellido").getValue());
                String phone = getSafeString(snapshot.child("telefono").getValue());
                String profession = getSafeString(snapshot.child("profesion").getValue());
                String birthDate = getSafeString(snapshot.child("fechaNacimiento").getValue());
                String instagram = getSafeString(snapshot.child("instagram").getValue());
                if (instagram.isEmpty()) {
                    instagram = getSafeString(snapshot.child("redesSociales").getValue());
                }

                etName.setText(name);
                etLastName.setText(lastName);
                etPhone.setText(phone);
                etProfession.setText(profession);
                etBirthDate.setText(birthDate);
                etInstagram.setText(instagram);

                selectedImageBase64 = getSafeString(snapshot.child("profileImageBase64").getValue());
                if (!selectedImageBase64.isEmpty()) {
                    setPreviewImage(selectedImageBase64);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
                Toast.makeText(ProfileActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProfile() {
        String name = etName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String profession = etProfession.getText().toString().trim();
        String birthDate = etBirthDate.getText().toString().trim();
        String instagram = etInstagram.getText().toString().trim();

        if (TextUtils.isEmpty(name)
                || TextUtils.isEmpty(lastName)
                || TextUtils.isEmpty(phone)
                || TextUtils.isEmpty(profession)
                || TextUtils.isEmpty(birthDate)) {
            Toast.makeText(this, R.string.profile_error_name_required, Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage(getString(R.string.profile_saving_data));
        progressDialog.show();

        Map<String, Object> updates = new HashMap<>();
        updates.put("nombre", name);
        updates.put("apellido", lastName);
        updates.put("telefono", phone);
        updates.put("profesion", profession);
        updates.put("fechaNacimiento", birthDate);
        updates.put("instagram", instagram);
        updates.put("redesSociales", instagram);
        updates.put("profileImageBase64", selectedImageBase64);

        userRef.updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    progressDialog.dismiss();
                    AppUser current = SessionManager.getInstance().getCurrentUser();
                    if (current != null) {
                        AppUser updated = new AppUser(lastName, current.getId(), name, current.getEmail(), current.getRole(), current.isActive());
                        SessionManager.getInstance().setCurrentUser(updated);
                    }
                    Toast.makeText(this, R.string.profile_saved, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, getString(R.string.profile_save_error, e.getMessage()), Toast.LENGTH_SHORT).show();
                });
    }

    private void showBirthDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog picker = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = String.format(Locale.getDefault(), "%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                    etBirthDate.setText(date);
                },
                year,
                month,
                day
        );
        picker.show();
    }

    private void handlePickedImage(Uri uri) {
        try {
            InputStream stream = getContentResolver().openInputStream(uri);
            if (stream == null) {
                return;
            }
            Bitmap bitmap = BitmapFactory.decodeStream(stream);
            stream.close();
            if (bitmap == null) {
                return;
            }
            handlePickedBitmap(bitmap);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.profile_image_error, e.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }

    private void handlePickedBitmap(Bitmap bitmap) {
        try {
            Bitmap normalized = scaleBitmap(bitmap);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int quality = 70;
            normalized.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            while (outputStream.size() > IMAGE_MAX_BYTES && quality > 35) {
                outputStream.reset();
                quality -= 10;
                normalized.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            }

            byte[] bytes = outputStream.toByteArray();
            selectedImageBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
            ivProfilePreview.setImageBitmap(normalized);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.profile_image_error, e.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap scaleBitmap(Bitmap source) {
        int width = source.getWidth(), height = source.getHeight();
        if (width <= IMAGE_MAX_DIMENSION && height <= IMAGE_MAX_DIMENSION) {
            return source;
        }

        float scale = Math.min((float) IMAGE_MAX_DIMENSION / width, (float) IMAGE_MAX_DIMENSION / height);
        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);
        return Bitmap.createScaledBitmap(source, newWidth, newHeight, true);
    }

    private void showImageSourceChooser() {
        String[] options = new String[]{
                getString(R.string.profile_take_photo),
                getString(R.string.profile_choose_gallery)
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.profile_photo_options)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        cameraLauncher.launch(null);
                    } else {
                        galleryLauncher.launch("image/*");
                    }
                })
                .show();
    }

    private void setPreviewImage(String base64) {
        try {
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bitmap != null) {
                ivProfilePreview.setImageBitmap(bitmap);
            }
        } catch (Exception ignored) {
            ivProfilePreview.setImageResource(R.drawable.company_lg);
        }
    }

    private String getSafeString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    @Override
    protected void onDestroy() {
        if (scrollProfile != null
                && keyboardLayoutListener != null
                && scrollProfile.getViewTreeObserver().isAlive()) {
            scrollProfile.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardLayoutListener);
        }
        super.onDestroy();
    }
}



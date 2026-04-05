package com.almaquinta.analytics.iu.usermanagement;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.view.WindowInsetsCompat;

import com.almaquinta.analytics.R;
import com.almaquinta.analytics.data.model.AppUser;
import com.almaquinta.analytics.data.model.UserRole;
import com.almaquinta.analytics.security.AuthorizationService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UserManagementActivity extends AppCompatActivity {
    private final AuthorizationService authorizationService = new AuthorizationService();
    private final List<AppUser> users = new ArrayList<>();

    private UserAdapter adapter;
    private TextView tvTotalUsers;
    private TextView tvActiveUsers;
    private TextView tvInactiveUsers;
    private TextView tvAdmins;
    private TextView tvCoordinators;
    private TextView tvEmployees;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!hasAdminAccess()) {
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_management);
        configureSystemBars();

        View contentView = findViewById(R.id.scrollUserManagement);
        ViewCompat.setOnApplyWindowInsetsListener(contentView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bindViews();
        observeUsers();
    }

    private void configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
            getWindow().setStatusBarContrastEnforced(false);
        }
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);
    }

    private void bindViews() {
        tvTotalUsers = findViewById(R.id.tvTotalUsers);
        tvActiveUsers = findViewById(R.id.tvActiveUsers);
        tvInactiveUsers = findViewById(R.id.tvInactiveUsers);
        tvAdmins = findViewById(R.id.tvAdmins);
        tvCoordinators = findViewById(R.id.tvCoordinators);
        tvEmployees = findViewById(R.id.tvEmployees);
        ListView listUsers = findViewById(R.id.listUsers);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        adapter = new UserAdapter();
        listUsers.setAdapter(adapter);
        listUsers.setOnItemClickListener((parent, view, position, id) -> openEditDialog(users.get(position)));
    }

    private void observeUsers() {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Usuarios");
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                users.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    AppUser user = parseUser(child);
                    if (user == null) {
                        continue;
                    }
                    users.add(user);
                }

                users.sort((o1, o2) -> buildDisplayName(o1).compareToIgnoreCase(buildDisplayName(o2)));
                renderSummary();
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserManagementActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private AppUser parseUser(DataSnapshot snapshot) {
        String uid = getString(snapshot, "uid", snapshot.getKey());
        String name = getString(snapshot, "nombre", "");
        String lastName = getString(snapshot, "apellido", "");
        String email = getString(snapshot, "correo", "");
        String roleRaw = getString(snapshot, "role", "EMPLOYEE");
        boolean active = getBoolean(snapshot.child("active").getValue());

        if (uid == null || uid.trim().isEmpty()) {
            return null;
        }

        UserRole role = parseRole(roleRaw);
        return new AppUser(lastName, uid, name, email, role, active);
    }

    private void renderSummary() {
        int active = 0;
        int inactive = 0;
        int admins = 0;
        int coordinators = 0;
        int employees = 0;

        for (AppUser user : users) {
            if (user.isActive()) active++;
            else inactive++;

            if (user.getRole() == UserRole.ADMIN) admins++;
            else if (user.getRole() == UserRole.COORDINATOR) coordinators++;
            else employees++;
        }

        tvTotalUsers.setText(String.valueOf(users.size()));
        tvActiveUsers.setText(String.valueOf(active));
        tvInactiveUsers.setText(String.valueOf(inactive));
        tvAdmins.setText(String.valueOf(admins));
        tvCoordinators.setText(String.valueOf(coordinators));
        tvEmployees.setText(String.valueOf(employees));
    }

    private void openEditDialog(AppUser user) {
        authorizationService.requireAdminOnly();

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_user_management, null, false);
        TextView tvName = dialogView.findViewById(R.id.tvDialogName);
        TextView tvEmail = dialogView.findViewById(R.id.tvDialogEmail);
        Spinner spRole = dialogView.findViewById(R.id.spDialogRole);
        SwitchCompat swActive = dialogView.findViewById(R.id.swDialogActive);

        tvName.setText(buildDisplayName(user));
        tvEmail.setText(user.getEmail());
        swActive.setChecked(user.isActive());

        String[] roles = new String[]{"Administrador", "Coordinador", "Empleado"};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, roles);
        roleAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        spRole.setAdapter(roleAdapter);
        spRole.setSelection(roleIndex(user.getRole()));

        new AlertDialog.Builder(this)
                .setTitle(R.string.user_mgmt_dialog_title)
                .setView(dialogView)
                .setNegativeButton(R.string.user_mgmt_dialog_cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.user_mgmt_dialog_save, null)
                .show()
                .setOnShowListener(dialogInterface -> {
                    AlertDialog dialog = (AlertDialog) dialogInterface;
                    Button saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    saveButton.setOnClickListener(v -> saveUserChanges(user, spRole, swActive, dialog));
                });
    }

    private void saveUserChanges(AppUser user, Spinner spRole, SwitchCompat swActive, AlertDialog dialog) {
        authorizationService.requireAdminOnly();

        UserRole role = parseRole(String.valueOf(spRole.getSelectedItem()));
        boolean active = swActive.isChecked();

        Map<String, Object> updates = new HashMap<>();
        updates.put("role", role.name());
        updates.put("active", active);
        updates.put("nombre", user.getName());
        updates.put("apellido", user.getLastName());
        updates.put("correo", user.getEmail());
        updates.put("uid", user.getId());

        FirebaseDatabase.getInstance()
                .getReference("Usuarios")
                .child(user.getId())
                .updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, R.string.user_mgmt_update_success, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> Toast.makeText(this, getString(R.string.user_mgmt_update_error, e.getMessage()), Toast.LENGTH_SHORT).show());
    }

    private boolean hasAdminAccess() {
        try {
            authorizationService.requireAdminOnly();
            return true;
        } catch (SecurityException ex) {
            Toast.makeText(this, getString(R.string.user_mgmt_admin_only), Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }
    }

    private String getString(DataSnapshot snapshot, String key, String fallback) {
        Object value = snapshot.child(key).getValue();
        return value != null ? String.valueOf(value) : fallback;
    }

    private boolean getBoolean(Object value) {
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) return Boolean.parseBoolean((String) value);
        return true;
    }

    private UserRole parseRole(String roleRaw) {
        if (roleRaw == null) return UserRole.EMPLOYEE;
        if ("ADMIN".equalsIgnoreCase(roleRaw) || "Administrador".equalsIgnoreCase(roleRaw)) return UserRole.ADMIN;
        if ("COORDINATOR".equalsIgnoreCase(roleRaw) || "Coordinador".equalsIgnoreCase(roleRaw)) return UserRole.COORDINATOR;
        return UserRole.EMPLOYEE;
    }

    private int roleIndex(UserRole role) {
        if (role == UserRole.ADMIN) return 0;
        if (role == UserRole.COORDINATOR) return 1;
        return 2;
    }

    private String buildDisplayName(AppUser user) {
        return String.format(Locale.getDefault(), "%s %s", user.getName(), user.getLastName()).trim();
    }

    private String buildRoleLabel(UserRole role) {
        if (role == UserRole.ADMIN) return "Administrador";
        if (role == UserRole.COORDINATOR) return "Coordinador";
        return "Empleado";
    }

    private class UserAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return users.size();
        }

        @Override
        public Object getItem(int position) {
            return users.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, android.view.ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(UserManagementActivity.this).inflate(R.layout.item_user_management, parent, false);
            }

            AppUser user = users.get(position);
            TextView tvName = view.findViewById(R.id.tvUserName);
            TextView tvDetails = view.findViewById(R.id.tvUserDetails);
            TextView tvStatus = view.findViewById(R.id.tvUserStatus);

            tvName.setText(buildDisplayName(user));
            tvDetails.setText(getString(R.string.user_mgmt_user_details, user.getEmail(), buildRoleLabel(user.getRole())));
            tvStatus.setText(user.isActive() ? "Activo" : "Inactivo");
            tvStatus.setBackgroundResource(user.isActive() ? R.drawable.bg_status_active : R.drawable.bg_status_inactive);
            return view;
        }
    }
}







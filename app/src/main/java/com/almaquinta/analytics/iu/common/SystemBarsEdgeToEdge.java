package com.almaquinta.analytics.iu.common;

import android.graphics.Color;
import android.os.Build;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public final class SystemBarsEdgeToEdge {

    private SystemBarsEdgeToEdge() {
    }

    public static void apply(@NonNull AppCompatActivity activity, int... insetTargetViewIds) {
        apply(activity, false, false, insetTargetViewIds);
    }

    public static void applyWithIme(@NonNull AppCompatActivity activity, int... insetTargetViewIds) {
        apply(activity, false, true, insetTargetViewIds);
    }

    public static void apply(@NonNull AppCompatActivity activity, boolean lightSystemBars, int... insetTargetViewIds) {
        apply(activity, lightSystemBars, false, insetTargetViewIds);
    }

    public static void apply(@NonNull AppCompatActivity activity, boolean lightSystemBars, boolean includeImeInsets, int... insetTargetViewIds) {
        EdgeToEdge.enable(activity);
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
        activity.getWindow().setNavigationBarColor(Color.TRANSPARENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            activity.getWindow().setNavigationBarDividerColor(Color.TRANSPARENT);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activity.getWindow().setNavigationBarContrastEnforced(false);
            activity.getWindow().setStatusBarContrastEnforced(false);
        }

        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(lightSystemBars);
            controller.setAppearanceLightNavigationBars(lightSystemBars);
        }

        if (insetTargetViewIds == null) {
            return;
        }

        for (int viewId : insetTargetViewIds) {
            if (viewId == View.NO_ID) {
                continue;
            }
            View target = activity.findViewById(viewId);
            if (target == null) {
                continue;
            }
            final int baseLeft = target.getPaddingLeft(), baseTop = target.getPaddingTop(), baseRight = target.getPaddingRight(), baseBottom = target.getPaddingBottom();

            ViewCompat.setOnApplyWindowInsetsListener(target, (view, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                int bottomInset = systemBars.bottom;
                if (includeImeInsets) {
                    Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
                    bottomInset = Math.max(bottomInset, imeInsets.bottom);
                }
                view.setPadding(
                        baseLeft + systemBars.left,
                        baseTop + systemBars.top,
                        baseRight + systemBars.right,
                        baseBottom + bottomInset
                );
                return insets;
            });
            ViewCompat.requestApplyInsets(target);
        }
    }
}


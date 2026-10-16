package com.atlasx.gis.util;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.atlasx.gis.R;
import com.google.android.material.snackbar.Snackbar;

/** Small UI helpers shared by every screen: toasts, dialogs and sizing. */
public final class Ui {

    private Ui() {
    }

    /** Converts density-independent pixels to real pixels. */
    public static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    public static void toast(Context context, CharSequence message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void toast(Context context, @StringRes int message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void snack(View view, CharSequence message) {
        if (view != null) {
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
        }
    }

    /**
     * Confirmation dialog used before every destructive action, as required by
     * the interface guidelines.
     */
    public static void confirm(Context context, CharSequence title,
                               CharSequence message, CharSequence positiveLabel,
                               Runnable onConfirmed) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(R.string.cancel, (d, w) -> d.dismiss())
                .setPositiveButton(positiveLabel, (d, w) -> {
                    if (onConfirmed != null) {
                        onConfirmed.run();
                    }
                })
                .show();
    }

    /** Adds a child view with bottom margin equal to the given dp value. */
    public static void addWithBottomMargin(ViewGroup parent, View child, int marginDp) {
        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(parent.getContext(), marginDp);
        parent.addView(child, params);
    }

    /** Shows the soft keyboard for a view (used by the search field). */
    public static void showKeyboard(Activity activity, View view) {
        view.requestFocus();
        InputMethodManager manager =
                (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) {
            manager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    /** Hides the soft keyboard when the screen does not need it any more. */
    public static void hideKeyboard(Activity activity) {
        View focus = activity.getCurrentFocus();
        if (focus == null) {
            return;
        }
        InputMethodManager manager =
                (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) {
            manager.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }

    /** Convenience: sets a text view only when it exists in the layout. */
    public static void setTextOrGone(TextView view, CharSequence text, boolean showEmpty) {
        if (view == null) {
            return;
        }
        view.setText(text);
        boolean empty = text == null || text.length() == 0;
        view.setVisibility(!empty || showEmpty ? View.VISIBLE : View.GONE);
    }

    /** Human readable byte size, used by the dashboard storage statistic. */
    public static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format(java.util.Locale.getDefault(), "%.1f kB", kb);
        }
        double mb = kb / 1024.0;
        if (mb < 1024) {
            return String.format(java.util.Locale.getDefault(), "%.2f MB", mb);
        }
        return String.format(java.util.Locale.getDefault(), "%.2f GB", mb / 1024.0);
    }
}

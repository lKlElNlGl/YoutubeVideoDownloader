package com.lklelnlgl.iconlab;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int[] SCALES = {108, 109, 110, 111, 112, 113, 114, 115};
    private static final int[] ICONS = {
            R.mipmap.lab_size_01,
            R.mipmap.lab_size_02,
            R.mipmap.lab_size_03,
            R.mipmap.lab_size_04,
            R.mipmap.lab_size_05,
            R.mipmap.lab_size_06,
            R.mipmap.lab_size_07,
            R.mipmap.lab_size_08
    };
    private TextView status;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(dp(24), dp(24), dp(24), dp(24));

        status = new TextView(this);
        status.setTextSize(17);
        status.setGravity(Gravity.CENTER);
        status.setText("8 вариантов утверждённой иконки YoutubeVideoDownloader.\n"
                + "Рисунок без изменений — только масштаб 108–115%.\n"
                + "Нажмите кнопку, чтобы запросить размещение всех значков на рабочем столе.");
        layout.addView(status, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button button = new Button(this);
        button.setText("Добавить 8 значков на рабочий стол");
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(24);
        layout.addView(button, params);
        setContentView(layout);
        button.setOnClickListener(view -> addAllIcons());
    }

    private void addAllIcons() {
        PackageManager pm = getPackageManager();
        int enabled = 0;
        for (int i = 0; i < SCALES.length; i++) {
            String label = String.format(Locale.US, "%02d", i + 1);
            ComponentName alias = new ComponentName(
                    this, getPackageName() + ".LabSize" + label);
            try {
                pm.setComponentEnabledSetting(alias,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP);
                enabled++;
            } catch (RuntimeException error) {
                // Keep other variants available even if a single alias is rejected.
            }
        }

        ShortcutManager shortcuts = getSystemService(ShortcutManager.class);
        if (shortcuts == null || !shortcuts.isRequestPinShortcutSupported()) {
            status.setText("Включено " + enabled + " значков в списке приложений.\n"
                    + "Ваш лаунчер не поддерживает автоматические запросы на рабочий стол. "
                    + "Перетащите значки из списка приложений вручную.");
            Toast.makeText(this, "Значки включены в списке приложений",
                    Toast.LENGTH_LONG).show();
            return;
        }

        int requested = 0;
        // Android does not allow silent pinning. Each request is forwarded
        // to the launcher, which may ask for approval or discard pending pins.
        for (int i = 0; i < SCALES.length; i++) {
            String number = String.format(Locale.US, "%02d", i + 1);
            Intent launch = new Intent(this, MainActivity.class)
                    .setAction(Intent.ACTION_VIEW)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra("iconlab_variant", number);
            ShortcutInfo pin = new ShortcutInfo.Builder(this, "iconlab_size_" + number)
                    .setShortLabel(number + " · " + SCALES[i] + "%")
                    .setIcon(Icon.createWithResource(this, ICONS[i]))
                    .setIntent(launch)
                    .build();
            try {
                if (shortcuts.requestPinShortcut(pin, null)) requested++;
            } catch (RuntimeException ignored) {
                // A launcher may reject individual pins; aliases remain visible.
            }
        }
        status.setText("Запрошено размещение " + requested + " из 8 значков.\n"
                + "Подтвердите запросы лаунчера, если они появятся. "
                + "Если часть ярлыков не добавилась, найдите значки 01–08 "
                + "в списке приложений и перенесите их на рабочий стол.");
        Toast.makeText(this, "Запрос на добавление восьми значков отправлен",
                Toast.LENGTH_LONG).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

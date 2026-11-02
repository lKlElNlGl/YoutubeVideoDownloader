package com.lklelnlgl.iconlab;

import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int VARIANT_COUNT = 8;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(dp(24), dp(24), dp(24), dp(24));

        TextView description = new TextView(this);
        description.setTextSize(17);
        description.setGravity(Gravity.CENTER);
        description.setText("Восемь вариантов значка YoutubeVideoDownloader.\n"
                + "После нажатия откройте список приложений и перенесите значки "
                + "Icon 01 — Icon 08 на свободную страницу рабочего стола.");
        layout.addView(description, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button button = new Button(this);
        button.setText("Показать 8 вариантов");
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonParams.topMargin = dp(24);
        layout.addView(button, buttonParams);
        setContentView(layout);

        button.setOnClickListener(view -> {
            PackageManager pm = getPackageManager();
            int completed = 0;
            try {
                for (int i = 1; i <= VARIANT_COUNT; i++) {
                    String alias = String.format(Locale.ROOT,
                            "com.lklelnlgl.iconlab.LabVariant%02d", i);
                    ComponentName name = new ComponentName(this, alias);
                    pm.setComponentEnabledSetting(name,
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                            PackageManager.DONT_KILL_APP);
                    completed++;
                }
                description.setText("Включены все 8 значков.\n"
                        + "Откройте список приложений и перенесите Icon 01 — Icon 08 "
                        + "на последнюю страницу рабочего стола.\n"
                        + "Автоматическое размещение Android не разрешает.");
                button.setText("Показать 8 вариантов снова");
                Toast.makeText(this, "Восемь вариантов включены", Toast.LENGTH_LONG).show();
            } catch (RuntimeException exception) {
                Toast.makeText(this, "Включено " + completed
                        + " значков. Ошибка: " + exception.getClass().getSimpleName(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

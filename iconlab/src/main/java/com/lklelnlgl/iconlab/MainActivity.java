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

public final class MainActivity extends Activity {
    private static final int VARIANT_COUNT = 4;

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
        description.setText("Clean C — исходный образец.\n"
                + "C1–C3: очистка верхней каймы и нейтральный серый блик. "
                + "Размер и низ как у Clean C.");
        layout.addView(description, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button button = new Button(this);
        button.setText("Показать 4 варианта");
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonParams.topMargin = dp(24);
        layout.addView(button, buttonParams);
        setContentView(layout);

        button.setOnClickListener(view -> {
            PackageManager pm = getPackageManager();
            int completed = 0;
            try {
                for (int i = 0; i < VARIANT_COUNT; i++) {
                    String alias = getPackageName() + ".LabClean" + (i == 0 ? "C" : "C" + i);
                    pm.setComponentEnabledSetting(
                            new ComponentName(this, alias),
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                            PackageManager.DONT_KILL_APP);
                    completed++;
                }
                description.setText("Все 4 варианта включены.\n"
                        + "Найдите Clean C, C1, C2 и C3 в списке приложений. "
                        + "Сравните верхнюю кайму и цвет блика.");
                Toast.makeText(this, "Включены Clean C — Clean C3", Toast.LENGTH_LONG).show();
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

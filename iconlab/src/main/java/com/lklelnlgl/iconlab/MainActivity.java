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
    private static final int VARIANT_COUNT = 9;

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
        description.setText("Девять вариантов Icon 04: от 100% до 98% с шагом 0,25%.\n"
                + "Нажмите кнопку, затем найдите Icon 04a — Icon 04i "
                + "в списке приложений.");
        layout.addView(description, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button button = new Button(this);
        button.setText("Показать 9 вариантов");
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
                    String alias = getPackageName() + ".LabVariant04" + (char) ('a' + i);
                    ComponentName name = new ComponentName(this, alias);
                    pm.setComponentEnabledSetting(name,
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                            PackageManager.DONT_KILL_APP);
                    completed++;
                }
                description.setText("Все 9 значков включены.\n"
                        + "Откройте список приложений и сравните Icon 04a — Icon 04i. "
                        + "При необходимости перенесите их на рабочий стол.");
                Toast.makeText(this, "Включены Icon 04a — Icon 04i", Toast.LENGTH_LONG).show();
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

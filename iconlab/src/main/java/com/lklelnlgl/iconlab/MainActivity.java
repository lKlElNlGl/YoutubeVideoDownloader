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
        description.setText("Контрольное сравнение двух иконок на одном Realme.\n"
                + "A — точно старый Clean C2 из Icon Lab v0.3.2.\n"
                + "B — новый утверждённый рисунок, 100% из v0.5.5.\n"
                + "Оба — обычные PNG, без фоновых слоёв.");
        layout.addView(description, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button button = new Button(this);
        button.setText("Показать 2 значка");
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(24);
        layout.addView(button, params);
        setContentView(layout);

        button.setOnClickListener(view -> {
            PackageManager pm = getPackageManager();
            int enabled = 0;
            for (String alias : new String[] { "LabProbeOld", "LabProbeNew" }) {
                try {
                    pm.setComponentEnabledSetting(
                        new ComponentName(this, getPackageName() + "." + alias),
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP);
                    enabled++;
                } catch (RuntimeException e) {
                    Toast.makeText(this, "Ошибка добавления: " + alias,
                            Toast.LENGTH_LONG).show();
                }
            }
            description.setText("Включено " + enabled + " из 2 значков.\n"
                    + "Сравни A · Старый C2 и B · Новый 100% в списке приложений.");
        });
    }
    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

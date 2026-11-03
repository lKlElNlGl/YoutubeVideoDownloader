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
    private static final String[] ICON_ALIASES = {"LabSourceSmall", "LabSourceLarge"};

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
        description.setText("Две иконки, которые ты прислал, без редактирования.\n"
                + "A — исходный PNG 192×189.\n"
                + "B — исходный большой PNG 1035×1019.\n"
                + "Оба — обычные PNG с прозрачностью, без фоновых слоёв.");
        layout.addView(description, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        Button button = new Button(this);
        button.setText("Показать 2 значка");
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(24);
        layout.addView(button, params);
        setContentView(layout);

        button.setOnClickListener(view -> {
            int enabled = 0;
            PackageManager pm = getPackageManager();
            for (String alias : ICON_ALIASES) {
                try {
                    pm.setComponentEnabledSetting(
                            new ComponentName(this, getPackageName() + "." + alias),
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                            PackageManager.DONT_KILL_APP);
                    enabled++;
                } catch (RuntimeException exception) {
                    Toast.makeText(this, "Ошибка: " + alias
                                    + " (" + exception.getClass().getSimpleName() + ")",
                            Toast.LENGTH_LONG).show();
                }
            }
            description.setText("Включено " + enabled + " из 2 значков.\n"
                    + "Сравни _A_192px и _B_Большая в списке приложений.");
            if (enabled == 2) {
                Toast.makeText(this, "Оба значка включены", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

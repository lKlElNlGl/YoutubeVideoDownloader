package com.lklengl.youtubevideodownloader;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.LinearLayout;

/** Three selectable themes based on the approved dark premium concept. */
public final class ThemeStyle {
    public static final String[] MODES={"Светлая","Тёмная","Как в системе"};
    public static final int RED=0xFFFD3345;
    public static final int NIGHT_BG=0xFF0E1014;
    public static final int NIGHT_CARD=0xFF20242B;
    public static final int DAY_BG=0xFFF5F6F9;
    public static final int DAY_CARD=0xFFFFFFFF;
    private ThemeStyle(){}
    public static boolean isDark(int chosen,boolean systemDark){
        return chosen==1||(chosen==2&&systemDark);
    }
    public static boolean isDark(Context ctx){
        int pref=ctx.getSharedPreferences("design",Context.MODE_PRIVATE).getInt("theme",2);
        boolean system=(ctx.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
        return isDark(pref,system);
    }
    public static void initialize(Activity activity) {
        boolean dark=isDark(activity);
        activity.setTheme(dark ? android.R.style.Theme_Material_NoActionBar : android.R.style.Theme_Material_Light_NoActionBar);
        activity.getWindow().setStatusBarColor(dark?0xFF08090B:0xFFE8EAF0);
        activity.getWindow().setNavigationBarColor(dark?0xFF08090B:0xFFE8EAF0);
        activity.getWindow().getDecorView().setSystemUiVisibility(dark?0:
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }
    public static int text(Context ctx){return isDark(ctx)?Color.WHITE:0xFF1B2028;}
    public static int subdued(Context ctx){return isDark(ctx)?0xFFBBC3D3:0xFF4B5667;}
    public static int background(Context ctx){return isDark(ctx)?NIGHT_BG:DAY_BG;}
    public static int card(Context ctx){return isDark(ctx)?NIGHT_CARD:DAY_CARD;}
    public static GradientDrawable rounded(int fill,int stroke,float radius){
        GradientDrawable d=new GradientDrawable();
        d.setColor(fill);d.setCornerRadius(radius);d.setStroke(1,stroke);return d;
    }
    public static void styleButton(Button b, boolean primary) {
        Context c=b.getContext();
        b.setAllCaps(false);
        b.setTextColor(primary?Color.WHITE:text(c));
        b.setBackground(rounded(primary?RED:card(c),
            primary?0xFFEB4050:(isDark(c)?0xFF3A404C:0xFFD8DDE5),18*c.getResources().getDisplayMetrics().density));
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,
            (int)(49*c.getResources().getDisplayMetrics().density));
        params.setMargins(0,4,0,4);b.setLayoutParams(params);
    }
}

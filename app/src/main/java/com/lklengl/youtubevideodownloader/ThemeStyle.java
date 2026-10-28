package com.lklengl.youtubevideodownloader;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

/** Shared premium graphite/coral design tokens and compact visual components. */
public final class ThemeStyle {
    public static final String[] MODES={"Светлая","Тёмная","Как в системе"};
    public static final int RED=0xFFEF2830;
    public static final int NIGHT_BG=0xFF0D1119;
    public static final int NIGHT_CARD=0xFF272E39;
    public static final int DAY_BG=0xFFF5F6F9;
    public static final int DAY_CARD=0xFFFFFFFF;
    private ThemeStyle(){}
    public static boolean isDark(int selected,boolean darkSystem){
        return selected==1||(selected==2&&darkSystem);
    }
    public static boolean isDark(Context c){
        int value=c.getSharedPreferences("design",Context.MODE_PRIVATE).getInt("theme",2);
        boolean system=(c.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
        return isDark(value,system);
    }
    public static void initialize(Activity activity){
        boolean dark=isDark(activity);
        activity.setTheme(dark?android.R.style.Theme_Material_NoActionBar:
            android.R.style.Theme_Material_Light_NoActionBar);
        activity.getWindow().setStatusBarColor(dark?0xFF090B0E:0xFFE8EAF0);
        activity.getWindow().setNavigationBarColor(dark?0xFF090B0E:0xFFE8EAF0);
        activity.getWindow().getDecorView().setSystemUiVisibility(dark?0:
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }
    public static int text(Context c){return isDark(c)?0xFFF4F5F8:0xFF171C25;}
    public static int subdued(Context c){return isDark(c)?0xFFD3DAE6:0xFF596477;}
    public static int background(Context c){return isDark(c)?NIGHT_BG:DAY_BG;}
    public static int cardColor(Context c){return isDark(c)?NIGHT_CARD:DAY_CARD;}
    public static int border(Context c){return isDark(c)?0xFF5F6D80:0xFFD2D9E3;}
    public static int field(Context c){return isDark(c)?0xFF252C38:0xFFF0F2F6;}
    public static int buttonFill(Context c){return isDark(c)?0xFF353E4D:0xFFEDEFF5;}
    public static int buttonStroke(Context c){return isDark(c)?0xFF687586:0xFFD4DAE4;}
    public static int dp(Context c,float n){return (int)(n*c.getResources().getDisplayMetrics().density+0.5f);}
    public static GradientDrawable rounded(int fill,int stroke,float radius){
        GradientDrawable d=new GradientDrawable();
        d.setColor(fill);d.setCornerRadius(radius);d.setStroke(1,stroke);return d;
    }
    private static Drawable gradient(Context c){
        GradientDrawable result=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{0xFFFF4433,0xFFFC1F20,0xFFB50919});
        result.setCornerRadius(dp(c,11));result.setStroke(dp(c,1),0xFFFFAC9A);
        return result;
    }
    public static void icon(Button b,String icon,int px){
        int color=b.getCurrentTextColor();
        b.setCompoundDrawablesWithIntrinsicBounds(new UiIcons(icon,color,dp(b.getContext(),px)),null,null,null);
        b.setCompoundDrawablePadding(dp(b.getContext(),6));
    }
    /** Compact single-line navigation: unlike default Button padding, this fits small phones. */
    public static void styleTab(Button b,boolean selected,String symbol){
        styleButton(b,selected);
        b.setSingleLine(true);
        b.setMaxLines(1);
        b.setEllipsize(android.text.TextUtils.TruncateAt.END);
        b.setTextSize(12);
        b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(0);
        b.setPadding(dp(b.getContext(),4),0,dp(b.getContext(),4),0);
        icon(b,symbol,14);
        b.setCompoundDrawablePadding(dp(b.getContext(),2));
        b.setGravity(android.view.Gravity.CENTER);
    }
    public static void styleButton(Button b,boolean primary){
        Context c=b.getContext();
        b.setAllCaps(false);b.setTextSize(14);b.setLetterSpacing(0.005f);
        b.setTextColor(primary?Color.WHITE:text(c));
        b.setBackgroundTintList(null);
        Drawable shape;
        if(primary){
            GradientDrawable shine=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x48FFFFFF,0x12FFFFFF,0x00FFFFFF});
            shine.setCornerRadius(dp(c,11));
            android.graphics.drawable.LayerDrawable layers=new android.graphics.drawable.LayerDrawable(
                new Drawable[]{gradient(c),shine});
            layers.setLayerInset(1,dp(c,1),dp(c,1),dp(c,1),dp(c,1));
            shape=layers;
        }else shape=rounded(buttonFill(c),buttonStroke(c),dp(c,11));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(primary?0x44FFFFFF:
            (isDark(c)?0x44FFFFFF:0x33000000)),shape,null));
        b.setElevation(dp(c,primary?2:1));
        if(b.getLayoutParams()==null){
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(c,50));
            p.setMargins(0,dp(c,6),0,dp(c,6));b.setLayoutParams(p);
        }
    }
    public static void styleField(EditText e){
        Context c=e.getContext();
        e.setTextColor(text(c));e.setHintTextColor(subdued(c));e.setTextSize(14);
        e.setSingleLine(true);
        e.setBackground(rounded(field(c),buttonStroke(c),dp(c,10)));
        e.setPadding(dp(c,14),0,dp(c,12),0);
    }
    public static void styleSpinner(Spinner spinner){
        Context c=spinner.getContext();
        spinner.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        spinner.setPadding(dp(c,12),dp(c,5),dp(c,25),dp(c,5));
        spinner.setMinimumHeight(dp(c,50));
    }
    public static LinearLayout card(LinearLayout parent,String title){
        Context c=parent.getContext();
        LinearLayout panel=new LinearLayout(c);panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(c,14),dp(c,12),dp(c,14),dp(c,15));
        panel.setBackground(rounded(cardColor(c),border(c),dp(c,15)));
        panel.setElevation(dp(c,1));
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);
        params.setMargins(0,dp(c,5),0,dp(c,10));parent.addView(panel,params);
        TextView heading=new TextView(c);heading.setText(title);
        heading.setTextSize(16);heading.setTypeface(null,android.graphics.Typeface.BOLD);
        heading.setTextColor(text(c));heading.setPadding(dp(c,3),dp(c,4),0,dp(c,10));
        panel.addView(heading);
        // Titles are separated by spacing and card borders, never progress-like red stripes.
        return panel;
    }
}

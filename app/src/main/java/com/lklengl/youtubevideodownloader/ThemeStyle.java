package com.lklengl.youtubevideodownloader;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.ImageView;
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
    public static final int NIGHT_CARD=Palette051.CARD;
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
        activity.setTheme(dark?R.style.YvdDark:android.R.style.Theme_Material_Light_NoActionBar);
        activity.getWindow().setStatusBarColor(dark?0xFF090B0E:0xFFE8EAF0);
        activity.getWindow().setNavigationBarColor(dark?0xFF090B0E:0xFFE8EAF0);
        activity.getWindow().getDecorView().setSystemUiVisibility(dark?0:
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }
    public static int text(Context c){return isDark(c)?0xFFF4F5F8:0xFF171C25;}
    public static int subdued(Context c){return isDark(c)?0xFFD3DAE6:0xFF596477;}
    public static int background(Context c){return isDark(c)?NIGHT_BG:DAY_BG;}
    public static int cardColor(Context c){return isDark(c)?NIGHT_CARD:DAY_CARD;}
    public static int border(Context c){return isDark(c)?Palette051.BORDER:0xFFD2D9E3;}
    public static int field(Context c){return isDark(c)?Palette051.FIELD:0xFFF0F2F6;}
    public static int buttonFill(Context c){return isDark(c)?Palette051.BUTTON:0xFFEDEFF5;}
    public static int buttonStroke(Context c){return isDark(c)?Palette051.BUTTON_BORDER:0xFFD4DAE4;}
    public static int dp(Context c,float n){return (int)(n*c.getResources().getDisplayMetrics().density+0.5f);}
    public static GradientDrawable rounded(int fill,int stroke,float radius){
        GradientDrawable d=new GradientDrawable();
        d.setColor(fill);d.setCornerRadius(radius);d.setStroke(1,stroke);return d;
    }
    private static Drawable gradient(Context c,boolean outlined){
        GradientDrawable result=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
            new int[]{PremiumPalette.GRADIENT_TOP_LEFT,PremiumPalette.GRADIENT_BOTTOM_RIGHT});
        result.setCornerRadius(dp(c,11));
        if(outlined)result.setStroke(dp(c,1),isDark(c)?0xFFA92A21:0xFFFF9CA4);
        return result;
    }
    private static Drawable plainRounded(Context c,int fill){
        GradientDrawable result=new GradientDrawable();
        result.setColor(fill);result.setCornerRadius(dp(c,11));
        return result;
    }
    /** Popup colors are intentionally distinct from the card and input colors in dark mode. */
    /** Individual item follows the outer rounded popup only at the first and last positions. */
    public static GradientDrawable popupRowBackground(Context c,boolean selected,int position,int count){
        GradientDrawable row=new GradientDrawable();
        row.setColor(popupItemColor(c,selected));
        float r=dp(c,10),top=position==0?r:0,bottom=position==count-1?r:0;
        row.setCornerRadii(new float[]{top,top,top,top,bottom,bottom,bottom,bottom});
        return row;
    }
    public static GradientDrawable popupBackground(Context c){
        GradientDrawable result=rounded(isDark(c)?Palette051.POPUP:field(c),
            isDark(c)?Palette051.POPUP_BORDER:border(c),dp(c,11));
        result.setStroke(dp(c,1),isDark(c)?PremiumPalette.DARK_POPUP_BORDER:border(c));
        result.setPadding(dp(c,1),dp(c,1),dp(c,1),dp(c,1));
        return result;
    }
    public static int popupItemColor(Context c,boolean selected){
        if(!isDark(c))return selected?0xFFE6E7EB:field(c);
        return selected?Palette051.SELECTED:Palette051.POPUP;
    }
    public static AlertDialog.Builder dialog(Activity activity){
        return isDark(activity)?new AlertDialog.Builder(activity,R.style.YvdDialogDark):
            new AlertDialog.Builder(activity);
    }
    public static void styleTab(LinearLayout tab,boolean selected,String symbol,String title){
        Context c=tab.getContext();
        tab.removeAllViews();
        tab.setOrientation(LinearLayout.HORIZONTAL);
        tab.setGravity(android.view.Gravity.CENTER);
        tab.setPadding(dp(c,4),0,dp(c,4),0);
        tab.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF),
            selected?gradient(c,false):plainRounded(c,buttonFill(c)),null));
        tab.setElevation(dp(c,selected?2:1));
        ImageView image=new ImageView(c);
        image.setImageDrawable(new UiIcons(symbol,selected?Color.WHITE:text(c),dp(c,16)));
        tab.addView(image,new LinearLayout.LayoutParams(dp(c,16),dp(c,16)));
        TextView label=new TextView(c);
        label.setText(title);label.setTextSize(13);
        label.setTextColor(selected?Color.WHITE:text(c));
        label.setTypeface(null,android.graphics.Typeface.BOLD);
        label.setSingleLine(true);label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams textParams=new LinearLayout.LayoutParams(-2,-2);
        textParams.setMargins(dp(c,5),0,0,0);
        tab.addView(label,textParams);
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
                new int[]{0x22FFFFFF,0x08FFFFFF,0x00FFFFFF});
            shine.setCornerRadius(dp(c,11));
            android.graphics.drawable.LayerDrawable layers=new android.graphics.drawable.LayerDrawable(
                new Drawable[]{gradient(c,true),shine});
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

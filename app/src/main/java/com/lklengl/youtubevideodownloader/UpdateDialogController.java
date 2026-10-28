package com.lklengl.youtubevideodownloader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.*;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SSH Button Next update-dialog behaviour:
 * no outside/back dismiss while downloading, stable height, explicit cancel only,
 * progress / SHA-256+signer verification / Android installer stages.
 */
public final class UpdateDialogController {
    public interface Install {void launch(File apk);}
    private final Activity activity;
    private final GithubUpdater.Candidate candidate;
    private final Install installer;
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private GithubUpdater.Cancel cancellation;
    private AlertDialog dialog;
    private TextView stage, subtitle, details;
    private ProgressBar progress;
    private boolean busy=false, cancelling=false;
    private Button accept,cancel;

    public UpdateDialogController(Activity activity,GithubUpdater.Candidate candidate,Install installer) {
        this.activity=activity;this.candidate=candidate;this.installer=installer;
    }
    public void show() {
        float density=activity.getResources().getDisplayMetrics().density;
        LinearLayout layout=new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad=(int)(22*density);
        layout.setPadding(pad,(int)(10*density),pad,(int)(12*density));
        layout.setMinimumHeight((int)(174*density));
        subtitle=new TextView(activity);
        subtitle.setText(String.format(java.util.Locale.getDefault(),"Размер APK: %.1f МБ",candidate.size/1000000.0));
        subtitle.setTextSize(15);layout.addView(subtitle);
        stage=new TextView(activity);stage.setText("Готово к скачиванию");
        stage.setTextSize(15);stage.setPadding(0,pad/2,0,pad/3);layout.addView(stage);
        progress=new ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);progress.setProgress(0);layout.addView(progress,new LinearLayout.LayoutParams(-1,(int)(5*density)));
        details=new TextView(activity);details.setText("Нажатия вне окна не отменяют загрузку.");
        details.setTextSize(13);details.setPadding(0,pad/2,0,0);layout.addView(details);
        dialog=new AlertDialog.Builder(activity)
            .setTitle("Обновление "+candidate.tag)
            .setView(layout).setPositiveButton("Скачать и обновить",null)
            .setNegativeButton("Позже",null).create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnShowListener(d->{
            accept=dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            cancel=dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            accept.setOnClickListener(v->startDownload());
            cancel.setOnClickListener(v->{
                if(!busy){dialog.dismiss();executor.shutdownNow();}
                else if(!cancelling && cancellation!=null){
                    cancelling=true;cancel.setEnabled(false);
                    cancel.setText("Отмена загрузки…");
                    stage.setText("Остановка скачивания…");
                    cancellation.cancel();
                }
            });
        });
        dialog.show();
    }
    private void startDownload() {
        if(busy)return;
        busy=true;cancelling=false;cancellation=new GithubUpdater.Cancel();
        accept.setEnabled(false);accept.setText("Подождите…");
        cancel.setText("Отменить");cancel.setEnabled(true);
        stage.setText("Скачивание APK…");details.setText("Скачано: 0%");
        executor.execute(()->{
            try{
                File apk=new GithubUpdater(activity).download(candidate,cancellation,
                    (bytes,total)->activity.runOnUiThread(()->{
                        if(dialog!=null && dialog.isShowing()){
                            int percent=(int)Math.max(0,Math.min(100,bytes*100/Math.max(total,1)));
                            progress.setProgress(percent);details.setText("Скачано: "+percent+"%");
                        }
                    }),()->activity.runOnUiThread(()->{
                        stage.setText("Проверка SHA-256 и подписи APK…");
                        details.setText("Проверка целостности файла");
                        cancel.setEnabled(false);cancel.setText("Подождите…");
                    }));
                cancellation.check();
                activity.runOnUiThread(()->{
                    stage.setText("APK проверен, открываем установщик Android…");
                    details.setText("Проверка завершена успешно.");
                    dialog.dismiss();
                    executor.shutdown();
                    installer.launch(apk);
                });
            }catch(Exception e){
                activity.runOnUiThread(()->{
                    busy=false;
                    if(cancellation.isCancelled()){
                        stage.setText("Загрузка отменена");
                        details.setText("Временный файл удалён.");
                        progress.setProgress(0);
                    }else{
                        stage.setText("Не удалось скачать обновление");
                        details.setText(e.getMessage()==null?"Ошибка сети":e.getMessage());
                    }
                    accept.setEnabled(true);accept.setText("Повторить");
                    cancel.setText("Закрыть");cancel.setEnabled(true);
                    cancelling=false;
                });
            }
        });
    }
    public void close() {
        if(cancellation!=null)cancellation.cancel();
        if(dialog!=null)dialog.dismiss();
        executor.shutdownNow();
    }
}

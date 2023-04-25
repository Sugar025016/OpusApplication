package com.example.opusapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.opusapplication.util.PermissionsUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private TextView titleTextView;
    private TextView total_time_text;
    private TextView current_time_text;
    private ImageButton playButton;
    private ImageButton pauseButton;
    private ImageButton previous_button;
    private ImageButton next_button;
    private ImageButton repeat_status;
    private MediaPlayer mediaPlayer;
    private Handler handler;
    private SeekBar seekBar;
    private int playI;
    private int counter = 0;
    private PermissionsUtil permissionsUtil = new PermissionsUtil(this);;

    private List<Uri> musicItem;
    public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123;

    private MediaSessionCompat mediaSession;
    private String path = "";


    @SuppressLint("Range")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        permissionsUtil.checkPermissions();
        musicItem = new ArrayList<>();
        titleTextView = findViewById(R.id.title_text);
        total_time_text = findViewById(R.id.total_time_text);
        current_time_text = findViewById(R.id.current_time_text);
        playButton = findViewById(R.id.playButton);
        pauseButton = findViewById(R.id.pauseButton);
        previous_button = findViewById(R.id.previous_button);
        next_button = findViewById(R.id.next_button);

        playButton.setOnClickListener(this);
        pauseButton.setOnClickListener(this);
        previous_button.setOnClickListener(this);
        next_button.setOnClickListener(this);

        /** 廣播過濾器，過濾接廣播條件 **/
        IntentFilter filter = new IntentFilter();//監聽
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(mHeadsetReceiver, filter);

        repeat_status = findViewById(R.id.repeat_status);
        repeat_status.setOnClickListener(this);

        mediaSession = new MediaSessionCompat(this, "tag");

        /** 设置 MediaSessionCompat 的回调對象， 監聽耳機播放控制 **/
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                play();
                Toast.makeText(MainActivity.this, "onPlay()", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPause() {
                pause();
                Toast.makeText(MainActivity.this, "onPause()", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSkipToNext() {
                next();
                Toast.makeText(MainActivity.this, "onSkipToNext()", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSkipToPrevious() {
                previous();
                Toast.makeText(MainActivity.this, "onSkipToPrevious()", Toast.LENGTH_SHORT).show();
            }
        });

        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS);
        // 激活 MediaSessionCompat
        mediaSession.setActive(true);

        /** 描述媒体播放的状态(很重要，沒加入會沒動作) **/
        PlaybackStateCompat state = new PlaybackStateCompat.Builder().setActions(
                PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PLAY_PAUSE |
                        PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        ).build();
        mediaSession.setPlaybackState(state);

        /** 進度條監聽 **/
        seekBar = findViewById(R.id.seek_bar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // 如果是由用戶觸發的，則將播放進度設置為 SeekBar 的進度
                if (fromUser) {
                    mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // 開始拖動 SeekBar 時不執行任何操作
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 停止拖動 SeekBar 時不執行任何操作
            }
        });


        /** 檢查權限是否已經被授予 **/
        if (permissionsUtil.checkPermissions()) {
//
            getFile();
        } else {
            // 沒有權限，需要向用戶請求權限
            permissionsUtil.openPermissions();
        }
        setMediaPlayer();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                System.out.println("-----MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE---!!!---"+grantResults.length);
                System.out.println("-----MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE---!!!---"+grantResults[0]);
                System.out.println("-----MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE---!!!---"+ PackageManager.PERMISSION_GRANTED);
                // 如果使用者授予了權限
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    System.out.println("-----PERMISSION_GRANTED---!!!---");
                    // 可以執行訪問外部存儲空間的程式碼
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        Toast.makeText(this, "需要獲取權限，請給予權限", Toast.LENGTH_LONG).show();
                        //開啟應用程式資訊，讓使用者手動給予權限
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "按下拒絕", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(this, "允許權限", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mHeadsetReceiver);
        mediaSession.setCallback(null);
        mediaSession.setActive(false);
        mediaSession.release();
        mediaPlayer.release();
    }

    @Override
    public void onClick(View v) {
        System.out.println("-----------");
        if (!permissionsUtil.checkPermissions()) {
            System.out.println("--------!!!---");
            permissionsUtil.openPermissions();
        }else if(musicItem.size()<=0){
            getFile();
        }

        if (isMusicItemNull()) {
            return;
        }
        switch (v.getId()) {
            case R.id.playButton:
                playOrPause();
                break;
            case R.id.previous_button:
                previous();
                break;
            case R.id.next_button:
                next();
                break;
            case R.id.repeat_status:
                repeatStatus();
                break;
        }
    }

    @SuppressLint("Range")
    private void getFile(){

        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        musicItem.clear();
        Cursor cursor = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
                null,
                null,
                null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                // 取得檔案資訊
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                Log.d("MediaStore", "id: " +id);
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                int albumIdColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
                System.out.println("albumIdColumn: " + albumIdColumn);
                System.out.println("path: " + path);
                System.out.println("title: " + title);
                System.out.println("artist: " + artist);
                File file = new File(path); // 獲取名稱為 path 的公開成員變數對應的 Field 物件

                Uri uri = Uri.fromFile(file);
                musicItem.add(uri);
            }
            cursor.close();
        }
    }

    public void repeatStatus() {
        counter++;
        switch (counter % 3) {
            case 0:
                repeat_status.setImageResource(R.drawable.repeat_all);
                mediaPlayer.setLooping(false);
                break;
            case 1:
                repeat_status.setImageResource(R.drawable.repeat_one);
                mediaPlayer.setLooping(true);
                break;
            case 2:
                repeat_status.setImageResource(R.drawable.repeat_all_off);
                mediaPlayer.setLooping(false);
                break;
        }
    }

    void play() {
        if (mediaPlayer != null) {
            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
                playButton.setImageResource(R.drawable.pause_selector);
            }
        }
    }

    void playOrPause() {
        if (mediaPlayer != null) {
            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
                playButton.setImageResource(R.drawable.pause_selector);
            } else {
                mediaPlayer.pause();
                playButton.setImageResource(R.drawable.play_selector);
            }
        }
    }

    void pause() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                playButton.setImageResource(R.drawable.play_selector);
            }
        }
    }

    public void next() {
        playI++;
        if (playI < musicItem.size()) {

            nextOrPrevious();
        } else {
            playI = musicItem.size() - 1;
            Toast.makeText(this, "這是最後一首", Toast.LENGTH_SHORT).show();
            playButton.setImageResource(R.drawable.pause_selector);
        }

    }

    public void previous() {
        playI--;
        if (playI >= 0) {
            nextOrPrevious();
        } else {
            playI = 0;
            Toast.makeText(this, "這是第一首", Toast.LENGTH_SHORT).show();
        }
    }

    public void nextOrPrevious() {
        setMediaPlayer();
        playOrPause();

    }

    public boolean isMusicItemNull() {
        if (musicItem.size() < 1) {
            Toast.makeText(this, "沒有音樂", Toast.LENGTH_SHORT).show();
            playButton.setImageResource(R.drawable.pause_selector);
            return true;
        }
        return false;
    }


    public void setMediaPlayer() {
        if (isMusicItemNull()) {
            return;
        }

        if (playI < 0) {
            playI = 0;
        }
        if (playI >= musicItem.size()) {
            playI = musicItem.size() - 1;
        }
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        // 使用 MediaPlayer 播放音樂
        Uri resId = musicItem.get(playI);
        mediaPlayer = MediaPlayer.create(this, resId);
        if (counter % 3 == 1) {
            mediaPlayer.setLooping(true);
        }
//        mediaPlayer.setLooping(true);
        seekBar.setMax(mediaPlayer.getDuration());

        /** 監聽音樂播放進度 **/
        handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null) {
                    seekBar.setProgress(mediaPlayer.getCurrentPosition());
                    current_time_text.setText(intToTime(mediaPlayer.getCurrentPosition()));
                    handler.postDelayed(this, 500);
                }
            }
        });

        /** 監聽mediaPlayer播放結束 **/
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                // 音樂播放完畢
                // 在這裡可以執行下一首音樂的播放等操作
                playButton.setImageResource(R.drawable.play_selector);
                playI++;
                handler.removeCallbacksAndMessages(null);

                if (playI >= musicItem.size()) {
                    System.out.println("playI:  " + playI);
                    playI = 0;
                    if (counter % 3 == 0) {
                        System.out.println("counter:  " + counter);

                        nextOrPrevious();
                    } else {
                        setMediaPlayer();
                    }
                } else {
                    nextOrPrevious();

                }
            }
        });
        // 取得音樂檔案名稱
//        titleTextView.setText(getResources().getResourceEntryName(resId));
        titleTextView.setText(resId.getLastPathSegment());
        total_time_text.setText(intToTime(mediaPlayer.getDuration()));
    }


    public static String intToTime(long time) {
        int temp = (int) time / 1000;
        int hh = temp / 3600;
        int mm = (temp % 3600) / 60;
        int ss = (temp % 3600) % 60;
        return (hh < 10 ? ("0" + hh) : hh) + ":" +
                (mm < 10 ? ("0" + mm) : mm) + ":" +
                (ss < 10 ? ("0" + ss) : ss);
    }

    /** 生命週期 在頁面開啟時呼叫 **/
//    @Override
//    protected void onStart() {
//        super.onStart();
//        System.out.println("1111111111onStart");
//        // 激活 MediaSessionCompat
//        mediaSession.setActive(true);
//    }
    /** 生命週期 在頁面關閉時呼叫 **/
//    @Override
//    protected void onStop() {
//        super.onStop();
//        System.out.println("22222222222onStop");
//
//        // 停用 MediaSessionCompat
//        mediaSession.setActive(false);
//    }


    /**
     * 監聽廣播
     **/
    private final BroadcastReceiver mHeadsetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("BroadcastReceiver", "廣播");
            int state;
            if (intent != null && intent.getAction() != null) {
                switch (intent.getAction()) {
                    case AudioManager.ACTION_AUDIO_BECOMING_NOISY:
                        // 耳機已拔出
                        Log.d("AudioManager", "耳機已拔出");
                        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                            pause();
                        }
                        break;
                    case Intent.ACTION_HEADSET_PLUG:
                        state = intent.getIntExtra("state", -1);
                        if (state == 0) {
                            // 耳機已拔出
                            Log.d("Intent", "耳機已拔出");
                            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                                pause();
                            }
                        }
                        break;
                }
            }
        }
    };
}
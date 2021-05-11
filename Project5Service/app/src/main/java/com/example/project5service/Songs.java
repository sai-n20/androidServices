package com.example.project5service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Songs extends Service {

    //Data structures to house required song information
    //Credits to https://free-stock-music.com for the royalty free music used here. All music from MusicCentral comes under the CC-BY license
    List<String> songTitles;
    List<String> artists;
    ArrayList<String> pictures = new ArrayList<>();
    List<String> urls;
    List<String> fileNames;

    @Override
    public void onCreate() {
        super.onCreate();

        //Get values from res directory
        songTitles = Arrays.asList(getResources().getStringArray(R.array.songTitles));
        artists = Arrays.asList(getResources().getStringArray(R.array.artists));
        urls = Arrays.asList(getResources().getStringArray(R.array.urls));
        fileNames = Arrays.asList(getResources().getStringArray(R.array.fileNames));

        //Create base64 representation of the image. This is a compact method to store images using a well established primitive data type
        createImgBase64();

        //Create notification channel and build notification. This is required for a foreground service
        createNotificationChannel();

        Intent notificationIntent = new Intent(this, Songs.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification =
                new Notification.Builder(this, "1")
                        .setContentTitle("MusicCentral")
                        .setContentText("The channel for music player notifications")
                        .setSmallIcon(R.mipmap.ic_launcher_round)
                        .setContentIntent(pendingIntent)
                        .build();

        startForeground(1, notification);
    }

    private void createNotificationChannel() {
        CharSequence name = "MusicCentral";
        String description = "The channel for music player notifications";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel("1", name, importance);
        channel.setDescription(description);

        // Register the channel with the system; can't change the importance
        // or other notification behaviors after this
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }


    //Helper function to convert bitmap to byte array
    public byte[] getBytesFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        return stream.toByteArray();
    }

    //Utility function to create base64 representation of a image resource. Uses bytefrombitmap helper function
    void createImgBase64() {
        for(int i = 0; i < fileNames.size(); i++) {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), getResources().getIdentifier(fileNames.get(i), "drawable", "com.example.project5service"));
            String img = Base64.encodeToString(getBytesFromBitmap(bitmap), Base64.NO_WRAP);
            pictures.add(img);
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {
        return START_STICKY;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    //Shared AIDL
    private final SongService.Stub mBinder = new SongService.Stub() {

        //Return all information about all songs
        public Bundle songList() {
            Bundle bundle = new Bundle();
            bundle.putStringArrayList("songs", new ArrayList<>(songTitles));
            bundle.putStringArrayList("artists", new ArrayList<>(artists));
            bundle.putStringArrayList("pics", new ArrayList<>(pictures));
            bundle.putStringArrayList("urls", new ArrayList<>(urls));
            return bundle;
        }

        //Return all information about specified song
        public List<String> getSpecificSong(int id) {
            ArrayList<String> songArray = new ArrayList<>();
            songArray.add(songTitles.get(id));
            songArray.add(artists.get(id));
            songArray.add(pictures.get(id));
            songArray.add(urls.get(id));
            return songArray;
        }

        //Return URL of specified song
        public String getSongURL(int id) {
            return urls.get(id);
        }

        //Get number of songs stored to create RecyclerView in MusicClient
        public int getSongAmount() { return songTitles.size(); }
    };
}

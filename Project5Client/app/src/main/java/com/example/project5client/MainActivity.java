package com.example.project5client;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.project5service.SongService;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    //Proxy object to communicate with MusicServer
    protected SongService AIDL;

    RecyclerView nameView;
    TextView mStatus;
    Button mButtonBind;
    Button mButtonShow;

    //In the case of get all songs, populate these arrays into the view
    //Credits to https://free-stock-music.com for the royalty free music used here. All music from MusicCentral comes under the CC-BY license
    public List<String> nameList;
    public List<String> artistList;
    public List<String> pictureList;
    public List<String> urlList;

    public MyAdapter adapter;
    MediaPlayer mediaPlayer;

    //Song length field to specify to RecyclerView on how many items to be expected
    public int songLength = 0;

    //Define the listener with a lambda and access the position of the list item from the view to play relevant song
    RVClickListener mListener = (view, position)->{

        TextView song = view.findViewById(R.id.textView);
        TextView artist = view.findViewById(R.id.textView2);
        ImageView image = view.findViewById(R.id.imageView);

        //My implementation loads empty tiles as a placeholder for the songs. Tapping on empty tile populates song info by calling respective service API using proxy
        if(song.getText() == "Tap to download song info") {

            //Set song and artist text
            List<String> songDetails = AIDL.getSpecificSong(position);
            song.setText(songDetails.get(0));
            artist.setText(songDetails.get(1));

            //Decode base64 string back to bitmap and set that for the chosen song
            byte[] decodedString = Base64.decode(songDetails.get(2), Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            image.setImageBitmap(decodedByte);
        }

        //If tile is already populated then next tap starts playing the song..please give it a few seconds to buffer and turn up the volume on the AVD
        else {

            if(mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            //Get URL from service API using proxy
            String url = AIDL.getSongURL(position);

            //Initialize default Android media player and set corresponding attributes
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
            );

            //MP3 file needs to be buffered so better to use prepareAsync() and start playback when media player is ready
            try {
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mp.start();
                    }
                });
                mediaPlayer.setDataSource(url);
                mediaPlayer.prepareAsync();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    //Binder object returned by the service is received in ServiceConnection object, to be used as the proxy
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder iBinder) {
            AIDL = SongService.Stub.asInterface((IBinder) iBinder);
            try{
                //First thing to fill empty tiles of RecyclerView
                songLength = AIDL.getSongAmount();
                initializeRV();
            }catch (RemoteException e){
                e.printStackTrace();
            }
        }

        //Perform graceful closing of playback/removal of RecyclerView upon service disconnection
        @Override
        public void onServiceDisconnected(ComponentName name) {
            try {
                mediaPlayer.stop();
                if (mediaPlayer != null) mediaPlayer.release();
                mediaPlayer = null;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            AIDL = null;
            unbindService(serviceConnection);
            nameList = new ArrayList<>();
            artistList = new ArrayList<>();
            pictureList = new ArrayList<>();
            urlList = new ArrayList<>();
            songLength = 0;

            //Remove RecyclerView by setting all elements to empty list
            adapter = new MyAdapter(nameList, artistList, pictureList, songLength, true, mListener);
            nameView.setAdapter(adapter);
            nameView.removeAllViews();
            mStatus.setText("Service closed unexpectedly");
            mButtonBind.setText("Bind MusicCentral");

            //Button to get all songs to be enabled if not already and to be removed from view
            mButtonShow.setVisibility(View.GONE);
            mButtonShow.setEnabled(true);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nameView = findViewById(R.id.recycler_view);
        mStatus = findViewById(R.id.textView1);
        mButtonBind = findViewById(R.id.button1);
        mButtonShow = findViewById(R.id.button2);
        nameView.setLayoutManager(new LinearLayoutManager(this));
    }

    //Runs when user taps Bind/Unbind MusicCentral service button
    public void onBindCentral(View v) {

        //Service is not bound, send ServiceConnection object and update certain view elements accordingly
        if(AIDL == null) {
            Intent intent = new Intent("com.service.MusicCentral");
            intent.setPackage("com.example.project5service"); // set location
            bindService(intent, serviceConnection, Service.BIND_AUTO_CREATE);
            mButtonBind.setText("Unbind MusicCentral");
            mButtonShow.setVisibility(View.VISIBLE);
        }

        //Steps to be taken on unbinding from MusicCentral. Similar steps ran on onServiceDisconnected()
        else {
            try {
                mediaPlayer.stop();
                if (mediaPlayer != null) mediaPlayer.release();
                mediaPlayer = null;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            AIDL = null;
            unbindService(serviceConnection);
            nameList = new ArrayList<>();
            artistList = new ArrayList<>();
            pictureList = new ArrayList<>();
            urlList = new ArrayList<>();
            songLength = 0;
            adapter = new MyAdapter(nameList, artistList, pictureList, songLength, true, mListener);
            nameView.setAdapter(adapter);
            nameView.removeAllViews();
            mStatus.setText("Service is not bound");
            mButtonBind.setText("Bind MusicCentral");
            mButtonShow.setVisibility(View.GONE);
            mButtonShow.setEnabled(true);
        }
    }

    //Get all songs information from service. Unpack bundle and pass RecyclerView the corresponding arrays
    public void onShowAll(View v) throws RemoteException {
        Bundle songBundle = AIDL.songList();
        nameList = (List<String>) songBundle.get("songs");
        artistList = (List<String>) songBundle.get("artists");
        pictureList = (List<String>) songBundle.get("pics");
        urlList = (List<String>) songBundle.get("urls");
        adapter = new MyAdapter(nameList, artistList, pictureList, songLength,false, mListener);
        nameView.setAdapter(adapter);
        mButtonShow.setEnabled(false);
    }

    //Initialize RecyclerView by creating empty tile placeholders corresponding to number of songs
    void initializeRV() {
        adapter = new MyAdapter(nameList, artistList, pictureList, songLength, true, mListener);
        nameView.setHasFixedSize(true);
        nameView.setAdapter(adapter);
        mStatus.setText("Service bound");
    }

    //Graceful shutdown
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            unbindService(serviceConnection);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


}
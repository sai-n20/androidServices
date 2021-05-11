package com.example.project5client;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.RemoteException;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

    //Create arrays required for functionality
    private List<String> nameList;
    private List<String> artistList;
    private List<String> pictureList;

    //If RecyclerView created for first time on binding, flag determines creation of empty tiles
    private boolean init = true;
    private RVClickListener RVlistener; //listener defined in main activity
    private int songLength;

    /*
    passing in the data lists and the listener defined in the main activity
     */

    public MyAdapter(List<String> songnames, List<String> artistnames,
                     List<String> piclinks, int length, boolean status,
                     RVClickListener listener){
        nameList = songnames;
        artistList = artistnames;
        pictureList = piclinks;
        songLength = length;
        init = status;
        this.RVlistener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        //Inflate view for individual list/grid item defined in XML spec
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View listView = inflater.inflate(R.layout.individual, parent, false);
        ViewHolder viewHolder = new ViewHolder(listView, RVlistener);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        //Empty tiles creation
        if(init) {
            holder.song.setText("Tap to download song info");
            holder.artist.setText("Artist");
        }
        else {
            //Bind data items to view dynamically
            holder.song.setText(nameList.get(position));
            holder.artist.setText(artistList.get(position));
            byte[] decodedString = Base64.decode(pictureList.get(position), Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            holder.image.setImageBitmap(decodedByte);
        }
    }

    //RecyclerView needs this for item generation
    @Override
    public int getItemCount() {
        return songLength;
    }




    /*
        This class creates a wrapper object around a view that contains the layout for
         an individual item in the list. It also implements the onClickListener so each ViewHolder in the list is clickable.
        It's onclick method will call the onClick method of the RVClickListener defined in
        the main activity.
     */
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        //Fields to be populated
        public TextView song;
        public TextView artist;
        public ImageView image;
        private RVClickListener listener;
        private View itemView;


        public ViewHolder(@NonNull View itemView, RVClickListener passedListener) {
            super(itemView);
            song = itemView.findViewById(R.id.textView);
            artist = itemView.findViewById(R.id.textView2);
            image = itemView.findViewById(R.id.imageView);
            this.itemView = itemView;
            this.listener = passedListener;
            itemView.setOnClickListener(this); //set short click listener
        }

        @Override
        public void onClick(View v) {
            try {
                listener.onClick(v, getAdapterPosition());
            } catch (IOException | InterruptedException | RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}

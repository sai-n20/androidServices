// SongService.aidl
package com.example.project5service;

// Declare any non-default types here with import statements

interface SongService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */

    List<String> getSpecificSong(int id);
    String getSongURL(int id);
    Bundle songList();
    int getSongAmount();
}
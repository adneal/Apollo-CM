/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.andrew.apollo.model;

/**
 * A class that represents a song.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class Song {

    /**
     * The unique Id of the song
     */
    public String mSongId;

    /**
     * The song name
     */
    public String mSongName;

    /**
     * The song artist
     */
    public String mArtistName;

    /**
     * The song album
     */
    public String mAlbumName;

    /**
     * The song duration
     */
    public String mDuration;

    /**
     * Constructor of <code>Song</code>
     * 
     * @param songId The Id of the song
     * @param songName The name of the song
     * @param artistName The song artist
     * @param albumName The song album
     * @param duration The duration of a song
     */
    public Song(final String songId, final String songName, final String artistName,
            final String albumName, final String duration) {
        mSongId = new String(songId);
        mSongName = songName;
        mArtistName = artistName;
        mAlbumName = albumName;
        mDuration = duration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (mAlbumName == null ? 0 : mAlbumName.hashCode());
        result = prime * result + (mArtistName == null ? 0 : mArtistName.hashCode());
        result = prime * result + (mDuration == null ? 0 : mDuration.hashCode());
        result = prime * result + (mSongId == null ? 0 : mSongId.hashCode());
        result = prime * result + (mSongName == null ? 0 : mSongName.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Song other = (Song)obj;
        if (mAlbumName == null) {
            if (other.mAlbumName != null) {
                return false;
            }
        } else if (!mAlbumName.equals(other.mAlbumName)) {
            return false;
        }
        if (mArtistName == null) {
            if (other.mArtistName != null) {
                return false;
            }
        } else if (!mArtistName.equals(other.mArtistName)) {
            return false;
        }
        if (mDuration == null) {
            if (other.mDuration != null) {
                return false;
            }
        } else if (!mDuration.equals(other.mDuration)) {
            return false;
        }
        if (mSongId == null) {
            if (other.mSongId != null) {
                return false;
            }
        } else if (!mSongId.equals(other.mSongId)) {
            return false;
        }
        if (mSongName == null) {
            if (other.mSongName != null) {
                return false;
            }
        } else if (!mSongName.equals(other.mSongName)) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return mSongName;
    }
}

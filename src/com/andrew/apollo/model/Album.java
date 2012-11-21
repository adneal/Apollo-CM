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
 * A class that represents an album.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class Album {

    /**
     * The unique Id of the album
     */
    public String mAlbumId;

    /**
     * The name of the album
     */
    public String mAlbumName;

    /**
     * The album artist
     */
    public String mArtistName;

    /**
     * The number of songs in the album
     */
    public String mSongNumber;

    /**
     * The year the album was released
     */
    public String mYear;

    /**
     * Constructor of <code>Album</code>
     * 
     * @param albumId The Id of the album
     * @param albumName The name of the album
     * @param artistName The album artist
     * @param songNumber The number of songs in the album
     * @param albumYear The year the album was released
     */
    public Album(final String albumId, final String albumName, final String artistName,
            final String songNumber, final String albumYear) {
        super();
        mAlbumId = albumId;
        mAlbumName = albumName;
        mArtistName = artistName;
        mSongNumber = songNumber;
        mYear = albumYear;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (mAlbumId == null ? 0 : mAlbumId.hashCode());
        result = prime * result + (mAlbumName == null ? 0 : mAlbumName.hashCode());
        result = prime * result + (mArtistName == null ? 0 : mArtistName.hashCode());
        result = prime * result + (mSongNumber == null ? 0 : mSongNumber.hashCode());
        result = prime * result + (mYear == null ? 0 : mYear.hashCode());
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
        final Album other = (Album)obj;
        if (mAlbumId == null) {
            if (other.mAlbumId != null) {
                return false;
            }
        } else if (!mAlbumId.equals(other.mAlbumId)) {
            return false;
        }
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
        if (mSongNumber == null) {
            if (other.mSongNumber != null) {
                return false;
            }
        } else if (!mSongNumber.equals(other.mSongNumber)) {
            return false;
        }
        if (mYear == null) {
            if (other.mYear != null) {
                return false;
            }
        } else if (!mYear.equals(other.mYear)) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return mAlbumName;
    }

}

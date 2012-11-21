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
 * A class that represents an artist.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class Artist {

    /**
     * The unique Id of the artist
     */
    public String mArtistId;

    /**
     * The artist name
     */
    public String mArtistName;

    /**
     * The number of albums for the artist
     */
    public String mAlbumNumber;

    /**
     * The number of songs for the artist
     */
    public String mSongNumber;

    /**
     * Constructor of <code>Artist</code>
     * 
     * @param artistId The Id of the artist
     * @param artistName The artist name
     * @param songNumber The number of songs for the artist
     * @param albumNumber The number of albums for the artist
     */
    public Artist(final String artistId, final String artistName, final String songNumber,
            final String albumNumber) {
        super();
        mArtistId = artistId;
        mArtistName = artistName;
        mSongNumber = songNumber;
        mAlbumNumber = albumNumber;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (mAlbumNumber == null ? 0 : mAlbumNumber.hashCode());
        result = prime * result + (mArtistId == null ? 0 : mArtistId.hashCode());
        result = prime * result + (mArtistName == null ? 0 : mArtistName.hashCode());
        result = prime * result + (mSongNumber == null ? 0 : mSongNumber.hashCode());
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
        final Artist other = (Artist)obj;
        if (mAlbumNumber == null) {
            if (other.mAlbumNumber != null) {
                return false;
            }
        } else if (!mAlbumNumber.equals(other.mAlbumNumber)) {
            return false;
        }
        if (mArtistId == null) {
            if (other.mArtistId != null) {
                return false;
            }
        } else if (!mArtistId.equals(other.mArtistId)) {
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
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return mArtistName;
    }

}

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
 * A class that represents a playlist.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class Playlist {

    /**
     * The unique Id of the playlist
     */
    public String mPlaylistId;

    /**
     * The playlist name
     */
    public String mPlaylistName;

    /**
     * Constructor of <code>Genre</code>
     * 
     * @param playlistId The Id of the playlist
     * @param playlistName The playlist name
     */
    public Playlist(final String playlistId, final String playlistName) {
        super();
        mPlaylistId = playlistId;
        mPlaylistName = playlistName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (mPlaylistId == null ? 0 : mPlaylistId.hashCode());
        result = prime * result + (mPlaylistName == null ? 0 : mPlaylistName.hashCode());
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
        final Playlist other = (Playlist)obj;
        if (mPlaylistId == null) {
            if (other.mPlaylistId != null) {
                return false;
            }
        } else if (!mPlaylistId.equals(other.mPlaylistId)) {
            return false;
        }
        if (mPlaylistName == null) {
            if (other.mPlaylistName != null) {
                return false;
            }
        } else if (!mPlaylistName.equals(other.mPlaylistName)) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return mPlaylistName;
    }

}

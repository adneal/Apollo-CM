/**
 * 
 */

package com.andrew.apollo;

/**
 * @author Andrew Neal
 */
public interface Constants {

    // Last.fm API
    public String LASTFM_API_KEY = "0bec3f7ec1f914d7c960c12a916c8fb3";

    // Tab titles
    public String[] mTitles = {
            "RECENT", "ARTISTS", "ALBUMS", "SONGS", "PLAYLISTS", "GENRES"
    };

    // SharedPreferences
    public String APOLLO = "Apollo", APOLLO_PREFERENCES = "apollopreferences",
            ARTIST_IMAGE = "artistimage", ARTIST_IMAGE_ORIGINAL = "artistimageoriginal",
            ALBUM_IMAGE = "albumimage", ARTIST_KEY = "artist", ALBUM_KEY = "album",
            GENRE_KEY = "genres", ARTIST_ID = "artistid", NUMWEEKS = "numweeks",
            PLAYLIST_NAME_FAVORITES = "Favorites", PLAYLIST_NAME = "playlist",
            THEME_PACKAGE_NAME = "themePackageName", THEME_DESCRIPTION = "themeDescription",
            THEME_PREVIEW = "themepreview", THEME_TITLE = "themeTitle";

    // Bundle & Intent type
    public String MIME_TYPE = "mimetype", INTENT_ACTION = "action", DATA_SCHEME = "file";

    // Storage Volume
    public String EXTERNAL = "external";

    // Playlists
    public final static long PLAYLIST_UNKNOWN = -1, PLAYLIST_ALL_SONGS = -2, PLAYLIST_QUEUE = -3,
            PLAYLIST_NEW = -4, PLAYLIST_FAVORITES = -5, PLAYLIST_RECENTLY_ADDED = -6;

    // Genres
    public final static String[] GENRES_DB = {
            "Blues", "Classic Rock", "Country", "Dance", "Disco", "Funk", "Grunge", "Hip-Hop",
            "Jazz", "Metal", "New Age", "Oldies", "Other", "Pop", "R&B", "Rap", "Reggae", "Rock",
            "Techno", "Industrial", "Alternative", "Ska", "Death Metal", "Pranks", "Soundtrack",
            "Euro-Techno", "Ambient", "Trip-Hop", "Vocal", "Jazz+Funk", "Fusion", "Trance",
            "Classical", "Instrumental", "Acid", "House", "Game", "Sound Clip", "Gospel", "Noise",
            "AlternRock", "Bass", "Soul", "Punk", "Space", "Meditative", "Instrumental Pop",
            "Instrumental Rock", "Ethnic", "Gothic", "Darkwave", "Techno-Industrial", "Electronic",
            "Pop-Folk", "Eurodance", "Dream", "Southern Rock", "Comedy", "Cult", "Gangsta",
            "Top 40", "Christian Rap", "Pop/Funk", "Jungle", "Native American", "Cabaret",
            "New Wave", "Psychedelic", "Rave", "Showtunes", "Trailer", "Lo-Fi", "Tribal",
            "Acid Punk", "Acid Jazz", "Polka", "Retro", "Musical", "Rock & Roll", "Hard Rock",
            "Folk", "Folk-Rock", "National Folk", "Swing", "Fast Fusion", "Bebob", "Latin",
            "Revival", "Celtic", "Bluegrass", "Avantgarde", "Gothic Rock", "Progressive Rock",
            "Psychedelic Rock", "Symphonic Rock", "Slow Rock", "Big Band", "Chorus",
            "Easy Listening", "Acoustic", "Humour", "Speech", "Chanson", "Opera", "Chamber Music",
            "Sonata", "Symphony", "Booty Bass", "Primus", "Porn Groove", "Satire", "Slow Jam",
            "Club", "Tango", "Samba", "Folklore", "Ballad", "Power Ballad", "Rhythmic Soul",
            "Freestyle", "Duet", "Punk Rock", "Drum Solo", "A capella", "Euro-House", "Dance Hall",
            "Goa", "Drum & Bass", "Club-House", "Hardcore", "Terror", "Indie", "Britpop",
            "Negerpunk", "Polsk Punk", "Beat", "Christian Gangsta Rap", "Heavy Metal",
            "Black Metal", "Crossover", "Contemporary Christian", "Christian Rock ", "Merengue",
            "Salsa", "Thrash Metal", "Anime", "JPop", "Synthpop"
    };

    // Theme item type
    public final static int THEME_ITEM_BACKGROUND = 0, THEME_ITEM_FOREGROUND = 1;

    public final static String INTENT_ADD_TO_PLAYLIST = "com.andrew.apollo.ADD_TO_PLAYLIST",
            INTENT_PLAYLIST_LIST = "playlistlist",
            INTENT_CREATE_PLAYLIST = "com.andrew.apollo.CREATE_PLAYLIST",
            INTENT_RENAME_PLAYLIST = "com.andrew.apollo.RENAME_PLAYLIST",
            INTENT_KEY_RENAME = "rename", INTENT_KEY_DEFAULT_NAME = "default_name";

}

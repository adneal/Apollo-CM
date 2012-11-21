
package com.andrew.apollo.lyrics;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

import java.io.File;
import java.io.IOException;

public class OfflineLyricsProvider implements LyricsProvider {

    private File mAudioFile;

    /**
     * Constructor of <code>OfflineLyricsProvider</code>
     * 
     * @param filePath The path to save the lyrics
     */
    public OfflineLyricsProvider(final String filePath) {
        setTrackFile(filePath);
    }

    /**
     * @param path The file to save our {@link File}
     */
    public void setTrackFile(final String path) {
        if (path == null) {
            return;
        }
        mAudioFile = new File(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLyrics(final String artist, final String song) {
        String lyrics = null;
        try {
            if (mAudioFile == null) {
                return null;
            }
            if (mAudioFile.exists()) {
                // Use jAudioTagger library to get the file's lyrics
                final AudioFile file = AudioFileIO.read(mAudioFile);
                final Tag tag = file.getTag();
                lyrics = tag.getFirst(FieldKey.LYRICS);
            }
        } catch (final ReadOnlyFileException e) {
            e.printStackTrace();
        } catch (final CannotReadException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        } catch (final TagException e) {
            e.printStackTrace();
        } catch (final InvalidAudioFrameException e) {
            e.printStackTrace();
        } catch (final UnsupportedOperationException e) {
            e.printStackTrace();
        }
        return lyrics;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProviderName() {
        return "File metadata";
    }

    /**
     * @param lyrics The lyrics to save
     * @param filePath The path to save them
     */
    public static void saveLyrics(final String lyrics, final String filePath) {
        try {
            final File file = new File(filePath);
            if (file.exists()) {
                // Use jAudioTagger library to set the file's lyrics
                final AudioFile audioFile = AudioFileIO.read(file);
                final Tag tag = audioFile.getTag();
                tag.setField(FieldKey.LYRICS, lyrics);
                audioFile.commit();
            }
        } catch (final ReadOnlyFileException e) {
            e.printStackTrace();
        } catch (final CannotReadException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        } catch (final TagException e) {
            e.printStackTrace();
        } catch (final InvalidAudioFrameException e) {
            e.printStackTrace();
        } catch (final CannotWriteException e) {
            e.printStackTrace();
        } catch (final NullPointerException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param filePath The path to the lyrics we're deleting
     */
    public static void deleteLyrics(final String filePath) {
        try {
            final File file = new File(filePath);
            if (file.exists()) {
                // Use jAudioTagger library to delete the file's lyrics
                final AudioFile audioFile = AudioFileIO.read(file);
                final Tag tag = audioFile.getTag();
                tag.deleteField(FieldKey.LYRICS);
                audioFile.commit();
            }
        } catch (final ReadOnlyFileException e) {
            e.printStackTrace();
        } catch (final CannotReadException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        } catch (final TagException e) {
            e.printStackTrace();
        } catch (final InvalidAudioFrameException e) {
            e.printStackTrace();
        } catch (final CannotWriteException e) {
            e.printStackTrace();
        }
    }
}

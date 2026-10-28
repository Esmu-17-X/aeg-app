package com.esmu.os;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

final class Music {

    private static MediaPlayer player;

    static JSONObject list(Context ctx) throws Exception {
        JSONObject out = new JSONObject();
        JSONArray arr = new JSONArray();

        ContentResolver cr = ctx.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String[] cols = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION
        };

        try (Cursor c = cr.query(uri, cols,
                MediaStore.Audio.Media.IS_MUSIC + " != 0",
                null,
                MediaStore.Audio.Media.TITLE + " ASC")) {
            if (c != null) {
                int iId       = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int iTitle    = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int iArtist   = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int iAlbum    = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                int iDuration = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);

                while (c.moveToNext() && arr.length() < 2000) {
                    long id = c.getLong(iId);
                    Uri fileUri = Uri.withAppendedPath(uri, String.valueOf(id));
                    JSONObject o = new JSONObject();
                    o.put("id", id);
                    o.put("uri", fileUri.toString());
                    o.put("title", safe(c.getString(iTitle), "Unknown title"));
                    o.put("artist", safe(c.getString(iArtist), "Unknown artist"));
                    o.put("album", safe(c.getString(iAlbum), "Unknown album"));
                    o.put("duration", c.getLong(iDuration));
                    arr.put(o);
                }
            }
        }
        out.put("items", arr);
        return out;
    }

    static JSONObject play(Context ctx, String uri) throws Exception {
        if (uri == null || uri.isEmpty()) throw new Exception("no track");
        stopInternal();
        player = new MediaPlayer();
        player.setDataSource(ctx, Uri.parse(uri));
        player.prepare();
        player.start();
        return new JSONObject();
    }

    static JSONObject pause() {
        try { if (player != null && player.isPlaying()) player.pause(); } catch (Exception ignored) {}
        return new JSONObject();
    }

    static JSONObject resume() {
        try { if (player != null) player.start(); } catch (Exception ignored) {}
        return new JSONObject();
    }

    static JSONObject stop() {
        stopInternal();
        return new JSONObject();
    }

    private static void stopInternal() {
        try { if (player != null) { player.stop(); player.release(); } } catch (Exception ignored) {}
        player = null;
    }

    static JSONObject seek(int ms) {
        try { if (player != null) player.seekTo(ms); } catch (Exception ignored) {}
        return new JSONObject();
    }

    static JSONObject position() {
        JSONObject o = new JSONObject();
        try {
            o.put("position", player != null ? player.getCurrentPosition() : 0);
            o.put("playing", player != null && player.isPlaying());
        } catch (Exception ignored) {}
        return o;
    }

    static JSONObject dataUrl(Context ctx, String uri) throws Exception {
        try (InputStream in = ctx.getContentResolver().openInputStream(Uri.parse(uri))) {
            if (in == null) throw new Exception("cannot open " + uri);
            byte[] bytes = readAll(in);
            String b64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
            JSONObject o = new JSONObject();
            o.put("url", "data:audio/mpeg;base64," + b64);
            return o;
        }
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        return bos.toByteArray();
    }

    private static String safe(String s, String fallback) {
        return (s == null || s.trim().isEmpty() || "<unknown>".equals(s)) ? fallback : s;
    }
}

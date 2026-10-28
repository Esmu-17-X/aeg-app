package com.esmu.os;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

final class Perms {

    private static final Map<String, String[]> MAP = new HashMap<>();
    static {
        MAP.put("storage", new String[]{ Build.VERSION.SDK_INT >= 33
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE });
        MAP.put("camera", new String[]{ Manifest.permission.CAMERA });
        MAP.put("mic", new String[]{ Manifest.permission.RECORD_AUDIO });
        MAP.put("location", new String[]{ Manifest.permission.ACCESS_FINE_LOCATION,
                                          Manifest.permission.ACCESS_COARSE_LOCATION });
        MAP.put("contacts", new String[]{ Manifest.permission.READ_CONTACTS });
        MAP.put("notifications", Build.VERSION.SDK_INT >= 33
                ? new String[]{ Manifest.permission.POST_NOTIFICATIONS }
                : new String[0]);
        MAP.put("music", Build.VERSION.SDK_INT >= 33
                ? new String[]{ Manifest.permission.READ_MEDIA_AUDIO }
                : new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE });
    }

    private static MainActivity host;
    private static String pending;

    static void attach(MainActivity a) { host = a; }

    static JSONObject status(Context ctx) {
        JSONObject o = new JSONObject();
        try {
            for (String key : MAP.keySet()) o.put(key, granted(ctx, key));
            if (Build.VERSION.SDK_INT >= 30) {
                boolean allFiles = Environment.isExternalStorageManager();
                o.put("storage", allFiles || granted(ctx, "storage"));
            }
        } catch (Exception ignored) {}
        return o;
    }

    static boolean granted(Context ctx, String key) {
        String[] req = MAP.get(key);
        if (req == null) return false;
        if (req.length == 0) return true;
        for (String p : req) {
            if (ContextCompat.checkSelfPermission(ctx, p) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

    static JSONObject request(String name, int requestCode) {
        JSONObject o = new JSONObject();
        try {
            if (name == null) { o.put("pending", false); o.put("note", "unknown permission"); return o; }

            if ("storage".equals(name) && Build.VERSION.SDK_INT >= 30
                    && !Environment.isExternalStorageManager()) {
                Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                i.setData(Uri.parse("package:" + host.getPackageName()));
                try { host.startActivity(i); } catch (Exception e) {
                    host.startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
                }
                o.put("pending", true);
                o.put("note", "Grant \"All files access\" for Esmu OS in the screen that just opened.");
                return o;
            }

            if (granted(host, name)) { o.put("pending", false); return o; }

            String[] req = MAP.get(name);
            if (req == null || req.length == 0) { o.put("pending", false); return o; }
            pending = name;
            ActivityCompat.requestPermissions(host, req, requestCode);
            o.put("pending", true);
            return o;
        } catch (Exception e) {
            try { o.put("pending", false); o.put("note", e.getMessage()); } catch (Exception ignored) {}
            return o;
        }
    }

    static void result(String[] perms, int[] res) {
        boolean ok = true;
        for (int r : res) if (r != PackageManager.PERMISSION_GRANTED) ok = false;
        String type = pending; pending = null;
        if (host != null) host.notifyJsPermission(type, ok);
    }

    static final int REQ = 7001;
}

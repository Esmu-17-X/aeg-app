package com.esmu.os;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;

import org.json.JSONObject;

import java.util.Locale;

final class DeviceInfo {

    static JSONObject battery(Context ctx) {
        JSONObject o = new JSONObject();
        try {
            IntentFilter f = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent i = ctx.registerReceiver(null, f);
            int level = i != null ? i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
            int scale = i != null ? i.getIntExtra(BatteryManager.EXTRA_SCALE, 100) : 100;
            int status = i != null ? i.getIntExtra(BatteryManager.EXTRA_STATUS, -1) : -1;
            int pct = (int) Math.round(level * 100.0 / scale);
            boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL;
            o.put("level", pct);
            o.put("charging", charging);
        } catch (Exception e) {
            try { o.put("level", 0); o.put("charging", false); } catch (Exception ignored) {}
        }
        return o;
    }

    static JSONObject info(Context ctx) {
        JSONObject o = new JSONObject();
        try {
            o.put("manufacturer", capitalize(Build.MANUFACTURER));
            o.put("model", Build.MODEL);
            o.put("android", Build.VERSION.RELEASE);
            o.put("sdk", Build.VERSION.SDK_INT);
            o.put("locale", Locale.getDefault().toString());
            o.put("battery", battery(ctx));

            StatFs fs = new StatFs(Environment.getDataDirectory().getPath());
            long total = fs.getBlockCountLong() * fs.getBlockSizeLong();
            long free  = fs.getAvailableBlocksLong() * fs.getBlockSizeLong();
            o.put("totalBytes", total);
            o.put("freeBytes", free);
        } catch (Exception ignored) {}
        return o;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}

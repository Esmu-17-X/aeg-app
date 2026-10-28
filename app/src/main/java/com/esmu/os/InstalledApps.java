package com.esmu.os;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;

final class InstalledApps {

    static JSONObject list(Context ctx, boolean icons) {
        JSONObject out = new JSONObject();
        JSONArray arr = new JSONArray();
        try {
            PackageManager pm = ctx.getPackageManager();
            Intent main = new Intent(Intent.ACTION_MAIN, null);
            main.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> apps = pm.queryIntentActivities(main, 0);
            Collections.sort(apps, new ResolveInfo.DisplayNameComparator(pm));
            for (ResolveInfo ri : apps) {
                JSONObject o = new JSONObject();
                o.put("label", String.valueOf(ri.loadLabel(pm)));
                o.put("pkg", ri.activityInfo.packageName);
                if (icons) {
                    try {
                        Drawable d = ri.loadIcon(pm);
                        String b64 = drawableToBase64Png(d, 96);
                        if (b64 != null) o.put("icon", "data:image/png;base64," + b64);
                    } catch (Exception ignored) {}
                }
                arr.put(o);
            }
        } catch (Exception ignored) {}
        try { out.put("items", arr); } catch (Exception ignored) {}
        return out;
    }

    static JSONObject launch(Context ctx, String pkg) throws Exception {
        PackageManager pm = ctx.getPackageManager();
        Intent i = pm.getLaunchIntentForPackage(pkg);
        if (i == null) throw new Exception("No launchable activity for " + pkg);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(i);
        return new JSONObject();
    }

    private static String drawableToBase64Png(Drawable d, int size) {
        try {
            Bitmap bmp;
            if (d instanceof BitmapDrawable && ((BitmapDrawable) d).getBitmap() != null) {
                bmp = ((BitmapDrawable) d).getBitmap();
            } else {
                bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(bmp);
                d.setBounds(0, 0, size, size);
                d.draw(c);
            }
            Bitmap scaled = Bitmap.createScaledBitmap(bmp, size, size, true);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.PNG, 100, bos);
            return Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }
}

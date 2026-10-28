package com.esmu.os;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Vibrator;
import android.provider.Settings;
import android.webkit.JavascriptInterface;

import org.json.JSONObject;

public class EsmuBridge {

    private final MainActivity host;

    EsmuBridge(MainActivity host) { this.host = host; }

    @JavascriptInterface
    public void postMessage(String raw) {
        long id = -1;
        try {
            JSONObject m = new JSONObject(raw);
            id = m.optLong("id", -1);
            String method = m.optString("method", "");
            JSONObject args = m.optJSONObject("args");
            if (args == null) args = new JSONObject();

            JSONObject result = route(method, args);
            host.replyToJs(id, Reply.ok(id, result));
        } catch (Exception e) {
            host.replyToJs(id, Reply.err(id, e.getMessage()));
        }
    }

    private JSONObject route(String method, JSONObject args) throws Exception {
        Context ctx = host;

        switch (method) {
            case "perm.status":    return Perms.status(ctx);
            case "perm.request":   return Perms.request(args.optString("name", null), Perms.REQ);

            case "device.info":    return DeviceInfo.info(ctx);
            case "battery":        return DeviceInfo.battery(ctx);

            case "fs.list":        return PhoneFs.list(args.optString("path", ""));
            case "fs.read":        return PhoneFs.read(args.optString("path", ""), args.optInt("max", 0));
            case "fs.write":       return PhoneFs.write(args.optString("path", ""), args.optString("text", ""));
            case "fs.writeBase64": return PhoneFs.writeBase64(args.optString("path", ""), args.optString("base64", ""));
            case "fs.dataUrl":     return PhoneFs.dataUrl(args.optString("path", ""));
            case "fs.mkdir":       return PhoneFs.mkdir(args.optString("path", ""));
            case "fs.delete":      return PhoneFs.delete(args.optString("path", ""));

            case "apps.list":      return InstalledApps.list(ctx, args.optBoolean("icons", false));
            case "apps.launch":    return InstalledApps.launch(ctx, args.optString("pkg", ""));

            case "contacts.list":  return Contacts.list(ctx, args.optString("query", ""), args.optInt("limit", 200));

            case "music.list":     return Music.list(ctx);
            case "music.play":     return Music.play(ctx, args.optString("uri", ""));
            case "music.pause":    return Music.pause();
            case "music.resume":   return Music.resume();
            case "music.stop":     return Music.stop();
            case "music.seek":     return Music.seek(args.optInt("ms", 0));
            case "music.position": return Music.position();
            case "music.dataUrl":  return Music.dataUrl(ctx, args.optString("uri", ""));

            case "notify":         return Notify.send(ctx, args.optString("title", "Esmu"), args.optString("text", ""));
            case "open.url":       return openUrl(args.optString("url", ""));
            case "share":          return share(args.optString("text", ""));
            case "vibrate":        return vibrate(args.optInt("ms", 150));
            case "settings.open":  return openSettings(args.optString("page", "main"));
            case "keepAwake":      return keepAwake(args.optBoolean("on", false));

            default:
                throw new Exception("unknown method: " + method);
        }
    }

    private JSONObject openUrl(String url) throws Exception {
        if (url == null || url.isEmpty()) throw new Exception("empty url");
        Intent i;
        if (url.startsWith("tel:")) i = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
        else if (url.startsWith("geo:")) i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        else i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        host.startActivity(i);
        return new JSONObject();
    }

    private JSONObject share(String text) throws Exception {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, text == null ? "" : text);
        Intent chooser = Intent.createChooser(i, "Share via");
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        host.startActivity(chooser);
        return new JSONObject();
    }

    private JSONObject vibrate(int ms) {
        try {
            Vibrator v = (Vibrator) host.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) v.vibrate(Math.max(10, Math.min(ms, 3000)));
        } catch (Exception ignored) {}
        return new JSONObject();
    }

    private JSONObject openSettings(String page) throws Exception {
        String action;
        switch (page == null ? "main" : page) {
            case "wifi":       action = Settings.ACTION_WIFI_SETTINGS; break;
            case "bluetooth":  action = Settings.ACTION_BLUETOOTH_SETTINGS; break;
            case "display":    action = Settings.ACTION_DISPLAY_SETTINGS; break;
            case "sound":      action = Settings.ACTION_SOUND_SETTINGS; break;
            case "battery":    action = Intent.ACTION_POWER_USAGE_SUMMARY; break;
            case "location":   action = Settings.ACTION_LOCATION_SOURCE_SETTINGS; break;
            case "apps":       action = Settings.ACTION_APPLICATION_SETTINGS; break;
            case "home":       action = Settings.ACTION_HOME_SETTINGS; break;
            default:           action = Settings.ACTION_SETTINGS; break;
        }
        Intent i = new Intent(action);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        host.startActivity(i);
        return new JSONObject();
    }

    private JSONObject keepAwake(boolean on) {
        host.runOnUiThread(() -> {
            if (on) host.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            else host.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        });
        return new JSONObject();
    }
}

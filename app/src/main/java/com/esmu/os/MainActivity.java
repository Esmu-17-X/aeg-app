package com.esmu.os;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private WebView web;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Perms.attach(this);

        getWindow().setStatusBarColor(Color.parseColor("#05070f"));
        getWindow().setNavigationBarColor(Color.parseColor("#05070f"));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setGeolocationEnabled(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);

        // FIX 1: Disable hardware acceleration to fix touch glitches
        web.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        // FIX 2: Force WebView to handle all touch events directly
        web.setOnTouchListener((v, event) -> {
            v.onTouchEvent(event);
            return true;
        });

        web.addJavascriptInterface(new EsmuBridge(this), "Android");

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(@NonNull ConsoleMessage m) {
                android.util.Log.e("EsmuError", m.message() + " @ line " + m.lineNumber());
                return true;
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> request.grant(request.getResources()));
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback cb) {
                cb.invoke(origin, true, false);
            }
        });

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, @NonNull WebResourceRequest r) {
                String url = r.getUrl().toString();
                if (url.startsWith("file://") || url.startsWith("about:")) return false;
                try {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                } catch (Exception ignored) {}
                return true;
            }

            @Override
            public void onPageFinished(WebView v, String url) {
                injectShim(v);
            }
        });

        web.loadUrl("file:///android_asset/esmu-os.html");
        setContentView(web);
    }

    private void injectShim(WebView v) {
        String js =
            "javascript:(function(){" +
                "if(window.EsmuNative)return;" +
                "window.EsmuNative={" +
                    "postMessage:function(str){" +
                        "try{Android.postMessage(String(str))}" +
                        "catch(e){console.error('Bridge error:', e)}" +
                    "}," +
                    "onmessage:null" +
                "};" +
                "document.body.style.touchAction='manipulation';" + // FIX 3: Force CSS touch
            "})();";
        v.evaluateJavascript(js, null);
    }

    void replyToJs(long id, String json) {
        String js = "javascript:(function(){try{if(window.EsmuNative&&window.EsmuNative.onmessage)" +
                "window.EsmuNative.onmessage({data:" + jsString(json) + "})}catch(e){console.error('Reply error:', e)}})();";
        runOnUiThread(() -> web.evaluateJavascript(js, null));
    }

    void notifyJsPermission(String type, boolean granted) {
        if (!granted && type != null) {
            runOnUiThread(() -> Toast.makeText(this, "Permission denied: " + type, Toast.LENGTH_SHORT).show());
        }
    }

    private static String jsString(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                default:
                    if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
                    else b.append(c);
            }
        }
        return b.append('"').toString();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Perms.result(permissions, grantResults);
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript(
            "javascript:(function(){try{return window.__esmuBack&&window.__esmuBack()?'1':'0'}catch(e){return '0'}})()",
            value -> {
                if (!"\"1\"".equals(value) && !"1".equals(value)) {
                    MainActivity.super.onBackPressed();
                }
            });
    }

    @Override protected void onResume()  { super.onResume();  web.onResume();  }
    @Override protected void onPause()   { web.onPause();     super.onPause(); }
    @Override protected void onDestroy() {
        if (web != null) {
            web.removeJavascriptInterface("Android");
            web.destroy();
        }
        super.onDestroy();
    }
}

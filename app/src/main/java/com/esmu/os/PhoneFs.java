package com.esmu.os;

import android.os.Environment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

final class PhoneFs {

    private static File root() {
        File ext = Environment.getExternalStorageDirectory();
        return ext != null ? ext : new File("/sdcard");
    }

    private static File resolve(String rel) {
        File base = root();
        if (rel == null || rel.isEmpty() || "/".equals(rel)) return base;
        File f = new File(base, rel).getAbsoluteFile();
        String basePath = base.getAbsolutePath();
        String p = f.getAbsolutePath();
        if (!p.startsWith(basePath)) throw new SecurityException("path escapes the phone root");
        return f;
    }

    static JSONObject list(String rel) throws Exception {
        File dir = resolve(rel);
        if (!dir.exists()) throw new Exception("No such folder: " + rel);
        if (!dir.isDirectory()) throw new Exception("Not a folder: " + rel);

        File[] kids = dir.listFiles();
        List<File> arr = new ArrayList<>();
        if (kids != null) arr.addAll(Arrays.asList(kids));
        arr.sort(Comparator
                .comparing((File f) -> !f.isDirectory())
                .thenComparing(f -> f.getName().toLowerCase()));

        JSONArray items = new JSONArray();
        for (File f : arr) {
            JSONObject o = new JSONObject();
            o.put("name", f.getName());
            o.put("dir", f.isDirectory());
            o.put("size", f.isDirectory() ? 0 : f.length());
            items.put(o);
        }
        JSONObject out = new JSONObject();
        out.put("path", rel == null ? "" : rel);
        out.put("root", root().getAbsolutePath());
        out.put("items", items);
        return out;
    }

    static JSONObject read(String rel, int max) throws Exception {
        File f = resolve(rel);
        if (!f.exists()) throw new Exception("No such file: " + rel);
        if (f.isDirectory()) throw new Exception("That is a folder");
        long len = f.length();
        byte[] bytes;
        boolean truncated = false;
        if (max > 0 && len > max) {
            try (FileInputStream in = new FileInputStream(f)) {
                bytes = new byte[max];
                int got = in.read(bytes);
                if (got < max) bytes = Arrays.copyOf(bytes, Math.max(0, got));
            }
            truncated = true;
        } else {
            bytes = Files.readAllBytes(f.toPath());
        }
        JSONObject o = new JSONObject();
        o.put("text", new String(bytes, StandardCharsets.UTF_8));
        o.put("truncated", truncated);
        return o;
    }

    static JSONObject write(String rel, String text) throws Exception {
        File f = resolve(rel);
        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(text == null ? new byte[0] : text.getBytes(StandardCharsets.UTF_8));
        }
        return new JSONObject();
    }

    static JSONObject writeBase64(String rel, String b64) throws Exception {
        File f = resolve(rel);
        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        byte[] bytes = Base64.getDecoder().decode(b64);
        try (FileOutputStream out = new FileOutputStream(f)) { out.write(bytes); }
        return new JSONObject();
    }

    static JSONObject dataUrl(String rel) throws Exception {
        File f = resolve(rel);
        if (!f.exists()) throw new Exception("No such file: " + rel);
        byte[] bytes = Files.readAllBytes(f.toPath());
        String mime = guessMime(f.getName());
        String b64 = Base64.getEncoder().encodeToString(bytes);
        JSONObject o = new JSONObject();
        o.put("url", "data:" + mime + ";base64," + b64);
        return o;
    }

    static JSONObject mkdir(String rel) throws Exception {
        File f = resolve(rel);
        if (!f.mkdirs() && !f.isDirectory()) throw new Exception("Could not create folder");
        return new JSONObject();
    }

    static JSONObject delete(String rel) throws Exception {
        File f = resolve(rel);
        if (!f.exists()) throw new Exception("No such file or folder");
        deleteRecursive(f);
        return new JSONObject();
    }

    private static void deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] k = f.listFiles();
            if (k != null) for (File c : k) deleteRecursive(c);
        }
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }

    private static String guessMime(String name) {
        String n = name.toLowerCase();
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".gif")) return "image/gif";
        if (n.endsWith(".webp")) return "image/webp";
        if (n.endsWith(".bmp")) return "image/bmp";
        return "application/octet-stream";
    }
}

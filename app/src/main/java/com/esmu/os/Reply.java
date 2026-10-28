package com.esmu.os;

import org.json.JSONObject;

final class Reply {
    static String ok(long id, JSONObject result) {
        JSONObject o = new JSONObject();
        try { o.put("id", id); o.put("result", result == null ? new JSONObject() : result); }
        catch (Exception ignored) {}
        return o.toString();
    }
    static String err(long id, String message) {
        JSONObject o = new JSONObject();
        try { o.put("id", id); o.put("error", message == null ? "error" : message); }
        catch (Exception ignored) {}
        return o.toString();
    }
}

package com.esmu.os;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

import org.json.JSONArray;
import org.json.JSONObject;

final class Contacts {

    static JSONObject list(Context ctx, String query, int limit) throws Exception {
        if (androidx.core.content.ContextCompat.checkSelfPermission(ctx,
                android.Manifest.permission.READ_CONTACTS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            throw new Exception("Permission denied: allow Contacts on the Device tab.");
        }

        JSONObject out = new JSONObject();
        JSONArray arr = new JSONArray();
        ContentResolver cr = ctx.getContentResolver();

        String selection = null;
        String[] args = null;
        if (query != null && !query.trim().isEmpty()) {
            selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?";
            args = new String[]{ "%" + query.trim() + "%" };
        }

        try (Cursor c = cr.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                },
                selection, args,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC")) {
            if (c != null) {
                while (c.moveToNext() && arr.length() < limit) {
                    JSONObject o = new JSONObject();
                    o.put("name", c.getString(0) == null ? "" : c.getString(0));
                    o.put("number", c.getString(1) == null ? "" : c.getString(1));
                    arr.put(o);
                }
            }
        }
        out.put("items", arr);
        return out;
    }
}

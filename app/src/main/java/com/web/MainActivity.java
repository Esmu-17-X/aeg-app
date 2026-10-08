package com.aeg.web;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

public class MainActivity extends Activity {
    static final int LIME = 0xFFC6F432, BG = 0xFF0D0E0C, CARD = 0xFF171815, INK = 0xFFF1EFE8, MUTE = 0xFF9A978D;
    String[] templates = {"Corporate", "Restaurant", "Portfolio", "Online shop",
            "Real estate", "Clinic", "School", "Hotel and travel"};
    String[] pkgs = {"Starter", "Growth", "Scale"};
    int[] prices = {25000, 65000, 145000};
    TextView total;
    RadioGroup rg;

    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    GradientDrawable box(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        return g;
    }

    TextView tv(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    EditText field(String hint, int type, int lines) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(MUTE);
        e.setTextColor(INK);
        e.setInputType(type);
        e.setMinLines(lines);
        e.setGravity(lines > 1 ? Gravity.TOP : Gravity.CENTER_VERTICAL);
        e.setBackground(box(CARD, 14));
        e.setPadding(dp(14), dp(12), dp(14), dp(12));
        return e;
    }

    void add(LinearLayout root, View v, int topDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(topDp);
        root.addView(v, p);
    }

    void updateTotal() {
        int i = rg.indexOfChild(rg.findViewById(rg.getCheckedRadioButtonId()));
        if (i < 0) i = 0;
        total.setText(String.format("Total: ETB %,d", prices[i]));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(40), dp(20), dp(30));
        sv.addView(root);

        add(root, tv("AEG Web", 34, INK, true), 0);
        add(root, tv("Websites for business companies. Place your order below.", 15, MUTE, false), 4);

        add(root, tv("Your name", 14, INK, true), 24);
        add(root, field("Jane Doe", InputType.TYPE_CLASS_TEXT, 1), 6);
        add(root, tv("Company", 14, INK, true), 14);
        add(root, field("Acme Inc.", InputType.TYPE_CLASS_TEXT, 1), 6);
        add(root, tv("Email", 14, INK, true), 14);
        add(root, field("you@company.com", InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS | InputType.TYPE_CLASS_TEXT, 1), 6);

        add(root, tv("Template", 14, INK, true), 14);
        Spinner sp = new Spinner(this);
        sp.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, templates));
        sp.setBackground(box(CARD, 14));
        add(root, sp, 6);

        add(root, tv("Package", 14, INK, true), 14);
        rg = new RadioGroup(this);
        for (int i = 0; i < pkgs.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setId(View.generateViewId());
            rb.setText(String.format("%s  -  ETB %,d", pkgs[i], prices[i]));
            rb.setTextColor(INK);
            rg.addView(rb);
            if (i == 1) rg.check(rb.getId());
        }
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup g, int id) { updateTotal(); }
        });
        add(root, rg, 4);

        add(root, tv("About your project", 14, INK, true), 14);
        add(root, field("What does your business do?", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE, 4), 6);

        total = tv("", 20, LIME, true);
        add(root, total, 22);
        updateTotal();
        add(root, tv("All payments are made in Ethiopian Birr (ETB).", 12, MUTE, false), 2);

        Button send = new Button(this);
        send.setText("Send order");
        send.setAllCaps(false);
        send.setTextColor(0xFF111210);
        send.setTypeface(Typeface.DEFAULT_BOLD);
        send.setBackground(box(LIME, 30));
        add(root, send, 18);   // no click action yet, as requested

        setContentView(sv);
    }
}

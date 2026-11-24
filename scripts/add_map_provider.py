from pathlib import Path
p = Path("az_drive/app/src/main/java/com/azdrive/launcher/MainActivity.java")
s = p.read_text(encoding="utf-8")
def replace_once(old, new):
    global s
    if old not in s:
        raise SystemExit("Patch anchor not found: " + old[:80])
    s = s.replace(old, new, 1)
replace_once("import android.widget.TextView;", "import android.widget.TextView;\nimport android.webkit.WebView;\nimport android.webkit.WebViewClient;")
replace_once("    private long nextPrayerMillis = -1;", "    private long nextPrayerMillis = -1;\n    private String mapProvider = \"osm\";\n    private LinearLayout mapPanel;")
replace_once("        buildDashboard();", "        mapProvider = getPreferences(MODE_PRIVATE).getString(\"map_provider\", \"osm\");\n        buildDashboard();")
replace_once("        LinearLayout mapPanel = panel(); mapPanel.setGravity(Gravity.CENTER); mapPanel.setPadding(dp(22), dp(18), dp(22), dp(18));", "        mapPanel = panel(); mapPanel.setGravity(Gravity.CENTER); mapPanel.setPadding(dp(12), dp(10), dp(12), dp(10));")
old = '''        TextView mapIcon = text("⌖", 58, CYAN, true); mapIcon.setGravity(Gravity.CENTER);
        TextView mapTitle = text("الملاحة", 25, WHITE, true); mapTitle.setGravity(Gravity.CENTER);
        TextView mapSub = text("افتح خرائط Google أو تطبيق الملاحة المفضل", 13, MUTED, false); mapSub.setGravity(Gravity.CENTER);
        Button mapButton = actionButton("فتح Google Maps", true); mapButton.setOnClickListener(v -> launchPackage("com.google.android.apps.maps"));
        mapPanel.addView(mapIcon); mapPanel.addView(mapTitle); mapPanel.addView(mapSub); mapPanel.addView(space(18)); mapPanel.addView(mapButton);'''
new = '''        TextView mapTitle = text("الملاحة", 20, WHITE, true); mapTitle.setGravity(Gravity.CENTER);
        Button sourceButton = actionButton("مصدر الخرائط: " + providerLabel(), false);
        sourceButton.setOnClickListener(v -> chooseMapProvider());
        mapPanel.addView(mapTitle);
        mapPanel.addView(sourceButton, new LinearLayout.LayoutParams(-1, dp(42)));
        renderMapContent();'''
replace_once(old, new)
marker = "    private void showApps() {"
methods = '''    private String providerLabel() {
        if ("google".equals(mapProvider)) return "Google Maps";
        if ("hudhud".equals(mapProvider)) return "هدهد";
        return "OpenStreetMap";
    }

    private void chooseMapProvider() {
        String[] labels = {"OpenStreetMap (افتراضي)", "هدهد", "Google Maps"};
        int selected = "hudhud".equals(mapProvider) ? 1 : ("google".equals(mapProvider) ? 2 : 0);
        new AlertDialog.Builder(this).setTitle("مصدر الخرائط")
                .setSingleChoiceItems(labels, selected, (dialog, which) -> {
                    mapProvider = which == 1 ? "hudhud" : (which == 2 ? "google" : "osm");
                    getPreferences(MODE_PRIVATE).edit().putString("map_provider", mapProvider).apply();
                    buildDashboard();
                    dialog.dismiss();
                }).setNegativeButton("إلغاء", null).show();
    }

    private void renderMapContent() {
        if ("osm".equals(mapProvider)) {
            WebView map = new WebView(this);
            map.setBackgroundColor(PANEL);
            map.getSettings().setJavaScriptEnabled(true);
            map.setWebViewClient(new WebViewClient());
            String html = "<html><meta name='viewport' content='width=device-width,initial-scale=1,user-scalable=no'><body style='margin:0;height:100%;background:#111a27'><iframe style='height:100%;width:100%;border:0' src='https://www.openstreetmap.org/export/embed.html?bbox=46.60%2C24.65%2C46.73%2C24.78&layer=mapnik&marker=24.7136%2C46.6753'></iframe></body></html>";
            map.loadDataWithBaseURL("https://www.openstreetmap.org", html, "text/html", "UTF-8", null);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 0, 1);
            lp.setMargins(0, dp(8), 0, dp(6));
            mapPanel.addView(map, lp);
            Button open = actionButton("فتح الملاحة", true);
            open.setOnClickListener(v -> openMapUrl("https://www.openstreetmap.org/#map=13/24.7136/46.6753"));
            mapPanel.addView(open, new LinearLayout.LayoutParams(-1, dp(42)));
        } else {
            TextView hint = text("اضغط لفتح تطبيق " + providerLabel() + " أو اختر تطبيقًا مثبتًا", 13, MUTED, false);
            hint.setGravity(Gravity.CENTER);
            mapPanel.addView(hint, new LinearLayout.LayoutParams(-1, 0, 1));
            Button open = actionButton("فتح " + providerLabel(), true);
            open.setOnClickListener(v -> {
                if ("google".equals(mapProvider)) launchPackage("com.google.android.apps.maps");
                else showApps();
            });
            mapPanel.addView(open, new LinearLayout.LayoutParams(-1, dp(46)));
        }
    }

    private void openMapUrl(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (Exception e) { toast("تعذر فتح الخرائط"); }
    }

'''
replace_once(marker, methods + marker)
p.write_text(s, encoding="utf-8")
